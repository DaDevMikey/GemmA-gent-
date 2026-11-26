package com.kairo.agent.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kairo.agent.R
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.update.UpdateCheckResult

@Composable
fun SettingsScreen(
    state: GemmAHomeState,
    wakeWordEnabled: Boolean,
    onBack: () -> Unit,
    onUpdateSettings: (AgentSettings) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onOpenMemory: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenReleaseNotes: () -> Unit,
    onCheckUpdates: () -> Unit,
    onReplayTour: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0E))
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFFE3E3E3)
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE3E3E3)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppLanguagePickerRow(
                selectedAppLang = state.settings.appLanguage,
                selectedModelLang = state.settings.preferredLanguage,
                onSelectAppAndModelLang = { appLang, modelLang ->
                    onUpdateSettings(state.settings.copy(appLanguage = appLang, preferredLanguage = modelLang))
                },
                accentColor = Color(0xFFA8C7FA)
            )

            HorizontalDivider(color = Color(0xFF232528))

            Text(
                text = stringResource(R.string.settings_section_agent),
                color = Color(0xFFA8C7FA),
                style = MaterialTheme.typography.labelLarge
            )

            SettingsToggleRow(
                title = stringResource(R.string.multi_agent_title),
                subtitle = stringResource(R.string.multi_agent_subtitle),
                checked = state.settings.multiAgentEnabled,
                onCheckedChange = { onUpdateSettings(state.settings.copy(multiAgentEnabled = it)) }
            )
            SettingsToggleRow(
                title = stringResource(R.string.setting_wake_word_title),
                subtitle = stringResource(R.string.setting_wake_word_sub),
                checked = wakeWordEnabled,
                onCheckedChange = onToggleWakeWord
            )
            SettingsToggleRow(
                title = stringResource(R.string.setting_nerd_stats_title),
                subtitle = stringResource(R.string.setting_nerd_stats_sub),
                checked = state.settings.showNerdStats,
                onCheckedChange = { onUpdateSettings(state.settings.copy(showNerdStats = it)) }
            )
            SettingsToggleRow(
                title = stringResource(R.string.setting_mini_mode_title),
                subtitle = stringResource(R.string.setting_mini_mode_sub),
                checked = state.settings.miniModeOverlay,
                onCheckedChange = { onUpdateSettings(state.settings.copy(miniModeOverlay = it)) }
            )
            SettingsToggleRow(
                title = stringResource(R.string.setting_root_title),
                subtitle = stringResource(R.string.setting_root_sub),
                checked = state.settings.rootPowerMode,
                onCheckedChange = { onUpdateSettings(state.settings.copy(rootPowerMode = it)) }
            )

            HorizontalDivider(color = Color(0xFF232528))

            Text(
                text = stringResource(R.string.settings_section_data),
                color = Color(0xFFA8C7FA),
                style = MaterialTheme.typography.labelLarge
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onOpenMemory, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.memory_label, state.memories.size))
                }
                FilledTonalButton(onClick = onOpenConnections, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.connections_label))
                }
            }

            HorizontalDivider(color = Color(0xFF232528))

            Text(
                text = stringResource(R.string.settings_section_updates),
                color = Color(0xFFA8C7FA),
                style = MaterialTheme.typography.labelLarge
            )

            Surface(color = Color(0xFF1E2025), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.updates_card_title), color = Color(0xFFE3E3E3), style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = onCheckUpdates, enabled = !state.isCheckingUpdates) {
                            if (state.isCheckingUpdates) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            else Text(stringResource(R.string.check_now), color = Color(0xFFA8C7FA))
                        }
                    }
                    when (val upd = state.updateStatus) {
                        is UpdateCheckResult.UpdateAvailable -> {
                            Text(stringResource(R.string.update_available_label, upd.latestVersion), color = Color(0xFF81C995), style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { onOpenUrl(upd.apkUrl ?: upd.releaseUrl) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.download_update))
                            }
                        }
                        is UpdateCheckResult.UpToDate -> {
                            Text(stringResource(R.string.up_to_date_label, upd.currentVersion), color = Color(0xFF9AA0A6), style = MaterialTheme.typography.bodySmall)
                        }
                        is UpdateCheckResult.Unavailable -> {
                            Text(upd.reason, color = Color(0xFF9AA0A6), style = MaterialTheme.typography.bodySmall)
                        }
                        null -> {
                            Text(stringResource(R.string.tap_check_updates), color = Color(0xFF9AA0A6), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedButton(onClick = onOpenReleaseNotes, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.release_notes))
                    }
                }
            }

            OutlinedButton(onClick = onReplayTour, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.replay_tour), color = Color(0xFFE3E3E3))
            }

            OutlinedButton(
                onClick = { onOpenUrl("https://paypal.me/damanmikey") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFA8C7FA))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.support_paypal), color = Color(0xFFA8C7FA))
            }

            Text(
                text = stringResource(R.string.credits_twidget),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9AA0A6)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = Color(0xFFE3E3E3), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF9AA0A6), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
