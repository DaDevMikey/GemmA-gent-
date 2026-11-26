package com.kairo.agent.ui.main

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kairo.agent.R
import com.kairo.agent.data.LocaleHelper
import com.kairo.agent.ui.components.RichResponseText

val KairoLanguages = listOf(
    "en" to "English",
    "nl" to "Nederlands",
    "de" to "Deutsch",
    "ru" to "Русский"
)

@Composable
fun AppLanguagePickerRow(
    selectedAppLang: String,
    selectedModelLang: String,
    onSelectAppAndModelLang: (appLang: String, modelLang: String) -> Unit,
    accentColor: Color = Color(0xFFA8C7FA)
) {
    val context = LocalContext.current
    var pendingChoice by remember { mutableStateOf<Pair<String, String>?>(null) }

    pendingChoice?.let { (code, label) ->
        AlertDialog(
            onDismissRequest = { pendingChoice = null },
            containerColor = Color(0xFF17191C),
            titleContentColor = Color(0xFFE3E3E3),
            textContentColor = Color(0xFF9AA0A6),
            title = { Text(stringResource(R.string.model_lang_prompt_title, label), fontWeight = FontWeight.SemiBold) },
            text = { Text(stringResource(R.string.model_lang_prompt_body, label)) },
            confirmButton = {
                Button(onClick = {
                    pendingChoice = null
                    onSelectAppAndModelLang(code, code)
                    LocaleHelper.applyAppLanguage(context, code, recreateActivity = true)
                }) { Text(stringResource(R.string.model_lang_yes, label)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingChoice = null
                    onSelectAppAndModelLang(code, selectedModelLang)
                    LocaleHelper.applyAppLanguage(context, code, recreateActivity = true)
                }) {
                    Text(stringResource(R.string.model_lang_keep), color = Color(0xFF9AA0A6))
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.preferred_language), color = accentColor, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KairoLanguages.forEach { (code, label) ->
                FilterChip(
                    selected = selectedAppLang == code,
                    onClick = { pendingChoice = code to label },
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
        title = { Text(stringResource(R.string.whats_new_title, versionName), fontWeight = FontWeight.SemiBold) },
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
            Button(onClick = onDismiss) { Text(stringResource(R.string.got_it)) }
        }
    )
}
