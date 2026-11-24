package com.kairo.agent.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )
    val spinAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_spin"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (isAnimating) {
                    scaleX = pulseScale
                    scaleY = pulseScale
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
