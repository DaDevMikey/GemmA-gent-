package com.kairo.agent.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginUiGateTest {
    private val oneUi = KairoPlugin(
        packageName = "com.kairo.plugin.oneui", name = "One UI", description = "",
        type = "theme", uiEngine = "sesl9"
    )

    @Test fun oneUiCapabilityEnablesNativeControls() = assertTrue(oneUi.usesSesl9Controls)
    @Test fun legacyPalettePluginKeepsMaterialControls() = assertFalse(oneUi.copy(uiEngine = null).usesSesl9Controls)
    @Test fun otherThemeCannotOptInThroughIconHint() = assertFalse(
        oneUi.copy(packageName = "another.theme", iconPack = "oneui-icons").usesSesl9Controls
    )
    @Test fun languagePluginCannotEnableNativeControls() = assertFalse(oneUi.copy(type = "language").usesSesl9Controls)
    @Test fun unknownEngineKeepsMaterialControls() = assertFalse(oneUi.copy(uiEngine = "future").usesSesl9Controls)
}
