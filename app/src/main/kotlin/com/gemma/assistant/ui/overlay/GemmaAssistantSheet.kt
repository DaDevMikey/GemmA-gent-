package com.gemma.assistant.ui.overlay

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemma.assistant.ui.components.GemmASparkleIcon
import com.gemma.assistant.ui.components.ThinkingDotsIndicator

private val SheetSurface = Color(0xFF131314)
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
    "Device status" to "Check my battery and device status",
    "Set 7:30 alarm" to "Set an alarm for 7:30 AM",
    "Flashlight on" to "Turn on the flashlight"
)

/**
 * Floating dark bottom-sheet overlay for GemmA with an animated gradient border,
 * swipe-up-to-fullscreen & expand button, screen-context & image attachment support,
 * streaming output, and voice/text input.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GemmaAssistantSheet(
    responseText: String,
    isGenerating: Boolean,
    isListening: Boolean,
    partialTranscript: String,
    ttsEnabled: Boolean,
    statusMessage: String?,
    actionSummary: String?,
    actionSuccess: Boolean,
    attachedImage: Bitmap?,
    isScreenAttachment: Boolean,
    hasScreenContext: Boolean,
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

    LaunchedEffect(Unit) {
        sheetVisible = true
    }

    LaunchedEffect(partialTranscript) {
        if (partialTranscript.isNotEmpty()) {
            draft = partialTranscript
        }
    }

    LaunchedEffect(responseText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val isActive = isGenerating || isListening
    val transition = rememberInfiniteTransition(label = "gemma_rim")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing)),
        label = "rim_angle"
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

    val sheetShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .systemBarsPadding()
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
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(240))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .animateContentSize()
                    .clip(sheetShape)
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        brush = rimBrush,
                        shape = sheetShape
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                if (totalDrag < -90f) {
                                    onOpenFullscreen()
                                } else if (totalDrag > 110f) {
                                    onDismiss()
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
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Drag handle pill (swipe up for fullscreen, swipe down to dismiss)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
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
                                text = "GemmA",
                                color = PrimaryText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onToggleTts,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (ttsEnabled) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                    contentDescription = if (ttsEnabled) "Mute spoken replies" else "Speak replies",
                                    tint = if (ttsEnabled) AccentBlue else SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onOpenFullscreen,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Open fullscreen",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Surface(
                                color = BadgeSurface,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Gemma 4",
                                    color = AccentBlue,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Text(
                            text = statusMessage,
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (actionSummary != null) {
                        Surface(
                            color = BadgeSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (actionSuccess) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.ErrorOutline
                                    },
                                    contentDescription = null,
                                    tint = if (actionSuccess) ActionSuccessGreen else ActionErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = actionSummary,
                                    color = PrimaryText,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    if (isGenerating && responseText.isEmpty()) {
                        ThinkingDotsIndicator(
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else if (responseText.isNotEmpty() || isGenerating) {
                        val displayResponse = if (isGenerating) "$responseText ▌" else responseText
                        Text(
                            text = displayResponse,
                            color = PrimaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 240.dp)
                                .verticalScroll(scrollState)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuickPrompts.forEach { (label, promptText) ->
                                AssistChip(
                                    onClick = {
                                        if (label == "Ask about screen") {
                                            onAttachScreen()
                                        }
                                        onSend(promptText)
                                    },
                                    label = { Text(label, color = PrimaryText) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = BadgeSurface
                                    ),
                                    border = null
                                )
                            }
                        }
                    }

                    if (attachedImage != null || (isScreenAttachment && hasScreenContext)) {
                        Surface(
                            color = BadgeSurface,
                            shape = RoundedCornerShape(14.dp)
                        ) {
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
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Screenshot,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = if (isScreenAttachment) "Current screen attached" else "Image attached",
                                    color = PrimaryText,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                IconButton(
                                    onClick = onClearAttachment,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        tint = SecondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
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
                        IconButton(onClick = onPickImage) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Upload image",
                                tint = AccentBlue
                            )
                        }

                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(AccentBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitDraft() }),
                            maxLines = 4,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (draft.isEmpty()) {
                                    Text(
                                        text = if (isListening) "Listening..." else "Ask GemmA anything...",
                                        color = if (isListening) AccentBlue else SecondaryText
                                    )
                                }
                                innerTextField()
                            }
                        )
                        IconButton(onClick = onMicClick) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Voice input",
                                tint = if (isListening) ListeningRed else AccentBlue
                            )
                        }
                        val canSend = draft.isNotBlank() || attachedImage != null
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (canSend) AccentBlue else BadgeSurface)
                                .clickable(enabled = canSend, onClick = submitDraft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (canSend) SheetSurface else SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
