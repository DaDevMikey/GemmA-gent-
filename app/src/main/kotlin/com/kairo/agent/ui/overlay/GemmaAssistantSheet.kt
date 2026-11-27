package com.kairo.agent.ui.overlay

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.kairo.agent.ui.components.kairoCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import com.kairo.agent.ui.components.KairoFilterChip as FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.kairo.agent.ui.components.KairoIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.kairo.agent.ui.components.KairoSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairo.agent.agent.AgentPersona
import com.kairo.agent.agent.NerdStats
import com.kairo.agent.ui.components.GemmASparkleIcon
import com.kairo.agent.ui.components.ListeningWaveformIndicator
import com.kairo.agent.ui.components.ResponseShineSweep
import com.kairo.agent.ui.components.RichResponseText
import com.kairo.agent.ui.components.StarDustParticles
import com.kairo.agent.ui.components.ThinkingDotsIndicator

private val SheetSurface = Color(0xEC131314)
private val InputPillSurface = Color(0xFF1E1F20)
private val BadgeSurface = Color(0xFF282A2C)
private val PrimaryText = Color(0xFFE3E3E3)
private val SecondaryText = Color(0xFF8E918F)
private val AccentBlue = Color(0xFFA8C7FA)
private val ListeningRed = Color(0xFFF28B82)
private val ActionSuccessGreen = Color(0xFF81C995)
private val ActionErrorRed = Color(0xFFF28B82)

private val QuickPrompts = listOf(
    "Ask about screen" to "What is on my screen right now?",
    "Catch me up" to "Catch me up on my recent notifications",
    "Device status" to "Check my battery and device status",
    "Set 7:30 alarm" to "Set an alarm for 7:30 AM"
)

/**
 * Floating frosted-dark bottom-sheet overlay for Kairo with:
 * - Audio-reactive edge glow tied to live mic rmsDb
 * - Spring physics & mini-mode pill collapse
 * - Persona chips, nerd stats (tok/s, TTFT, RAM), Think Harder, and swipe-to-fullscreen
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GemmaAssistantSheet(
    responseText: String,
    isGenerating: Boolean,
    isListening: Boolean,
    rmsDb: Float,
    partialTranscript: String,
    ttsEnabled: Boolean,
    statusMessage: String?,
    actionSummary: String?,
    actionSuccess: Boolean,
    attachedImage: Bitmap?,
    isScreenAttachment: Boolean,
    hasScreenContext: Boolean,
    selectedPersona: AgentPersona,
    nerdStats: NerdStats?,
    showNerdStats: Boolean,
    miniMode: Boolean,
    onSelectPersona: (AgentPersona) -> Unit,
    onToggleMiniMode: () -> Unit,
    onThinkHarder: () -> Unit,
    onSend: (String) -> Unit,
    onMicClick: () -> Unit,
    onPickImage: () -> Unit,
    onAttachScreen: () -> Unit,
    onClearAttachment: () -> Unit,
    onToggleTts: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var sheetVisible by remember { mutableStateOf(false) }
    var totalDrag by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { sheetVisible = true }
    LaunchedEffect(partialTranscript) { if (partialTranscript.isNotEmpty()) draft = partialTranscript }
    LaunchedEffect(responseText) { scrollState.animateScrollTo(scrollState.maxValue) }

    val isActive = isGenerating || isListening
    val isCompactMini = miniMode && responseText.isEmpty() && !isGenerating && attachedImage == null && actionSummary == null

    val transition = rememberInfiniteTransition(label = "kairo_rim")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = if (isListening) 1800 else 3200, easing = LinearEasing)),
        label = "rim_angle"
    )

    var isFullscreen by remember { mutableStateOf(false) }
    val horizPadRaw by animateDpAsState(
        targetValue = if (isFullscreen) 0.dp else 10.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fs_hpad"
    )
    val vertPadRaw by animateDpAsState(
        targetValue = if (isFullscreen) 0.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fs_vpad"
    )
    val cornerRadiusRaw by animateDpAsState(
        targetValue = if (isFullscreen) 18.dp else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fs_corner"
    )
    val horizPad = horizPadRaw.coerceAtLeast(0.dp)
    val vertPad = vertPadRaw.coerceAtLeast(0.dp)
    val cornerRadius = cornerRadiusRaw.coerceAtLeast(0.dp)
    val sheetShape = RoundedCornerShape(cornerRadius)
    val targetBorderWidth = when {
        isListening -> (1.5f + (rmsDb / 12f) * 3.5f).dp
        isGenerating || isFullscreen -> 2.4.dp
        else -> 1.dp
    }
    val borderWidth by animateDpAsState(
        targetValue = targetBorderWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "border_width"
    )

    val rimColorInts = remember {
        intArrayOf(
            Color(0xFF4285F4).toArgb(),
            Color(0xFF9B72CB).toArgb(),
            Color(0xFFD96570).toArgb(),
            Color(0xFF4285F4).toArgb()
        )
    }

    val activeAngle = if (isActive) angle else 0f
    val rimBrush = remember(activeAngle, rimColorInts) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val cx = size.width / 2f
                val cy = size.height / 2f
                return SweepGradient(cx, cy, rimColorInts, null).apply {
                    if (activeAngle != 0f) {
                        val matrix = Matrix().apply { postRotate(activeAngle, cx, cy) }
                        setLocalMatrix(matrix)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .then(if (isFullscreen) Modifier else Modifier.systemBarsPadding())
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = sheetVisible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(260))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier)
                    .padding(horizontal = horizPad, vertical = vertPad)
                    .animateContentSize(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                    .clip(sheetShape)
                    .border(width = borderWidth, brush = rimBrush, shape = sheetShape)
                    .pointerInput(isFullscreen) {
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                if (totalDrag < -90f) onOpenFullscreen()
                                else if (totalDrag > 110f) {
                                    if (isFullscreen) isFullscreen = false else onDismiss()
                                }
                                totalDrag = 0f
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = sheetShape,
                color = SheetSurface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isCompactMini) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(4.dp)
                                    .background(Color(0xFF444746), RoundedCornerShape(2.dp))
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GemmASparkleIcon(size = 20.dp, isAnimating = isActive)
                                Text(
                                    text = "Kairo",
                                    color = PrimaryText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (showNerdStats && nerdStats != null && nerdStats.tokensPerSec > 0f) {
                                    Surface(color = BadgeSurface, shape = RoundedCornerShape(50)) {
                                        Text(
                                            text = "${"%.1f".format(nerdStats.tokensPerSec)} tok/s • ${nerdStats.ttftMs}ms",
                                            color = SecondaryText,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (responseText.isNotEmpty() && !isGenerating) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(responseText))
                                            android.widget.Toast.makeText(context, "Response copied", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = onToggleTts, modifier = Modifier.size(30.dp)) {
                                    Icon(
                                        imageVector = if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                        contentDescription = "TTS",
                                        tint = if (ttsEnabled) AccentBlue else SecondaryText,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                IconButton(onClick = onToggleMiniMode, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Compress, contentDescription = "Mini mode", tint = SecondaryText, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = onOpenFullscreen, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.OpenInFull, contentDescription = "Open in full app", tint = AccentBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (statusMessage != null) {
                            Text(text = statusMessage, color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                        }

                        if (actionSummary != null) {
                            Surface(color = BadgeSurface, shape = kairoCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (actionSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (actionSuccess) ActionSuccessGreen else ActionErrorRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(text = actionSummary, color = PrimaryText, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (isGenerating && responseText.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)) {
                                StarDustParticles(modifier = Modifier.matchParentSize())
                                ThinkingDotsIndicator(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        } else if (responseText.isNotEmpty() || isGenerating) {
                            var wasGen by remember { mutableStateOf(isGenerating) }
                            var shine by remember { mutableStateOf(false) }
                            LaunchedEffect(isGenerating) {
                                if (wasGen && !isGenerating && responseText.isNotEmpty()) {
                                    shine = true
                                }
                                wasGen = isGenerating
                            }
                            val displayResponse = if (isGenerating) "$responseText ▌" else responseText
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .heightIn(max = if (isFullscreen) 560.dp else 240.dp)
                            ) {
                                Box(modifier = Modifier.matchParentSize().clip(kairoCornerShape(14.dp))) {
                                    if (isGenerating) {
                                        StarDustParticles(modifier = Modifier.matchParentSize())
                                    }
                                    ResponseShineSweep(trigger = shine, modifier = Modifier.matchParentSize())
                                }
                                RichResponseText(
                                    text = displayResponse,
                                    textColor = PrimaryText,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(scrollState)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            if (!isGenerating) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = onThinkHarder) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentBlue)
                                        Text(" Think Harder", color = AccentBlue, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                QuickPrompts.forEach { (label, promptText) ->
                                    AssistChip(
                                        onClick = {
                                            if (label == "Ask about screen") onAttachScreen()
                                            onSend(promptText)
                                        },
                                        label = { Text(label, color = PrimaryText) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = BadgeSurface),
                                        border = null
                                    )
                                }
                            }

                            // Persona switcher row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AgentPersona.entries.forEach { persona ->
                                    FilterChip(
                                        selected = selectedPersona == persona,
                                        onClick = { onSelectPersona(persona) },
                                        label = { Text(persona.label, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = InputPillSurface,
                                            selectedContainerColor = AccentBlue.copy(alpha = 0.22f),
                                            labelColor = SecondaryText,
                                            selectedLabelColor = AccentBlue
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }
                    }

                    if (attachedImage != null || (isScreenAttachment && hasScreenContext)) {
                        Surface(color = BadgeSurface, shape = kairoCornerShape(14.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (attachedImage != null) {
                                    Image(
                                        bitmap = attachedImage.asImageBitmap(),
                                        contentDescription = "Attached image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.Screenshot, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = if (isScreenAttachment) "Current screen attached" else "Image attached",
                                    color = PrimaryText,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                IconButton(onClick = onClearAttachment, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove attachment", tint = SecondaryText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    val submitDraft = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty() || attachedImage != null) {
                            onSend(trimmed.ifEmpty { "Describe this image." })
                            draft = ""
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(InputPillSurface, RoundedCornerShape(50))
                            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCompactMini) {
                            IconButton(onClick = onToggleMiniMode, modifier = Modifier.size(36.dp)) {
                                GemmASparkleIcon(size = 18.dp, isAnimating = isActive)
                            }
                        }
                        IconButton(onClick = onPickImage) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload image", tint = AccentBlue)
                        }
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            textStyle = if (com.kairo.agent.ui.theme.LocalSesl9Enabled.current) MaterialTheme.typography.bodyLarge.copy(color = Color.White) else TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(AccentBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitDraft() }),
                            maxLines = 4,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (draft.isEmpty()) {
                                    if (isListening) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ListeningWaveformIndicator()
                                            Text("Listening...", color = AccentBlue)
                                        }
                                    } else {
                                        Text("Ask Kairo anything...", color = SecondaryText)
                                    }
                                }
                                innerTextField()
                            }
                        )
                        IconButton(onClick = onMicClick) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = if (isListening) ListeningRed else AccentBlue
                            )
                        }
                        val canSend = draft.isNotBlank() || attachedImage != null
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (canSend) AccentBlue else BadgeSurface)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White),
                                    enabled = canSend,
                                    onClick = submitDraft
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (canSend) Color(0xFF0B0C0E) else SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
