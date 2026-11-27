package com.kairo.agent.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.kairo.agent.R
import com.kairo.agent.ui.theme.LocalSesl9Enabled
import dev.oneuiproject.oneui.R as OneUiR

/** Directional vectors in the One UI library retain their Android auto-mirroring. */
@DrawableRes
internal fun oneUiIconResource(vectorName: String, enabled: Boolean): Int? {
    if (!enabled) return null
    return when (vectorName.substringAfterLast('.')) {
    "Settings" -> R.drawable.oneui_tw_ic_ab_setting_mtrl
    "Edit" -> R.drawable.oneui_tw_ic_ab_edit_mtrl
    "Close" -> OneUiR.drawable.ic_oui_close
    "Mic" -> R.drawable.oneui_ic_foldable_voice
    "ArrowUpward" -> R.drawable.oneui_ic_foldable_send
    "AutoAwesome", "Psychology" -> R.drawable.oneui_tw_ic_ai_assistant
    "Policy", "Security" -> R.drawable.oneui_security_glyph
    "Hearing" -> R.drawable.oneui_ic_audio_bixby
    "ArrowBack" -> OneUiR.drawable.ic_oui_back
    "Search" -> OneUiR.drawable.ic_oui_search
    "AddPhotoAlternate" -> OneUiR.drawable.ic_oui_image_outline
    "CheckCircle" -> OneUiR.drawable.ic_oui_checkbox_checked_outline
    "ContentCopy" -> OneUiR.drawable.ic_oui_copy_outline
    "DeleteOutline" -> OneUiR.drawable.ic_oui_delete_outline
    "Download" -> OneUiR.drawable.ic_oui_download
    "ErrorOutline" -> OneUiR.drawable.ic_oui_error
    "Favorite" -> OneUiR.drawable.ic_oui_heart
    "Language" -> OneUiR.drawable.ic_oui_internet_website
    "Extension" -> OneUiR.drawable.ic_oui_puzzle
    "FavoriteBorder" -> OneUiR.drawable.ic_oui_heart_outline
    "Hub" -> OneUiR.drawable.ic_oui_nearby_devices
    "KeyboardArrowDown" -> OneUiR.drawable.ic_oui_keyboard_arrow_down
    "Lock" -> OneUiR.drawable.ic_oui_lock_outline
    "Memory" -> OneUiR.drawable.ic_oui_device_outline
    "NewReleases" -> OneUiR.drawable.ic_oui_notes
    "OpenInFull" -> OneUiR.drawable.ic_oui_expand
    "Compress" -> OneUiR.drawable.ic_oui_folder_shrink
    "Refresh" -> OneUiR.drawable.ic_oui_refresh
    "Screenshot" -> OneUiR.drawable.ic_oui_screenshot
    "Share" -> OneUiR.drawable.ic_oui_share_outline
    "Stop" -> OneUiR.drawable.ic_oui_control_stop
    "VolumeUp" -> OneUiR.drawable.ic_oui_volume_up
    "VolumeOff" -> OneUiR.drawable.ic_oui_sound_mute_outline
    else -> null
    }
}

@Composable
fun KairoIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val resource = oneUiIconResource(imageVector.name, LocalSesl9Enabled.current)
    if (resource != null) {
        Icon(painterResource(resource), contentDescription, modifier, tint)
    } else {
        Icon(imageVector, contentDescription, modifier, tint)
    }
}
