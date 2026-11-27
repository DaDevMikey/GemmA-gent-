package com.kairo.agent.ui.components

import org.junit.Assert.*
import org.junit.Test

class PluginIconTest {
    private val icons = listOf("Settings", "Edit", "Close", "Mic", "ArrowUpward", "AutoAwesome",
        "Psychology", "Policy", "Security", "Hearing", "ArrowBack", "Search", "AddPhotoAlternate",
        "CheckCircle", "ContentCopy", "DeleteOutline", "Download", "ErrorOutline", "FavoriteBorder",
        "Hub", "KeyboardArrowDown", "Lock", "Memory", "NewReleases", "OpenInFull", "Compress",
        "Refresh", "Screenshot", "Share", "Stop", "VolumeUp", "VolumeOff")

    @Test fun disabledPluginNeverRemapsAnIcon() {
        icons.forEach { assertNull(it, oneUiIconResource("Filled.$it", false)) }
    }
    @Test fun everyCurrentControlHasAOneUiIcon() {
        icons.forEach { assertNotNull(it, oneUiIconResource("Filled.$it", true)) }
    }
    @Test fun autoMirroredNamesAreSupported() {
        assertEquals(oneUiIconResource("Filled.ArrowBack", true), oneUiIconResource("AutoMirrored.Filled.ArrowBack", true))
    }
    @Test fun tintedDrawerIconsUseGlyphsInsteadOfOpaqueCategoryBadges() {
        assertEquals(dev.oneuiproject.oneui.R.drawable.ic_oui_nearby_devices, oneUiIconResource("Filled.Hub", true))
        assertEquals(com.kairo.agent.R.drawable.oneui_security_glyph, oneUiIconResource("Filled.Policy", true))
        assertEquals(dev.oneuiproject.oneui.R.drawable.ic_oui_close, oneUiIconResource("Filled.Close", true))
    }
    @Test fun unknownIconsRetainTheStockFallback() {
        assertNull(oneUiIconResource("Filled.FutureIcon", true))
    }
}
