package com.gemma.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
