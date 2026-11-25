package com.kairo.agent.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.AiCoreModelProfile
import com.kairo.agent.data.EngineBackendMode
import com.kairo.agent.engine.AiCoreModelOption
import com.kairo.agent.engine.CompatibilityReport
import com.kairo.agent.engine.ModelAvailability
import com.kairo.agent.service.NotificationCatchUpStore
import com.kairo.agent.ui.components.GemmASparkleIcon
import com.kairo.agent.ui.components.ReactiveAuroraBackground
import kotlinx.coroutines.delay

private val FrostedCardBg = Color(0xBD16181D)
private val FrostedBorder = Color(0x33FFFFFF)
private val TextPrimary = Color(0xFFE3E3E3)
private val TextSecondary = Color(0xFF9AA0A6)
private val AccentBlue = Color(0xFFA8C7FA)
private val SuccessGreen = Color(0xFF81C995)

@Composable
fun OnboardingScreen(
    availability: ModelAvailability,
    availableAiCoreModels: List<AiCoreModelOption> = emptyList(),
    settings: AgentSettings,
    compatibilityReport: CompatibilityReport?,
    isCheckingCompatibility: Boolean,
    healthConnectGranted: Boolean,
    onRunChecks: () -> Unit,
    onDownloadModel: () -> Unit,
    onUpdateSettings: (AgentSettings) -> Unit,
    onGrantMic: () -> Unit,
    onGrantLocation: () -> Unit = {},
    onGrantContacts: () -> Unit,
    onGrantCall: () -> Unit,
    onGrantHealthConnect: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onComplete: (String) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("") }
    var pendingDisclosure by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }
    val context = LocalContext.current

    pendingDisclosure?.let { (title, body, proceed) ->
        AlertDialog(
            onDismissRequest = { pendingDisclosure = null },
            containerColor = Color(0xFF1A1D24),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = AccentBlue) },
            title = { Text(title, fontWeight = FontWeight.SemiBold) },
            text = { Text(body) },
            confirmButton = {
                Button(onClick = {
                    pendingDisclosure = null
                    proceed()
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDisclosure = null }) { Text("Not now", color = TextSecondary) }
            }
        )
    }

    LaunchedEffect(step) {
        if (step == 2) onRunChecks()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ReactiveAuroraBackground(
            isGenerating = true,
            isListening = step == 3
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(22.dp)
        ) {
            LinearProgressIndicator(
                progress = { ((step + 1) / 5f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = AccentBlue,
                trackColor = Color(0x33FFFFFF)
            )

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInHorizontally(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { it / 3 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(220)))
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
                                GemmASparkleIcon(size = 68.dp, isAnimating = true)
                                AnimatedHeadline("Meet Kairo")
                                Text(
                                    text = "A 100% private, on-device AI agent powered by Gemma 4. Prompts, voice, images, and screen context never leave your device.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                                OnDevicePrivacyBanner()
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
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AnimatedHeadline("Personalize Kairo")
                                Text("Tell Kairo what to call you and pick your default experience.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                                Surface(
                                    color = FrostedCardBg,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.border(1.dp, FrostedBorder, RoundedCornerShape(18.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = userName,
                                            onValueChange = { userName = it },
                                            label = { Text("Your Name (saved to Memory)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        SetupToggle(
                                            title = "Live Nerd Stats (tok/s, RAM)",
                                            subtitle = "Show real-time on-device speed metrics",
                                            checked = settings.showNerdStats,
                                            onCheckedChange = { onUpdateSettings(settings.copy(showNerdStats = it)) }
                                        )
                                        SetupToggle(
                                            title = "Cover-Screen Mini-Mode Overlay",
                                            subtitle = "Compact pill overlay until a reply streams",
                                            checked = settings.miniModeOverlay,
                                            onCheckedChange = { onUpdateSettings(settings.copy(miniModeOverlay = it)) }
                                        )
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
                                Button(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Next") }
                            }
                        }
                    }

                    2 -> {
                        var endpointDraft by remember(settings.localEndpointUrl) { mutableStateOf(settings.localEndpointUrl) }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedHeadline("Compatibility & Engine")
                                Text("Verify your on-device AI backend or configure a local model endpoint.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                                Surface(
                                    color = FrostedCardBg,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.border(1.dp, FrostedBorder, RoundedCornerShape(18.dp))
                                ) {
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

                                if (settings.backendMode != EngineBackendMode.LOCAL_HTTP) {
                                    Text("AICore Model", color = AccentBlue, style = MaterialTheme.typography.labelLarge)
                                    val availableOptions = availableAiCoreModels.filter {
                                        it.availability == ModelAvailability.AVAILABLE || it.availability == ModelAvailability.DOWNLOADABLE
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = settings.aiCoreModelProfile == AiCoreModelProfile.AUTO,
                                            onClick = { onUpdateSettings(settings.copy(aiCoreModelProfile = AiCoreModelProfile.AUTO)) },
                                            label = { Text("Auto (Best Available)") }
                                        )
                                        availableOptions.forEach { option ->
                                            FilterChip(
                                                selected = settings.aiCoreModelProfile == option.profile,
                                                onClick = { onUpdateSettings(settings.copy(aiCoreModelProfile = option.profile)) },
                                                label = {
                                                    val display = option.baseModelName ?: option.label
                                                    Text("$display (${option.label})")
                                                }
                                            )
                                        }
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

                                SetupToggle(
                                    title = "Bypass compatibility checks",
                                    subtitle = "Continue even if AICore is unavailable",
                                    checked = settings.bypassCompatibility,
                                    onCheckedChange = { onUpdateSettings(settings.copy(bypassCompatibility = it)) }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
                                Button(onClick = { step = 3 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Next") }
                            }
                        }
                    }

                    3 -> {
                        val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        val notifGranted = NotificationCatchUpStore.isAccessGranted(context)

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnimatedHeadline("Permissions & Connections")
                                OnDevicePrivacyBanner(compact = true)
                                Text("Every permission is optional and processed 100% on-device. Here is what each one enables:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                                PermissionCard(
                                    title = "Microphone",
                                    subtitle = "Enables voice prompts, hands-free dictation, and wake word detection. Audio is transcribed locally and never sent off-device.",
                                    granted = micGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            "Microphone Access",
                                            "Kairo uses your microphone strictly when you tap the mic or enable optional 'Hey Kairo' wake word detection. Audio is processed locally on your device and never uploaded.",
                                            onGrantMic
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = "Location",
                                    subtitle = "Used locally when you ask for nearby places, local weather, or your current city/address. Coordinates never leave your device.",
                                    granted = locationGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            "Location Access",
                                            "Kairo only reads your device's foreground location when you ask location-based questions (like nearby places or weather). Your coordinates stay 100% on-device.",
                                            onGrantLocation
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = "Contacts",
                                    subtitle = "Lets Kairo look up people by name when you say \"Call Mom\" or \"Text Alex on WhatsApp\" without needing phone numbers.",
                                    granted = contactsGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            "Contacts Access",
                                            "Kairo reads your contacts locally on-device solely to match a spoken or typed name to a phone number or email. Contact data is never shared or uploaded.",
                                            onGrantContacts
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = "Notification Catch-Up",
                                    subtitle = "Reads unread notifications locally (WhatsApp, Telegram, Discord, SMS) so Kairo can summarize what you missed.",
                                    granted = notifGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            "Notification Catch-Up Access",
                                            "Kairo reads recent notifications strictly in local device memory when you ask to catch up. Nothing is logged to external servers.",
                                            onOpenNotificationAccess
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = "Default Digital Assistant",
                                    subtitle = "Bind Kairo to your power button or corner swipe to open the floating overlay over any app and ask about your screen.",
                                    granted = false,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            "Default Digital Assistant",
                                            "Setting Kairo as your Digital Assistant lets it capture transient screen text or screenshots only when you trigger 'Ask about screen'. Nothing is stored or uploaded.",
                                            onOpenAssistantSettings
                                        )
                                    },
                                    actionLabel = "Set Up"
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
                                Button(onClick = { step = 4 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Continue") }
                            }
                        }
                    }

                    else -> {
                        val welcomeTitle = if (userName.isNotBlank()) "Welcome, ${userName.trim()}" else "Welcome"
                        LaunchedEffect(Unit) {
                            delay(1700L)
                            onComplete(userName)
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                GemmASparkleIcon(size = 76.dp, isAnimating = true)
                                AnimatedHeadline(welcomeTitle)
                                Text(
                                    text = "Let's get started",
                                    color = TextSecondary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = { onComplete(userName) }) {
                                    Text("Enter Kairo →", color = AccentBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedHeadline(text: String) {
    var visible by remember(text) { mutableStateOf(false) }
    LaunchedEffect(text) {
        delay(60)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(450),
        label = "headline_alpha"
    )
    Text(
        text = text,
        color = TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.alpha(alpha)
    )
}

@Composable
private fun SetupToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    granted: Boolean,
    onAction: () -> Unit,
    actionLabel: String = "Allow"
) {
    Surface(
        color = FrostedCardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, FrostedBorder, RoundedCornerShape(16.dp))
    ) {
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
                FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun OnDevicePrivacyBanner(compact: Boolean = false) {
    val infinite = rememberInfiniteTransition(label = "privacy_pulse")
    val shimmerAlpha by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "privacy_icon_alpha"
    )
    Surface(
        color = Color(0x9913221A),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x4481C995), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(shimmerAlpha)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.Memory, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                if (!compact) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "100% On-Device • Zero Cloud Uploads",
                    color = SuccessGreen,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (compact) {
                        "Everything—voice, location, contacts, and memories—stays strictly on your phone."
                    } else {
                        "Your chats, memories, voice, photos, location, and screen context are processed locally on your silicon. No analytics, no tracking, no servers."
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
