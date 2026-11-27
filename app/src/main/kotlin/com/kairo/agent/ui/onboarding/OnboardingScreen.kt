package com.kairo.agent.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import com.kairo.agent.ui.components.kairoCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import com.kairo.agent.ui.components.KairoAlertDialog as AlertDialog
import com.kairo.agent.ui.components.KairoButton as Button
import com.kairo.agent.ui.components.KairoCircularProgressIndicator as CircularProgressIndicator
import com.kairo.agent.ui.components.KairoFilledTonalButton as FilledTonalButton
import com.kairo.agent.ui.components.KairoFilterChip as FilterChip
import com.kairo.agent.ui.components.KairoIcon as Icon
import com.kairo.agent.ui.components.KairoLinearProgressIndicator as LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.kairo.agent.ui.components.KairoOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.kairo.agent.ui.components.KairoSurface as Surface
import com.kairo.agent.ui.components.KairoSwitch as Switch
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kairo.agent.R
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
import com.kairo.agent.ui.components.BlurIntoText
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
    var privacyAccepted by remember { mutableStateOf(false) }
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
            val animatedProgress by animateFloatAsState(
                targetValue = ((step + 1) / 6f).coerceIn(0f, 1f),
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "onboarding_progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = AccentBlue,
                trackColor = Color(0x33FFFFFF)
            )

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState >= initialState
                    val slideInOffset: (Int) -> Int = { fullWidth -> if (forward) fullWidth / 3 else -fullWidth / 3 }
                    val slideOutOffset: (Int) -> Int = { fullWidth -> if (forward) -fullWidth / 3 else fullWidth / 3 }
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetX = slideInOffset
                    ) + scaleIn(initialScale = 0.96f, animationSpec = tween(320)) + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            targetOffsetX = slideOutOffset
                        ) + scaleOut(targetScale = 0.96f, animationSpec = tween(240)) + fadeOut(tween(220)))
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
                                com.kairo.agent.ui.components.LandingBlurText(
                                    text = stringResource(R.string.welcome_title),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                BlurIntoText(key = "sub_0", delayMs = 340L) {
                                    Text(
                                        text = stringResource(R.string.welcome_sub),
                                        color = AccentBlue,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                BlurIntoText(key = "desc_0", delayMs = 460L) {
                                    Text(
                                        text = stringResource(R.string.welcome_desc),
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                OnDevicePrivacyBanner()
                                Surface(
                                    color = FrostedCardBg,
                                    shape = kairoCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, FrostedBorder, kairoCornerShape(16.dp))
                                        .clickable { privacyAccepted = !privacyAccepted }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        androidx.compose.material3.Checkbox(
                                            checked = privacyAccepted,
                                            onCheckedChange = { privacyAccepted = it }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                stringResource(R.string.agree_privacy_title),
                                                color = TextPrimary,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                stringResource(R.string.agree_privacy_sub),
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        TextButton(onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://kairo.mikeyslabs.xyz/privacy.html"))
                                                )
                                            }
                                        }) {
                                            Text(stringResource(R.string.read_policy), color = AccentBlue)
                                        }
                                    }
                                }
                            }
                            Button(
                                onClick = { step = 1 },
                                enabled = privacyAccepted,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(stringResource(R.string.get_started), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                                AnimatedHeadline(stringResource(R.string.personalize_title))
                                Text(stringResource(R.string.personalize_sub), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                                Surface(
                                    color = FrostedCardBg,
                                    shape = kairoCornerShape(18.dp),
                                    modifier = Modifier.border(1.dp, FrostedBorder, kairoCornerShape(18.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = userName,
                                            onValueChange = { userName = it },
                                            label = { Text(stringResource(R.string.setup_name_hint)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        SetupToggle(
                                            title = stringResource(R.string.setting_nerd_stats_title),
                                            subtitle = stringResource(R.string.setting_nerd_stats_sub),
                                            checked = settings.showNerdStats,
                                            onCheckedChange = { onUpdateSettings(settings.copy(showNerdStats = it)) }
                                        )
                                        SetupToggle(
                                            title = stringResource(R.string.setting_mini_mode_title),
                                            subtitle = stringResource(R.string.setting_mini_mode_sub),
                                            checked = settings.miniModeOverlay,
                                            onCheckedChange = { onUpdateSettings(settings.copy(miniModeOverlay = it)) }
                                        )
                                        com.kairo.agent.ui.main.AppLanguagePickerRow(
                                            selectedAppLang = settings.appLanguage,
                                            selectedModelLang = settings.preferredLanguage,
                                            onSelectAppAndModelLang = { appLang, modelLang ->
                                                onUpdateSettings(settings.copy(appLanguage = appLang, preferredLanguage = modelLang))
                                            }
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
                                AnimatedHeadline(stringResource(R.string.compat_title))
                                Text(stringResource(R.string.compat_sub), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                                Surface(
                                    color = FrostedCardBg,
                                    shape = kairoCornerShape(18.dp),
                                    modifier = Modifier.border(1.dp, FrostedBorder, kairoCornerShape(18.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("System Checks", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                            TextButton(onClick = onRunChecks) {
                                                if (isCheckingCompatibility) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                else Text("Re-check", color = AccentBlue)
                                            }
                                        }
                                        compatibilityReport?.items?.take(4)?.forEach { item ->
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
                                    listOf(EngineBackendMode.AUTO, EngineBackendMode.AICORE, EngineBackendMode.LOCAL_HTTP).forEach { mode ->
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
                                    subtitle = "Continue even if AICore or SoC allowlist is unavailable",
                                    checked = settings.bypassCompatibility,
                                    onCheckedChange = { onUpdateSettings(settings.copy(bypassCompatibility = it)) }
                                )

                                if (compatibilityReport != null && compatibilityReport.socAllowlisted && !compatibilityReport.aiCoreReady) {
                                    Surface(
                                        color = Color(0xFF1A2639),
                                        shape = kairoCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                "AICore Supported — Additional Setup Needed",
                                                color = AccentBlue,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "Your SoC (${compatibilityReport.socName}) supports AICore, but it isn't active yet. Open Developer Options → Debugging → AICore and enable the preview toggle, then tap Re-check.",
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                if (compatibilityReport != null && !compatibilityReport.allCorePassed && !settings.bypassCompatibility) {
                                    Surface(
                                        color = Color(0xFF3E2226),
                                        shape = kairoCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                "Device reported as incompatible",
                                                color = Color(0xFFF28B82),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "Your SoC (${compatibilityReport.socName}) is not on the Snapdragon 8 Elite / Tensor G5+ allowlist and AICore/Local endpoint isn't ready. You can still bypass this check to continue.",
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            FilledTonalButton(
                                                onClick = {
                                                    onUpdateSettings(settings.copy(bypassCompatibility = true))
                                                    step = 3
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Bypass & Continue Anyway")
                                            }
                                        }
                                    }
                                }
                            }

                            val canContinueCompat = (compatibilityReport?.allCorePassed == true) || settings.bypassCompatibility
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
                                Button(
                                    onClick = { step = 3 },
                                    enabled = canContinueCompat,
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) { Text("Next") }
                            }
                        }
                    }

                    3 -> {
                        val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedHeadline("Core Assistant & Voice")
                                OnDevicePrivacyBanner(compact = true)
                                Text(
                                    "Set up how you talk to Kairo and summon it over any app. Both are optional and run 100% on-device.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                PermissionCard(
                                    title = "Microphone",
                                    subtitle = "Enables voice prompts, hands-free dictation, and optional 'Hey Kairo' wake word detection. Audio is transcribed locally and never sent off-device.",
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
                                Button(onClick = { step = 4 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Next") }
                            }
                        }
                    }

                    4 -> {
                        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
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
                                AnimatedHeadline("Smart Connections (Optional)")
                                Text(
                                    "Let Kairo connect with your local apps and data. You can skip any of these and enable them later in Settings.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
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
                                    title = "Direct Calling",
                                    subtitle = "Allows Kairo to start phone calls immediately when asked, instead of only opening the dialer.",
                                    granted = callGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            "Direct Calling Access",
                                            "Kairo only places a call when you explicitly ask it to call someone. Call actions happen locally on your device.",
                                            onGrantCall
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
                                    title = "Health Connect",
                                    subtitle = "Reads today's steps, heart rate, sleep, distance, calories, and workouts from Health Connect for private on-device wellness summaries.",
                                    granted = healthConnectGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            "Health Connect Access",
                                            "Kairo reads your local Health Connect metrics (steps, heart rate, sleep, distance, calories, workouts) solely to summarize your day on-device.",
                                            onGrantHealthConnect
                                        )
                                    }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 3 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
                                Button(onClick = { step = 5 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Continue") }
                            }
                        }
                    }

                    else -> {
                        val welcomeTitle = if (userName.isNotBlank()) "Welcome, ${userName.trim()}" else "All Set, Ready?"
                        var subVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(240L)
                            subVisible = true
                        }
                        val subAlpha by animateFloatAsState(
                            targetValue = if (subVisible) 1f else 0f,
                            animationSpec = tween(600, easing = CubicBezierEasing(0.117f, 0.126f, 0.312f, 1f)),
                            label = "welcome_sub_alpha"
                        )
                        val subRise by animateFloatAsState(
                            targetValue = if (subVisible) 0f else 36f,
                            animationSpec = tween(650, easing = CubicBezierEasing(0.117f, 0.126f, 0.312f, 1f)),
                            label = "welcome_sub_rise"
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                GemmASparkleIcon(size = 86.dp, isAnimating = true)
                                AnimatedHeadline(welcomeTitle)
                                Text(
                                    text = "Gemma 4 is configured locally on your NPU with zero cloud telemetry.",
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.graphicsLayer {
                                        alpha = subAlpha
                                        translationY = subRise
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { onComplete(userName) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .graphicsLayer {
                                            alpha = subAlpha
                                            translationY = subRise
                                        },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(stringResource(R.string.ready_question), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    com.kairo.agent.ui.components.LandingBlurText(
        text = text,
        color = TextPrimary,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
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
        Switch(checked = checked, onCheckedChange = onCheckedChange, label = title)
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
        shape = kairoCornerShape(16.dp),
        modifier = Modifier.border(1.dp, FrostedBorder, kairoCornerShape(16.dp))
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
        shape = kairoCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x4481C995), kairoCornerShape(16.dp))
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
