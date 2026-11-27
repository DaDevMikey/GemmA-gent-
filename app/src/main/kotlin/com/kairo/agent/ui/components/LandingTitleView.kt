package com.kairo.agent.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import kotlin.math.roundToInt
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.graphics.Typeface
import android.icu.text.BreakIterator

/** Port of Twidget's letter-stagger blur reveal, adapted for Compose-owned text styling. */
internal class LandingTitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val fadeInterpolator = PathInterpolator(0.333f, 0f, 0.667f, 1f)
    private val movementInterpolator = PathInterpolator(0.22f, 0.25f, 0f, 1f)
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = Typeface.DEFAULT
    }
    @get:androidx.annotation.ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    private val usesHardwareBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    private val lettersLayer = TitleLayer(context, titlePaint)
    private val hardwareGlyphLayers = mutableListOf<HardwareGlyphLayer>()
    @get:androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private val blurEffects: Array<RenderEffect?> by lazy {
        arrayOfNulls(BLUR_BUCKET_COUNT + 1)
    }
    private var elapsedMs = Float.MAX_VALUE
    var onAnimationFinished: (() -> Unit)? = null

    private var title = ""
    private var geometry = TitleGeometry.EMPTY
    private var letterAnimator: ValueAnimator? = null
    private var playbackGeneration = 0

    init {
        clipChildren = false
        clipToPadding = false
        setWillNotDraw(false)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        lettersLayer.updatePaint(titlePaint)
        if (!usesHardwareBlur) addView(lettersLayer, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setTitle(title)
    }

    private var shapedLayout: StaticLayout? = null
    private var settled = true
    private var streamingMode = false
    private var streamStartUptimeMs = 0L
    private var lastAssignedStartMs = 0f

    private fun currentStreamElapsedMs(): Float {
        if (streamStartUptimeMs == 0L) streamStartUptimeMs = android.os.SystemClock.uptimeMillis()
        return (android.os.SystemClock.uptimeMillis() - streamStartUptimeMs).toFloat().coerceAtLeast(0f)
    }

    private fun activeStaggerMs(): Float = if (streamingMode) STREAM_STAGGER_MS else LETTER_STAGGER_MS
    private fun activeFadeMs(): Float = if (streamingMode) STREAM_FADE_MS else FADE_DURATION_MS
    private fun activeMovementMs(): Long = if (streamingMode) STREAM_MOVEMENT_MS else MOVEMENT_DURATION_MS
    private fun activeMaxBlurPx(): Float = if (streamingMode) STREAM_BLUR_RADIUS_PX else GLYPH_BLUR_RADIUS_PX
    private fun activeSlideDistance(): Float = if (streamingMode) {
        (width * 0.06f).coerceAtMost(dp(18f))
    } else {
        (width * 0.16f).coerceAtMost(dp(80f))
    }

    fun bind(value: String, typeface: Typeface, textSizePx: Float, color: Int, streaming: Boolean = false) {
        val prevTitle = title
        val modeChanged = streamingMode != streaming
        streamingMode = streaming
        val changed = prevTitle != value || modeChanged
        if (!changed && titlePaint.typeface == typeface && titlePaint.textSize == textSizePx && titlePaint.color == color) return
        titlePaint.typeface = typeface
        titlePaint.textSize = textSizePx
        titlePaint.color = color
        lettersLayer.updatePaint(titlePaint)

        if (streaming) {
            if (prevTitle.isEmpty() || !value.startsWith(prevTitle) || modeChanged) {
                streamStartUptimeMs = android.os.SystemClock.uptimeMillis()
                lastAssignedStartMs = 0f
                geometry = TitleGeometry.EMPTY
                elapsedMs = 0f
            }
            title = value
            if (width > 0) {
                rebuildGeometryIncremental(width, geometry.glyphs, currentStreamElapsedMs())
                ensureStreamingTicker()
            }
            requestLayout()
            return
        }

        // The layer paint and glyph positions must both reflect a new theme/font.
        geometry = TitleGeometry.EMPTY
        lastAssignedStartMs = 0f
        setTitle(value)
        if (changed) {
            prepare()
            val generation = playbackGeneration
            addOnLayoutChangeListener(object : OnLayoutChangeListener {
                override fun onLayoutChange(view: View, l: Int, t: Int, r: Int, b: Int,
                    oldL: Int, oldT: Int, oldR: Int, oldB: Int) {
                    removeOnLayoutChangeListener(this)
                    if (generation == playbackGeneration && isAttachedToWindow) play()
                }
            })
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (settled) shapedLayout?.draw(canvas)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        if (!isVisible) finish()
        super.onVisibilityAggregated(isVisible)
    }

    fun setTitle(value: CharSequence) {
        title = value.toString()
        requestLayout()
    }

    fun prepare() {
        cancel()
        elapsedMs = -LETTER_STAGGER_MS
        updateAnimationFrame()
    }

    fun play() {
        cancel()
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finish()
            return
        }
        if (geometry.glyphs.isEmpty()) {
            val generation = playbackGeneration
            if (title.isNotBlank()) post {
                if (generation == playbackGeneration && isShown) play()
            }
            return
        }
        elapsedMs = 0f
        val lastStart = geometry.glyphs.last().startTimeMs.toLong()
        val duration = lastStart + activeMovementMs()
        val startOffset = 0f
        letterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            var cancelled = false
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                elapsedMs = startOffset + animator.currentPlayTime.toFloat()
                updateAnimationFrame()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    elapsedMs = startOffset + duration.toFloat()
                    updateAnimationFrame()
                    onAnimationFinished?.invoke()
                }
            })
            start()
        }
    }

    private fun ensureStreamingTicker() {
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finish()
            return
        }
        if (geometry.glyphs.isEmpty()) return
        elapsedMs = currentStreamElapsedMs()
        updateAnimationFrame()
        if (settled) return
        if (letterAnimator?.isRunning == true) return
        letterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 60_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                elapsedMs = currentStreamElapsedMs()
                updateAnimationFrame()
                if (settled) {
                    cancel()
                    letterAnimator = null
                    onAnimationFinished?.invoke()
                }
            }
            start()
        }
    }

    fun cancel() {
        playbackGeneration++
        letterAnimator?.cancel()
        letterAnimator = null
    }

    fun finish() {
        cancel()
        elapsedMs = Float.MAX_VALUE
        updateAnimationFrame()
    }

    override fun onDetachedFromWindow() {
        cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(1)
        if (streamingMode) {
            rebuildGeometryIncremental(availableWidth, geometry.glyphs, currentStreamElapsedMs())
        } else {
            rebuildGeometry(availableWidth)
        }
        val desiredHeight = geometry.height.coerceAtLeast(1)
        val measuredWidth = resolveSize(availableWidth, widthMeasureSpec)
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
        val childWidthSpec = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
        // Blur needs drawing room, but must not add space before the next view.
        val layerHeight = measuredHeight + dp(TITLE_BLUR_BOTTOM_INSET_DP).toInt()
        val childHeightSpec = MeasureSpec.makeMeasureSpec(layerHeight, MeasureSpec.EXACTLY)
        for (index in 0 until childCount) getChildAt(index).measure(childWidthSpec, childHeightSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in 0 until childCount) getChildAt(index).layout(0, 0, width, height + dp(TITLE_BLUR_BOTTOM_INSET_DP).toInt())
        if (streamingMode) {
            ensureStreamingTicker()
        } else {
            updateAnimationFrame()
        }
    }

    private fun rebuildGeometry(width: Int) {
        rebuildGeometryIncremental(width, emptyList(), 0f)
    }

    private fun rebuildGeometryIncremental(width: Int, prevGlyphs: List<Glyph>, currentElapsedMs: Float) {
        if (title.isBlank()) {
            shapedLayout = null
            setGeometry(TitleGeometry.EMPTY)
            return
        }
        val layout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, width)
            .setAlignment(if (streamingMode) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER)
            // Samsung's sec font lets descenders such as g extend beyond the compact
            // line box. Keep the font padding so the title is never clipped.
            .setIncludePad(true)
            .setLineSpacing(0f, 1f)
            .build()
        shapedLayout = layout
        var sequence = 0
        val stagger = activeStaggerMs()
        val glyphs = buildList {
            val boundaries = BreakIterator.getCharacterInstance(resources.configuration.locales[0]).apply { setText(title) }
            var offset = boundaries.first()
            var next = boundaries.next()
            while (next != BreakIterator.DONE) {
                if (!title.substring(offset, next).all(Char::isWhitespace)) {
                    val line = layout.getLineForOffset(offset)
                    val idx = sequence
                    val startMs = if (idx < prevGlyphs.size) {
                        prevGlyphs[idx].startTimeMs
                    } else {
                        val nextStart = if (streamingMode) {
                            maxOf(currentElapsedMs, lastAssignedStartMs + stagger)
                        } else {
                            idx * stagger
                        }
                        lastAssignedStartMs = nextStart
                        nextStart
                    }
                    add(
                        Glyph(
                            start = offset,
                            end = next,
                            x = layout.getPrimaryHorizontal(offset),
                            baseline = layout.getLineBaseline(line).toFloat(),
                            sequence = sequence++,
                            contextStart = layout.getLineStart(line),
                            contextEnd = layout.getLineEnd(line),
                            isRtl = layout.isRtlCharAt(offset),
                            startTimeMs = startMs,
                        ),
                    )
                }
                offset = next
                next = boundaries.next()
            }
        }
        setGeometry(
            TitleGeometry(
                title,
                glyphs,
                layout.height,
            ),
        )
    }

    private fun setGeometry(value: TitleGeometry) {
        if (geometry == value) return
        geometry = value
        if (usesHardwareBlur) {
            rebuildHardwareGlyphLayers()
        } else {
            lettersLayer.geometry = geometry
        }
        updateAnimationFrame()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun rebuildHardwareGlyphLayers() {
        removeAllViews()
        hardwareGlyphLayers.clear()
        geometry.glyphs.forEach { glyph ->
            HardwareGlyphLayer(context, titlePaint, glyph).also { layer ->
                hardwareGlyphLayers += layer
                addView(
                    layer,
                    LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
            }
        }
    }

    private fun appliedBlurRadius(radius: Float): Float {
        if (radius <= BLUR_REMOVE_PX) return 0f
        if (radius < BLUR_FINE_THRESHOLD_PX) return radius
        if (radius < BLUR_COARSE_THRESHOLD_PX) return radius.roundToInt().toFloat()
        return (
            BLUR_COARSE_THRESHOLD_PX +
                ((radius - BLUR_COARSE_THRESHOLD_PX) / BLUR_COARSE_STEP_PX).roundToInt() *
                    BLUR_COARSE_STEP_PX
            ).roundToInt().coerceIn(0, BLUR_BUCKET_COUNT).toFloat()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private fun blurEffectFor(radius: Float): RenderEffect? {
        if (radius <= 0f) return null
        val bucket = radius.toInt()
        if (radius == bucket.toFloat() && bucket in 1..BLUR_BUCKET_COUNT) {
            return blurEffects[bucket] ?: RenderEffect.createBlurEffect(
                radius,
                radius,
                Shader.TileMode.DECAL,
            ).also { blurEffects[bucket] = it }
        }
        return RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL)
    }

    private fun updateAnimationFrame() {
        val totalDuration = if (geometry.glyphs.isEmpty()) 0f else geometry.glyphs.maxOf { it.startTimeMs } + activeMovementMs()
        settled = elapsedMs >= totalDuration
        for (index in 0 until childCount) getChildAt(index).visibility = if (settled) INVISIBLE else VISIBLE
        invalidate()
        if (settled) return
        if (usesHardwareBlur) {
            val distance = activeSlideDistance()
            hardwareGlyphLayers.forEach { layer ->
                layer.updateAnimation(elapsedMs, distance)
            }
        } else {
            lettersLayer.elapsedMs = elapsedMs
            lettersLayer.invalidate()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private data class Glyph(
        val start: Int,
        val end: Int,
        val x: Float,
        val baseline: Float,
        val sequence: Int,
        val contextStart: Int,
        val contextEnd: Int,
        val isRtl: Boolean,
        val startTimeMs: Float = 0f,
    )

    private data class TitleGeometry(
        val text: String,
        val glyphs: List<Glyph>,
        val height: Int,
    ) {
        companion object {
            val EMPTY = TitleGeometry("", emptyList(), 0)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    private inner class HardwareGlyphLayer(
        context: Context,
        paintSource: TextPaint,
        private val glyph: Glyph,
    ) : View(context) {
        private val paint = TextPaint(paintSource)
        private var lastAppliedRadius = Float.NaN

        fun updateAnimation(globalTime: Float, horizontalDistance: Float) {
            val localTime = globalTime - glyph.startTimeMs
            if (localTime <= 0f) {
                alpha = 0f
                translationX = horizontalDistance
                updateBlur(0f)
                return
            }
            val fade = fadeInterpolator.getInterpolation(
                (localTime / activeFadeMs()).coerceIn(0f, 1f),
            )
            val movement = movementInterpolator.getInterpolation(
                (localTime / activeMovementMs()).coerceIn(0f, 1f),
            )
            alpha = fade
            translationX = horizontalDistance * (1f - movement)
            if (fade <= 0f) {
                updateBlur(0f)
            } else {
                updateBlur(activeMaxBlurPx() * (1f - movement))
            }
        }

        private fun updateBlur(radius: Float) {
            val applied = appliedBlurRadius(radius)
            if (applied == lastAppliedRadius) return
            lastAppliedRadius = applied
            setRenderEffect(blurEffectFor(applied))
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawTextRun(title, glyph.start, glyph.end, glyph.contextStart, glyph.contextEnd,
                glyph.x, glyph.baseline, glyph.isRtl, paint)
        }
    }

    private inner class TitleLayer(
        context: Context,
        paintSource: TextPaint,
    ) : View(context) {
        private val paint = TextPaint(paintSource)
        private val blurMasks = arrayOfNulls<BlurMaskFilter>(BLUR_BUCKET_COUNT + 1)

        private fun maskFor(radius: Float): BlurMaskFilter {
            val bucket = radius.roundToInt().coerceIn(1, BLUR_BUCKET_COUNT)
            return blurMasks[bucket] ?: BlurMaskFilter(bucket.toFloat(), BlurMaskFilter.Blur.NORMAL)
                .also { blurMasks[bucket] = it }
        }

        var geometry: TitleGeometry = TitleGeometry.EMPTY
            set(value) {
                field = value
                invalidate()
            }
        var elapsedMs = Float.MAX_VALUE

        fun updatePaint(source: TextPaint) {
            paint.set(source)
            invalidate()
        }

        init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (geometry.glyphs.isEmpty()) return
            val horizontalDistance = activeSlideDistance()
            geometry.glyphs.forEach { glyph ->
                val localTime = elapsedMs - glyph.startTimeMs
                if (localTime < 0f) return@forEach
                val alpha = fadeInterpolator.getInterpolation(
                    (localTime / activeFadeMs()).coerceIn(0f, 1f),
                )
                val movement = movementInterpolator.getInterpolation(
                    (localTime / activeMovementMs()).coerceIn(0f, 1f),
                )
                paint.alpha = (alpha * 255).toInt()
                val blurRadius = (activeMaxBlurPx() * (1f - movement)).coerceAtLeast(0f)
                paint.maskFilter = if (blurRadius > BLUR_REMOVE_PX) {
                    maskFor(blurRadius)
                } else {
                    null
                }
                canvas.drawTextRun(geometry.text, glyph.start, glyph.end,
                    glyph.contextStart, glyph.contextEnd,
                    glyph.x + horizontalDistance * (1f - movement), glyph.baseline, glyph.isRtl, paint)
            }
            paint.maskFilter = null
        }

        private fun dp(value: Float): Float = value * resources.displayMetrics.density
    }

    private companion object {
        const val LETTER_STAGGER_MS = TitleRevealTiming.STAGGER_MS
        const val FADE_DURATION_MS = TitleRevealTiming.FADE_MS
        const val MOVEMENT_DURATION_MS = TitleRevealTiming.MOVEMENT_MS
        const val STREAM_STAGGER_MS = 12f
        const val STREAM_FADE_MS = 150f
        const val STREAM_MOVEMENT_MS = 220L
        const val STREAM_BLUR_RADIUS_PX = 32f
        const val GLYPH_BLUR_RADIUS_PX = 100f
        const val BLUR_RADIUS_STEP_PX = 1f
        const val BLUR_BUCKET_COUNT = (GLYPH_BLUR_RADIUS_PX / BLUR_RADIUS_STEP_PX).toInt()
        const val BLUR_COARSE_THRESHOLD_PX = 70f
        const val BLUR_COARSE_STEP_PX = 4f
        const val BLUR_FINE_THRESHOLD_PX = 8f
        const val BLUR_REMOVE_PX = 0.05f
        const val TITLE_BLUR_BOTTOM_INSET_DP = 24f
    }
}
