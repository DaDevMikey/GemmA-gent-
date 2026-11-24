package com.kairo.agent.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Multi-orb blurred aurora background that reacts live to Kairo's state
 * (idle breathing, voice listening amplitude, and active generation).
 */
@Composable
fun ReactiveAuroraBackground(
    isGenerating: Boolean = false,
    isListening: Boolean = false,
    rmsDb: Float = 0f,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "reactive_aurora")
    val drift by transition.animateFloat(
        initialValue = -0.15f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isGenerating || isListening) 2600 else 5500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_drift"
    )

    val voiceBoost by animateFloatAsState(
        targetValue = if (isListening) (1f + (rmsDb / 12f) * 0.45f) else if (isGenerating) 1.25f else 1f,
        animationSpec = spring(),
        label = "voice_boost"
    )

    val primaryOrbColor by animateColorAsState(
        targetValue = when {
            isListening -> Color(0xFF1A56DB).copy(alpha = 0.48f)
            isGenerating -> Color(0xFF6D28D9).copy(alpha = 0.44f)
            else -> Color(0xFF0E2356).copy(alpha = 0.55f)
        },
        animationSpec = tween(500),
        label = "orb1_color"
    )

    val secondaryOrbColor by animateColorAsState(
        targetValue = when {
            isListening -> Color(0xFF0EA5E9).copy(alpha = 0.34f)
            isGenerating -> Color(0xFFDB2777).copy(alpha = 0.30f)
            else -> Color(0xFF1E1B4B).copy(alpha = 0.35f)
        },
        animationSpec = tween(500),
        label = "orb2_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(90.dp)
        ) {
            val w = size.width
            val h = size.height

            val center1 = Offset(w * (0.5f + drift), h * 0.85f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryOrbColor, Color.Transparent),
                    center = center1,
                    radius = w * 0.85f * voiceBoost
                ),
                radius = w * 0.85f * voiceBoost,
                center = center1
            )

            val center2 = Offset(w * (0.3f - drift * 0.8f), h * 0.72f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryOrbColor, Color.Transparent),
                    center = center2,
                    radius = w * 0.65f * voiceBoost
                ),
                radius = w * 0.65f * voiceBoost,
                center = center2
            )
        }
    }
}
