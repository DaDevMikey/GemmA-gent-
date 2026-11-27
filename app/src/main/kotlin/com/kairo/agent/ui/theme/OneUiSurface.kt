package com.kairo.agent.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Compose adaptation of Twidget's static SESL surface rim; no input or layout changes. */
@Composable
fun Modifier.oneUiSurfaceRim(radius: Dp): Modifier {
    if (!LocalSesl9Enabled.current) return this
    return drawWithContent {
        drawContent()
        val width = 1.dp.toPx()
        if (size.width > width && size.height > width) {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.02f))),
                topLeft = Offset(width / 2, width / 2),
                size = Size(size.width - width, size.height - width),
                cornerRadius = CornerRadius((radius.toPx() - width / 2).coerceAtLeast(0f)),
                style = Stroke(width)
            )
        }
    }
}
