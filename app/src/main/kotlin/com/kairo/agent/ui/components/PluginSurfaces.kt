package com.kairo.agent.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kairo.agent.ui.theme.LocalKairoPalette
import com.kairo.agent.ui.theme.LocalSesl9Enabled

/** Remap only known neutral surfaces. Error, warning, success and custom accents stay meaningful. */
@Composable
internal fun pluginSurfaceColor(stock: Color): Color {
    if (!LocalSesl9Enabled.current) return stock
    return when (stock.toArgb().toLong() and 0xffffffffL) {
        0xFF0E0F11L, 0xFF0B0C0EL -> LocalKairoPalette.current.bg
        0xFF17191CL, 0xFF1E1F20L, 0xFF1E2025L, 0xFF17171AL -> LocalKairoPalette.current.surface
        0xFF232528L, 0xFF282A2CL -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> stock
    }
}

@Composable
fun kairoCornerShape(stockRadius: Dp): RoundedCornerShape =
    RoundedCornerShape(if (LocalSesl9Enabled.current && stockRadius >= 12.dp && stockRadius <= 24.dp)
        LocalKairoPalette.current.cornerDp.dp else stockRadius)

@Composable
fun KairoSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val outline = if (LocalSesl9Enabled.current && border == null && shape != RectangleShape) {
        BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .10f), Color.White.copy(alpha = .02f))))
    } else border
    Surface(modifier, shape, pluginSurfaceColor(color), contentColor, tonalElevation, shadowElevation, outline, content)
}

/** Capsules keep Compose semantics, callbacks, loading children and custom content intact. */
@Composable
fun KairoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    Button(onClick, if (LocalSesl9Enabled.current) modifier.defaultMinSize(minHeight = 48.dp) else modifier,
        enabled, if (LocalSesl9Enabled.current) RoundedCornerShape(50) else shape,
        colors, elevation, border, contentPadding, interactionSource, content)
}

@Composable
fun KairoFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    FilledTonalButton(onClick, if (LocalSesl9Enabled.current) modifier.defaultMinSize(minHeight = 48.dp) else modifier,
        enabled, if (LocalSesl9Enabled.current) RoundedCornerShape(50) else shape,
        colors, elevation, border, contentPadding, interactionSource, content)
}

@Composable
fun KairoOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(onClick, if (LocalSesl9Enabled.current) modifier.defaultMinSize(minHeight = 48.dp) else modifier,
        enabled, if (LocalSesl9Enabled.current) RoundedCornerShape(50) else shape,
        colors, elevation, border, contentPadding, interactionSource, content)
}

@Composable
fun KairoAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(onDismissRequest, confirmButton, modifier, dismissButton, icon, title, text,
        if (LocalSesl9Enabled.current) RoundedCornerShape(28.dp) else shape,
        pluginSurfaceColor(containerColor), iconContentColor, titleContentColor,
        textContentColor, tonalElevation, properties)
}

@Composable
fun KairoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null
) {
    FilterChip(selected, onClick, label,
        if (LocalSesl9Enabled.current) modifier.defaultMinSize(minHeight = 40.dp) else modifier,
        enabled, leadingIcon, trailingIcon, if (LocalSesl9Enabled.current) RoundedCornerShape(50) else shape,
        colors, elevation, border, interactionSource)
}
