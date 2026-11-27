package com.kairo.agent.ui.components

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.viewinterop.AndroidView

/** Shared landing/onboarding reveal; the surrounding theme owns font and colour. */
@Composable
fun LandingBlurText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val typeface = LocalFontFamilyResolver.current.resolve(
        style.fontFamily, style.fontWeight ?: FontWeight.Normal,
        style.fontStyle ?: FontStyle.Normal, style.fontSynthesis ?: FontSynthesis.All
    ).value as Typeface
    val size = with(LocalDensity.current) { style.fontSize.toPx() }
    AndroidView(
        modifier = modifier.fillMaxWidth().semantics { this.text = AnnotatedString(text) },
        factory = { LandingTitleView(it) },
        update = { it.bind(text, typeface, size, color.toArgb()) },
        onRelease = { it.cancel() }
    )
}
