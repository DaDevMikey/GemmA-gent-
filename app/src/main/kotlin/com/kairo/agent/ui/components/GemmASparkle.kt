package com.kairo.agent.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Four-point Gemini-style sparkle icon rendered with Google's four-color gradient,
 * with optional breathing & gentle rotation animation when GemmA is active.
 */
@Composable
fun GemmASparkleIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isAnimating: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "sparkle_anim")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_alpha"
    )
    val spinAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_spin"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (isAnimating) {
                    alpha = shimmerAlpha
                    rotationZ = spinAngle
                }
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            quadraticTo(cx, cy, w, cy)
            quadraticTo(cx, cy, cx, h)
            quadraticTo(cx, cy, 0f, cy)
            quadraticTo(cx, cy, cx, 0f)
            close()
        }

        val brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFEA4335),
                Color(0xFFFBBC05),
                Color(0xFF34A853),
                Color(0xFF4285F4)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        drawPath(path = path, brush = brush)
    }
}

/**
 * Animated three-dot thinking indicator shown while Gemma 4 processes locally.
 */
@Composable
fun ThinkingDotsIndicator(
    label: String = "Thinking locally",
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "thinking_dots")
    val dotColors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72CB),
        Color(0xFFD96570)
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dotColors.forEachIndexed { index, color ->
                val offsetY by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 420,
                            delayMillis = index * 130,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_y_$index"
                )
                val alpha by transition.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 420,
                            delayMillis = index * 130,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_a_$index"
                )
                Box(
                    modifier = Modifier
                        .offset(y = offsetY.dp)
                        .size(7.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Text(
            text = label,
            color = Color(0xFF8E918F),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Animated five-bar voice waveform shown when the microphone is actively listening.
 */
@Composable
fun ListeningWaveformIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "voice_wave")
    val barColors = listOf(
        Color(0xFF4285F4),
        Color(0xFF9B72CB),
        Color(0xFFD96570),
        Color(0xFF9B72CB),
        Color(0xFF4285F4)
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        barColors.forEachIndexed { index, color ->
            val barHeight by transition.animateFloat(
                initialValue = 6f,
                targetValue = 20f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 360,
                        delayMillis = index * 90,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Floating star & dust particles that drift and twinkle while Kairo is typing/streaming.
 */
@Composable
fun StarDustParticles(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "stardust")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stardust_phase"
    )
    val seeds = remember {
        listOf(
            Triple(0.12f, 0.25f, Color(0xFFA8C7FA)),
            Triple(0.28f, 0.72f, Color(0xFF9B72CB)),
            Triple(0.45f, 0.18f, Color(0xFF6DD58C)),
            Triple(0.62f, 0.60f, Color(0xFFD96570)),
            Triple(0.78f, 0.30f, Color(0xFFA8C7FA)),
            Triple(0.88f, 0.78f, Color(0xFFFBCB0A)),
            Triple(0.35f, 0.45f, Color(0xFFE3E3E3)),
            Triple(0.71f, 0.15f, Color(0xFF9B72CB))
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        seeds.forEachIndexed { idx, (rx, ry, color) ->
            val p = (phase + idx * 0.13f) % 1f
            val x = (rx * w + kotlin.math.sin(p * Math.PI * 2.0).toFloat() * 14f).coerceIn(4f, w - 4f)
            val y = ((ry - p * 0.35f + 1f) % 1f) * h
            val twinkle = (0.35f + 0.65f * kotlin.math.sin(p * Math.PI).toFloat()).coerceIn(0f, 1f)
            val r = if (idx % 2 == 0) 3.2f else 2.0f
            drawCircle(
                color = color.copy(alpha = twinkle * 0.75f),
                radius = r,
                center = Offset(x, y)
            )
            if (idx % 3 == 0) {
                // 4-point mini star cross
                val arm = r * 2.2f
                drawLine(color.copy(alpha = twinkle * 0.55f), Offset(x - arm, y), Offset(x + arm, y), strokeWidth = 1.2f)
                drawLine(color.copy(alpha = twinkle * 0.55f), Offset(x, y - arm), Offset(x, y + arm), strokeWidth = 1.2f)
            }
        }
    }
}

/**
 * Diagonal shining light sweep that glides across a response once streaming finishes.
 */
@Composable
fun ResponseShineSweep(
    trigger: Boolean,
    modifier: Modifier = Modifier
) {
    val sweep = remember { Animatable(-0.4f) }
    LaunchedEffect(trigger) {
        if (trigger) {
            sweep.snapTo(-0.4f)
            sweep.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing)
            )
        }
    }
    if (sweep.value in -0.39f..1.39f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val x = size.width * sweep.value
            val band = size.width * 0.32f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x28A8C7FA),
                        Color(0x38FFFFFF),
                        Color(0x289B72CB),
                        Color.Transparent
                    ),
                    start = Offset(x - band, 0f),
                    end = Offset(x + band, size.height)
                )
            )
        }
    }
}

/**
 * Twidget / One UI 8.5 inspired blur-into-sharp text reveal with gentle upward settle.
 */
@Composable
fun BlurIntoText(
    key: Any? = Unit,
    delayMs: Long = 60L,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var revealed by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        revealed = false
        kotlinx.coroutines.delay(delayMs)
        revealed = true
    }
    val easing = remember { CubicBezierEasing(0.117f, 0.126f, 0.312f, 1f) }
    val blurRadius by animateDpAsState(
        targetValue = if (revealed) 0.dp else 14.dp,
        animationSpec = tween(durationMillis = 560, easing = easing),
        label = "blur_into_text_dp"
    )
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = easing),
        label = "blur_into_text_alpha"
    )
    val risePx by animateFloatAsState(
        targetValue = if (revealed) 0f else 28f,
        animationSpec = tween(durationMillis = 580, easing = easing),
        label = "blur_into_text_rise"
    )
    Box(
        modifier = modifier
            .blur(blurRadius)
            .graphicsLayer {
                this.alpha = alpha
                translationY = risePx
            }
    ) {
        content()
    }
}
