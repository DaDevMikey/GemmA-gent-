package com.kairo.agent.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ReportResponseDialog(
    turn: ConversationTurn,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reasons = listOf(
        "Offensive / Harmful",
        "Inaccurate / Hallucination",
        "Unsafe Code / Advice",
        "Other"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var extraNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF17191C),
        titleContentColor = Color(0xFFE3E3E3),
        textContentColor = Color(0xFF9AA0A6),
        title = { Text("Report AI Response", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select a reason:", style = MaterialTheme.typography.labelLarge, color = Color(0xFFE3E3E3))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason) }
                        )
                    }
                }
                androidx.compose.material3.OutlinedTextField(
                    value = extraNote,
                    onValueChange = { extraNote = it },
                    label = { Text("Additional details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                androidx.compose.material3.Surface(
                    color = Color(0xFF1E242E),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Privacy Notice: Logs are NOT sent automatically. Tapping \"Open Email Client\" only prepares a local email draft with this response & reason so you can review and send it manually.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA8C7FA),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val body = buildString {
                    appendLine("Reason: $selectedReason")
                    if (extraNote.isNotBlank()) appendLine("Note: ${extraNote.trim()}")
                    appendLine()
                    appendLine("--- Local Diagnostic Log (User-Initiated) ---")
                    appendLine("Prompt: ${turn.prompt.take(500)}")
                    appendLine("Response: ${turn.response.take(1500)}")
                    if (!turn.actionSummary.isNullOrBlank()) {
                        appendLine("Action: ${turn.actionSummary}")
                    }
                }
                val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:support@damanmikey.me")
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Kairo AI Response Report: $selectedReason")
                    putExtra(android.content.Intent.EXTRA_TEXT, body)
                }
                runCatching {
                    context.startActivity(android.content.Intent.createChooser(emailIntent, "Send Report via Email"))
                }
                onDismiss()
            }) { Text("Open Email Client") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color(0xFF9AA0A6)) }
        }
    )
}
