package com.kairo.agent.ui.components

import android.content.res.ColorStateList
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SeslProgressBar
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kairo.agent.R
import com.kairo.agent.ui.theme.LocalKairoPalette
import com.kairo.agent.ui.theme.LocalSesl9Enabled

/** Stock branches deliberately call the same Material controls with their original defaults. */
@Composable
fun KairoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    if (!LocalSesl9Enabled.current) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
        return
    }
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    AndroidView(
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        factory = { context ->
            SwitchCompat(ContextThemeWrapper(context, R.style.Theme_Kairo_Sesl9)).apply {
                showText = false
            }
        },
        update = { view ->
            // Compose state updates must not emit a second settings mutation.
            view.setOnCheckedChangeListener(null)
            view.isChecked = checked
            view.isEnabled = enabled
            view.contentDescription = label
            view.setOnCheckedChangeListener { _, value -> currentOnCheckedChange(value) }
        },
        onRelease = { it.setOnCheckedChangeListener(null) }
    )
}

@Composable
fun KairoCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth
) {
    if (!LocalSesl9Enabled.current) {
        CircularProgressIndicator(modifier = modifier, color = color, strokeWidth = strokeWidth)
        return
    }
    val accent = LocalKairoPalette.current.accent.toArgb()
    AndroidView(
        modifier = modifier.size(48.dp).progressSemantics(),
        factory = { context ->
            OneUiLoadingSpinner(ContextThemeWrapper(context, R.style.Theme_Kairo_Sesl9)).apply {
                isIndeterminate = true
            }
        },
        update = { it.indeterminateTintList = ColorStateList.valueOf(accent) }
    )
}

@Composable
fun KairoLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor
) {
    if (!LocalSesl9Enabled.current) {
        LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor)
        return
    }
    val value = progress().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
    val accent = LocalKairoPalette.current.accent.toArgb()
    AndroidView(
        modifier = modifier.fillMaxWidth().height(4.dp).progressSemantics(value),
        factory = { context ->
            SeslProgressBar(ContextThemeWrapper(context, R.style.Theme_Kairo_Sesl9), null,
                android.R.attr.progressBarStyleHorizontal).apply {
                isIndeterminate = false
                max = 1000
            }
        },
        update = {
            it.progressTintList = ColorStateList.valueOf(accent)
            it.progressBackgroundTintList = ColorStateList.valueOf(trackColor.toArgb())
            it.progress = (value * it.max).toInt()
        }
    )
}
