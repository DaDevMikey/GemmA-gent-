package com.kairo.agent.ui

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.SeslProgressBar
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kairo.agent.data.PluginManager
import com.kairo.agent.ui.components.KairoCircularProgressIndicator
import com.kairo.agent.ui.components.KairoLinearProgressIndicator
import com.kairo.agent.ui.components.KairoSwitch
import com.kairo.agent.ui.theme.GemmATheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Install the One UI plugin fixture before running; verifies real PackageManager discovery. */
class PluginControlsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private var previousTheme: String? = null

    @Before fun saveTheme() {
        previousTheme = PluginManager.getEnabledThemePackage(rule.activity)
        PluginManager.setEnabledThemePackage(rule.activity, null)
    }

    @After fun restoreTheme() {
        PluginManager.setEnabledThemePackage(rule.activity, previousTheme)
    }

    @Test fun nativeControlsExistOnlyWhileInstalledPluginIsSelected() {
        assertTrue("Install the One UI plugin fixture", PluginManager.discoverPlugins(rule.activity)
            .any { it.usesSesl9Controls })
        val checked = mutableStateOf(false)
        var callbacks = 0
        rule.setContent {
            GemmATheme(darkTheme = true) {
                Column {
                    KairoSwitch(checked.value, { callbacks++; checked.value = it }, label = "Test switch")
                    KairoCircularProgressIndicator()
                    KairoLinearProgressIndicator(progress = { 0.42f })
                }
            }
        }
        rule.runOnIdle { assertTrue(nativeViews().isEmpty()) }
        rule.runOnIdle { PluginManager.setEnabledThemePackage(rule.activity, "com.kairo.plugin.oneui") }
        rule.waitForIdle()
        rule.runOnIdle {
            val controls = nativeViews()
            assertEquals(1, controls.filterIsInstance<SwitchCompat>().size)
            assertEquals(2, controls.filterIsInstance<SeslProgressBar>().size)
            assertEquals(420, controls.filterIsInstance<SeslProgressBar>().single { !it.isIndeterminate }.progress)
            assertEquals(0, callbacks)
            checked.value = true
        }
        rule.waitForIdle()
        rule.runOnIdle {
            val switch = nativeViews().filterIsInstance<SwitchCompat>().single()
            assertTrue(switch.isChecked)
            assertEquals("Recomposition must not write settings", 0, callbacks)
            switch.performClick()
            assertEquals(1, callbacks)
            assertFalse(checked.value)
        }
        rule.runOnIdle { PluginManager.setEnabledThemePackage(rule.activity, null) }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue("Disabling plugin must remove native controls", nativeViews().isEmpty()) }
        rule.runOnIdle { PluginManager.setEnabledThemePackage(rule.activity, "uninstalled.theme") }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue("Missing plugin must fall back to Material", nativeViews().isEmpty()) }
    }

    private fun nativeViews(): List<View> {
        fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup) {
            (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
        } else emptyList()
        return descendants(rule.activity.window.decorView).filter { it is SwitchCompat || it is SeslProgressBar }
    }
}
