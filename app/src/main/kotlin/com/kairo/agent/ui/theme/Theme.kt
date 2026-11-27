/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kairo.agent.ui.theme

import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kairo.agent.data.KairoPlugin
import com.kairo.agent.data.PluginManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class KairoPalette(
    val bg: Color = Color(0xFF0B0C0E),
    val surface: Color = Color(0xFF17191C),
    val pill: Color = Color(0xFF1E2025),
    val accent: Color = Color(0xFFA8C7FA),
    val cornerDp: Int = 24,
    val isCustomTheme: Boolean = false,
    val iconPack: String? = null
)

val LocalSesl9Enabled = staticCompositionLocalOf { false }
val LocalActiveThemePackage = staticCompositionLocalOf<String?> { null }

@Composable
private fun rememberActiveThemePlugin(): KairoPlugin? {
    val context = LocalContext.current.applicationContext
    var plugin by remember(context) { mutableStateOf(PluginManager.getActiveThemePlugin(context)) }
    DisposableEffect(context) {
        val preferences = context.getSharedPreferences(PluginManager.PREFS, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            plugin = PluginManager.getActiveThemePlugin(context)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                plugin = PluginManager.getActiveThemePlugin(context)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        // Close the gap between initial discovery and observer registration.
        plugin = PluginManager.getActiveThemePlugin(context)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            context.unregisterReceiver(receiver)
        }
    }
    return plugin
}

val LocalKairoPalette = staticCompositionLocalOf { KairoPalette() }

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    primaryContainer = primaryContainerDark,
    secondary = secondaryDark,
    tertiary = tertiaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    primaryContainer = primaryContainerLight,
    secondary = secondaryLight,
    tertiary = tertiaryLight
)

@Composable
fun GemmATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activePlugin = rememberActiveThemePlugin()
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val seslEnabled = activePlugin?.usesSesl9Controls == true
    val colorScheme = if (activePlugin != null) {
        baseScheme.copy(
            primary = activePlugin.accentColorInt()?.let { androidx.compose.ui.graphics.Color(it) } ?: baseScheme.primary,
            background = activePlugin.bgColorInt()?.let { androidx.compose.ui.graphics.Color(it) } ?: baseScheme.background,
            surface = activePlugin.surfaceColorInt()?.let { androidx.compose.ui.graphics.Color(it) } ?: baseScheme.surface
        )
    } else {
        baseScheme
    }
    val resolvedScheme = if (seslEnabled) colorScheme.copy(
        onPrimary = Color.White,
        primaryContainer = colorScheme.primary.copy(alpha = 0.18f),
        onPrimaryContainer = Color(0xFFB5D4FF),
        secondaryContainer = Color(0xFF303033),
        onSecondaryContainer = Color(0xFFFFFFFF),
        surfaceContainer = colorScheme.surface,
        surfaceContainerLow = Color(0xFF101012),
        surfaceContainerHigh = Color(0xFF303033),
        onSurface = Color(0xFFFFFFFF),
        onSurfaceVariant = Color(0xFFA3A3A7),
        outline = Color(0xFF53535B),
        outlineVariant = Color(0xFF3A3A3D)
    ) else colorScheme

    val palette = if (activePlugin != null) {
        KairoPalette(
            bg = activePlugin.bgColorInt()?.let { Color(it) } ?: Color(0xFF010102),
            surface = activePlugin.surfaceColorInt()?.let { Color(it) } ?: Color(0xFF17171A),
            pill = activePlugin.surfaceColorInt()?.let { Color(it) } ?: Color(0xFF17171A),
            accent = activePlugin.accentColorInt()?.let { Color(it) } ?: Color(0xFF387AFF),
            cornerDp = activePlugin.cornerDp ?: 26,
            isCustomTheme = true,
            iconPack = activePlugin.iconPack
        )
    } else {
        KairoPalette()
    }

    CompositionLocalProvider(
        LocalKairoPalette provides palette,
        LocalSesl9Enabled provides seslEnabled,
        LocalActiveThemePackage provides activePlugin?.packageName
    ) {
        MaterialTheme(
            colorScheme = resolvedScheme,
            typography = if (seslEnabled) OneUiTypography else Typography,
            shapes = if (seslEnabled) OneUiShapes else androidx.compose.material3.Shapes(),
            content = content
        )
    }
}

