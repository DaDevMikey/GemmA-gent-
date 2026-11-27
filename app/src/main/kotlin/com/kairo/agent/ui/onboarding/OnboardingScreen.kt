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
                }) { Text(stringResource(R.string.perm_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDisclosure = null }) { Text(stringResource(R.string.perm_not_now), color = TextSecondary) }
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
                                com.kairo.agent.ui.components.StaggeredBlurText(
                                    text = stringResource(R.string.welcome_title),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    key = "welcome_title"
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
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, FrostedBorder, RoundedCornerShape(16.dp))
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
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.border(1.dp, FrostedBorder, RoundedCornerShape(18.dp))
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
                                OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.back)) }
                                Button(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.next)) }
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
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.border(1.dp, FrostedBorder, RoundedCornerShape(18.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(R.string.setup_system_checks), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                            TextButton(onClick = onRunChecks) {
                                                if (isCheckingCompatibility) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                                else Text(stringResource(R.string.setup_recheck), color = AccentBlue)
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
                                                Text(stringResource(R.string.btn_download_gemma4))
                                            }
                                        }
                                    }
                                }

                                Text(stringResource(R.string.setup_backend_mode), color = AccentBlue, style = MaterialTheme.typography.labelLarge)
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
                                    Text(stringResource(R.string.setup_aicore_model), color = AccentBlue, style = MaterialTheme.typography.labelLarge)
                                    val availableOptions = availableAiCoreModels.filter {
                                        it.availability == ModelAvailability.AVAILABLE || it.availability == ModelAvailability.DOWNLOADABLE
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = settings.aiCoreModelProfile == AiCoreModelProfile.AUTO,
                                            onClick = { onUpdateSettings(settings.copy(aiCoreModelProfile = AiCoreModelProfile.AUTO)) },
                                            label = { Text(stringResource(R.string.aicore_auto_label)) }
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
                                        label = { Text(stringResource(R.string.setup_local_endpoint_hint)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                SetupToggle(
                                    title = stringResource(R.string.setup_bypass_title),
                                    subtitle = stringResource(R.string.setup_bypass_sub),
                                    checked = settings.bypassCompatibility,
                                    onCheckedChange = { onUpdateSettings(settings.copy(bypassCompatibility = it)) }
                                )

                                if (compatibilityReport != null && compatibilityReport.socAllowlisted && !compatibilityReport.aiCoreReady) {
                                    Surface(
                                        color = Color(0xFF1A2639),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                stringResource(R.string.setup_aicore_supported),
                                                color = AccentBlue,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                stringResource(R.string.setup_aicore_supported_body, compatibilityReport.socName),
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                if (compatibilityReport != null && !compatibilityReport.allCorePassed && !settings.bypassCompatibility) {
                                    Surface(
                                        color = Color(0xFF3E2226),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                stringResource(R.string.setup_incompat_title),
                                                color = Color(0xFFF28B82),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                stringResource(R.string.setup_incompat_body, compatibilityReport.socName),
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
                                                Text(stringResource(R.string.setup_bypass_btn))
                                            }
                                        }
                                    }
                                }
                            }

                            val canContinueCompat = (compatibilityReport?.allCorePassed == true) || settings.bypassCompatibility
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.back)) }
                                Button(
                                    onClick = { step = 3 },
                                    enabled = canContinueCompat,
                                    modifier = Modifier.weight(1f).height(50.dp)
                                ) { Text(stringResource(R.string.next)) }
                            }
                        }
                    }

                    3 -> {
                        val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        val micTitle = stringResource(R.string.disclosure_mic_title)
                        val micBody = stringResource(R.string.disclosure_onboard_mic_body)
                        val assistantTitle = stringResource(R.string.disclosure_onboard_assistant_title)
                        val assistantBody = stringResource(R.string.disclosure_onboard_assistant_body)

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedHeadline(stringResource(R.string.onboarding_voice_title))
                                OnDevicePrivacyBanner(compact = true)
                                Text(
                                    stringResource(R.string.onboarding_voice_body),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                PermissionCard(
                                    title = stringResource(R.string.perm_microphone),
                                    subtitle = stringResource(R.string.perm_microphone_sub),
                                    granted = micGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            micTitle,
                                            micBody,
                                            onGrantMic
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = stringResource(R.string.perm_digital_assistant),
                                    subtitle = stringResource(R.string.perm_digital_assistant_sub),
                                    granted = false,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            assistantTitle,
                                            assistantBody,
                                            onOpenAssistantSettings
                                        )
                                    },
                                    actionLabel = stringResource(R.string.perm_set_up)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.back)) }
                                Button(onClick = { step = 4 }, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.next)) }
                            }
                        }
                    }

                    4 -> {
                        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val contactsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        val callGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                        val notifGranted = NotificationCatchUpStore.isAccessGranted(context)
                        val locDiscTitle = stringResource(R.string.disclosure_location_title)
                        val locDiscBody = stringResource(R.string.disclosure_location_body)
                        val contactsDiscTitle = stringResource(R.string.disclosure_contacts_title)
                        val contactsDiscBody = stringResource(R.string.disclosure_contacts_body)
                        val callDiscTitle = stringResource(R.string.disclosure_calling_title)
                        val callDiscBody = stringResource(R.string.disclosure_calling_body)
                        val notifDiscTitle = stringResource(R.string.disclosure_notif_title)
                        val notifDiscBody = stringResource(R.string.disclosure_notif_body)
                        val healthDiscTitle = stringResource(R.string.disclosure_health_title)
                        val healthDiscBody = stringResource(R.string.disclosure_health_body)

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AnimatedHeadline(stringResource(R.string.onboarding_connections_title))
                                Text(
                                    stringResource(R.string.onboarding_connections_body),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                PermissionCard(
                                    title = stringResource(R.string.perm_location),
                                    subtitle = stringResource(R.string.perm_location_sub),
                                    granted = locationGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            locDiscTitle,
                                            locDiscBody,
                                            onGrantLocation
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = stringResource(R.string.perm_contacts),
                                    subtitle = stringResource(R.string.perm_contacts_sub),
                                    granted = contactsGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            contactsDiscTitle,
                                            contactsDiscBody,
                                            onGrantContacts
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = stringResource(R.string.perm_calling),
                                    subtitle = stringResource(R.string.perm_calling_sub),
                                    granted = callGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            callDiscTitle,
                                            callDiscBody,
                                            onGrantCall
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = stringResource(R.string.perm_notifications),
                                    subtitle = stringResource(R.string.perm_notifications_sub),
                                    granted = notifGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            notifDiscTitle,
                                            notifDiscBody,
                                            onOpenNotificationAccess
                                        )
                                    }
                                )
                                PermissionCard(
                                    title = stringResource(R.string.perm_health),
                                    subtitle = stringResource(R.string.perm_health_sub),
                                    granted = healthConnectGranted,
                                    onAction = {
                                        pendingDisclosure = Triple(
                                            healthDiscTitle,
                                            healthDiscBody,
                                            onGrantHealthConnect
                                        )
                                    }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 3 }, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.back)) }
                                Button(onClick = { step = 5 }, modifier = Modifier.weight(1f).height(50.dp)) { Text(stringResource(R.string.continue_label)) }
                            }
                        }
                    }

                    else -> {
                        val welcomeTitle = if (userName.isNotBlank()) stringResource(R.string.onboarding_welcome_name, userName.trim()) else stringResource(R.string.onboarding_welcome_default)
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
                                    text = stringResource(R.string.setup_final_sub),
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
    BlurIntoText(key = text, delayMs = 60L) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
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
    actionLabel: String = ""
) {
    val resolvedActionLabel = actionLabel.ifEmpty { stringResource(R.string.perm_allow) }
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
                    Text(stringResource(R.string.perm_granted), color = SuccessGreen, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                FilledTonalButton(onClick = onAction) { Text(resolvedActionLabel) }
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
                    text = stringResource(R.string.onboard_privacy_header),
                    color = SuccessGreen,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (compact) {
                        stringResource(R.string.onboard_privacy_compact)
                    } else {
                        stringResource(R.string.onboard_privacy_full)
                    },
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
