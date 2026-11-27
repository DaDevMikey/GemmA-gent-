package com.kairo.agent.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.kairo.agent.ui.components.kairoCornerShape
import com.kairo.agent.ui.components.pluginSurfaceColor
import com.kairo.agent.ui.theme.GemmATheme
import com.kairo.agent.ui.theme.OneUiTypography
import com.kairo.agent.ui.theme.Typography as StockTypography
import com.kairo.agent.data.PluginManager
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PluginAppearanceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun disabledThemeRestoresTypographyShapesAndSurfaceColors() {
        val previous = PluginManager.getEnabledThemePackage(rule.activity)
        try {
            assertTrue(PluginManager.discoverPlugins(rule.activity).any { it.usesSesl9Controls })
            PluginManager.setEnabledThemePackage(rule.activity, null)
            var font: androidx.compose.ui.text.font.FontFamily? = null
            var radius: RoundedCornerShape? = null
            var surface = Color.Unspecified
            rule.setContent {
                GemmATheme(darkTheme = true) {
                    font = MaterialTheme.typography.bodyLarge.fontFamily
                    radius = kairoCornerShape(14.dp)
                    surface = pluginSurfaceColor(Color(0xFF1E1F20))
                }
            }
            rule.runOnIdle {
                assertEquals(StockTypography.bodyLarge.fontFamily, font)
                assertEquals(RoundedCornerShape(14.dp), radius)
                assertEquals(Color(0xFF1E1F20), surface)
                PluginManager.setEnabledThemePackage(rule.activity, "com.kairo.plugin.oneui")
            }
            rule.waitForIdle()
            rule.runOnIdle {
                assertEquals(OneUiTypography.bodyLarge.fontFamily, font)
                assertEquals(RoundedCornerShape(22.dp), radius)
                assertEquals(Color(0xFF17171A), surface)
                PluginManager.setEnabledThemePackage(rule.activity, null)
            }
            rule.waitForIdle()
            rule.runOnIdle {
                assertEquals(StockTypography.bodyLarge.fontFamily, font)
                assertEquals(RoundedCornerShape(14.dp), radius)
                assertEquals(Color(0xFF1E1F20), surface)
            }
        } finally {
            PluginManager.setEnabledThemePackage(rule.activity, previous)
        }
    }
}
