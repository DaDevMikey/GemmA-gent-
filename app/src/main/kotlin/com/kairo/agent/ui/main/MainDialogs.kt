package com.kairo.agent.ui.main

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.kairo.agent.ui.components.RichResponseText

val KairoLanguages = listOf(
    "en" to "English",
    "nl" to "Nederlands",
    "de" to "Deutsch",
    "ru" to "Русский"
)

fun applyAppLanguage(langCode: String) {
    val locales = LocaleListCompat.forLanguageTags(langCode)
    AppCompatDelegate.setApplicationLocales(locales)
}

@Composable
fun AppLanguagePickerRow(
    selectedLang: String,
    onSelectLang: (String) -> Unit,
    accentColor: Color = Color(0xFFA8C7FA)
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Preferred Language", color = accentColor, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KairoLanguages.forEach { (code, label) ->
                FilterChip(
                    selected = selectedLang == code,
                    onClick = {
                        applyAppLanguage(code)
                        onSelectLang(code)
                    },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
fun WhatsNewChangelogDialog(
    versionName: String,
    changelogMarkdown: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF17191C),
        titleContentColor = Color(0xFFE3E3E3),
        textContentColor = Color(0xFF9AA0A6),
        title = { Text("What's New in v$versionName", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                RichResponseText(text = changelogMarkdown, textColor = Color(0xFFE3E3E3))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got it") }
        }
    )
}
