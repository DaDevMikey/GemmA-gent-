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
    val activePlugin = androidx.compose.runtime.remember(context) {
        com.kairo.agent.data.PluginManager.getActiveThemePlugin(context)
    }
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = if (activePlugin != null) {
        baseScheme.copy(
            primary = activePlugin.accentColorInt()?.let { androidx.compose.ui.graphics.Color(it) } ?: baseScheme.primary,
            background = activePlugin.bgColorInt()?.let { androidx.compose.ui.graphics.Color(it) } ?: baseScheme.background,
            surface = activePlugin.surfaceColorInt()?.let { androidx.compose.ui.graphics.Color(it) } ?: baseScheme.surface
        )
    } else {
        baseScheme
    }

    val palette = if (activePlugin != null) {
        KairoPalette(
            bg = activePlugin.bgColorInt()?.let { Color(it) } ?: Color(0xFF0B0C0E),
            surface = activePlugin.surfaceColorInt()?.let { Color(it) } ?: Color(0xFF17191C),
            pill = activePlugin.surfaceColorInt()?.let { Color(it) } ?: Color(0xFF1E2025),
            accent = activePlugin.accentColorInt()?.let { Color(it) } ?: Color(0xFFA8C7FA),
            cornerDp = activePlugin.cornerDp ?: 24,
            isCustomTheme = true,
            iconPack = activePlugin.iconPack
        )
    } else {
        KairoPalette()
    }

    CompositionLocalProvider(LocalKairoPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

