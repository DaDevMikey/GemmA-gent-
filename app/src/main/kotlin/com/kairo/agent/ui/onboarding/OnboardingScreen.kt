package com.kairo.agent.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.EngineBackendMode
import com.kairo.agent.engine.CompatibilityReport
import com.kairo.agent.engine.ModelAvailability
import com.kairo.agent.service.NotificationCatchUpStore
import com.kairo.agent.ui.components.GemmASparkleIcon

private val BgTop = Color(0xFF000000)
private val BgGlow = Color(0xFF0B193C)
private val CardBg = Color(0xFF16171A)
private val TextPrimary = Color(0xFFE3E3E3)
private val TextSecondary = Color(0xFF8E918F)
private val AccentBlue = Color(0xFFA8C7FA)
private val SuccessGreen = Color(0xFF81C995)

@Composable
fun OnboardingScreen(
    availability: ModelAvailability,
    settings: AgentSettings,
    compatibilityReport: CompatibilityReport?,
    isCheckingCompatibility: Boolean,
    onRunChecks: () -> Unit,
    onDownloadModel: () -> Unit,
    onUpdateSettings: (AgentSettings) -> Unit,
    onGrantMic: () -> Unit,
    onGrantContacts: () -> Unit,
    onGrantCall: () -> Unit,
    onGrantHealthConnect: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(step) {
        if (step == 1) onRunChecks()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(BgGlow, BgTop),
                    center = Offset(540f, 2100f),
                    radius = 1550f
                )
            )
            .systemBarsPadding()
            .padding(22.dp)
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "onboarding_step"
        ) { currentStep ->
            when (currentStep) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(24.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            GemmASparkleIcon(size = 64.dp, isAnimating = true)
                            Text(
                                text = "Meet Kairo",
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "A 100% private, on-device AI agent powered by Gemma 4. Prompts, voice, images, and screen context never leave your device.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("Get Started", fontSize = 16.sp)
                        }
                    }
                }

                1 -> {
                    var endpointDraft by remember(settings.localEndpointUrl) { mutableStateOf(settings.localEndpointUrl) }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Compatibility & Engine", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("Verify your on-device AI backend or configure a local model endpoint.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                            Surface(color = CardBg, shape = RoundedCornerShape(18.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("System Checks", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                        TextButton(onClick = onRunChecks) {
                                            if (isCheckingCompatibility) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            else Text("Re-check", color = AccentBlue)
                                        }
                                    }
                                    compatibilityReport?.items?.take(3)?.forEach { item ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(
                                                imageVector = if (item.passed) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = if (item.passed) SuccessGreen else TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(item.label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                                Text(item.detail, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    if (availability == ModelAvailability.DOWNLOADABLE) {
                                        FilledTonalButton(onClick = onDownloadModel, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(Modifier.size(8.dp))
                                            Text("Download Gemma 4")
                                        }
                                    }
                                }
                            }

                            Text("Backend Mode", color = AccentBlue, style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EngineBackendMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = settings.backendMode == mode,
                                        onClick = { onUpdateSettings(settings.copy(backendMode = mode)) },
                                        label = { Text(mode.name) }
                                    )
                                }
                            }

                            if (settings.backendMode != EngineBackendMode.AICORE) {
                                OutlinedTextField(
                                    value = endpointDraft,
                                    onValueChange = {
                                        endpointDraft = it
                                        onUpdateSettings(settings.copy(localEndpointUrl = it))
                                    },
                                    label = { Text("Local Model Endpoint (Optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text("Bypass compatibility checks", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    Text("Continue even if AICore is unavailable", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Switch(
                                    checked = settings.bypassCompatibility,
                                    onCheckedChange = { onUpdateSettings(settings.copy(bypassCompatibility = it)) }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text("Back")
                            }
                            Button(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text("Next")
                            }
                        }
                    }
                }

                else -> {
                    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    val callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                    val notifGranted = NotificationCatchUpStore.isAccessGranted(context)

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Permissions & Connections", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("Grant the capabilities you want Kairo to use. You can change these anytime.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                            PermissionCard("Microphone", "Voice prompts & wake word", micGranted, onGrantMic)
                            PermissionCard("Contacts", "Look up people to call or text", contactsGranted, onGrantContacts)
                            PermissionCard("Direct Calling", "Place calls directly without extra taps", callGranted, onGrantCall)
                            PermissionCard("Notification Catch-Up", "Summarize unread messages locally", notifGranted, onOpenNotificationAccess)
                            PermissionCard("Health Connect", "Read today's steps & heart rate", false, onGrantHealthConnect)

                            OutlinedButton(onClick = onOpenAssistantSettings, modifier = Modifier.fillMaxWidth()) {
                                Text("Set Kairo as Default Digital Assistant")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text("Back")
                            }
                            Button(onClick = onComplete, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text("Start Using Kairo")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    Surface(color = CardBg, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Text("Granted", color = SuccessGreen, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                FilledTonalButton(onClick = onAction) {
                    Text("Allow")
                }
            }
        }
    }
}
