package com.gemma.assistant.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.gemma.assistant.agent.AndroidAgentTools
import com.gemma.assistant.agent.ScreenContextHolder
import com.gemma.assistant.data.ChatHistoryStore
import com.gemma.assistant.data.ChatRecord
import com.gemma.assistant.data.MemoryStore
import com.gemma.assistant.engine.ModelAvailability
import com.gemma.assistant.service.WakeWordService
import com.gemma.assistant.ui.components.GemmASparkleIcon
import com.gemma.assistant.ui.components.ThinkingDotsIndicator
import com.gemma.assistant.ui.theme.GemmATheme
import com.gemma.assistant.update.UpdateCheckResult
import com.gemma.assistant.update.UpdateChecker
import com.gemma.assistant.voice.VoiceController
import kotlinx.coroutines.launch

private val GeminiBgTop = Color(0xFF000000)
private val GeminiBgGlow = Color(0xFF0B193C)
private val GeminiDrawerBg = Color(0xFF0E0F11)
private val GeminiPillBg = Color(0xFF1E1F20)
private val GeminiChipBg = Color(0xFF282A2C)
private val GeminiTextPrimary = Color(0xFFE3E3E3)
private val GeminiTextSecondary = Color(0xFF8E918F)
private val GeminiAccentBlue = Color(0xFFA8C7FA)
private val GeminiListeningRed = Color(0xFFF28B82)
private val ActionSuccessGreen = Color(0xFF81C995)
private val ActionErrorRed = Color(0xFFF28B82)

private val AgentQuickActions = listOf(
    "Set 7:30 alarm" to "Set an alarm for 7:30 AM",
    "Flashlight on" to "Turn on the flashlight",
    "Battery" to "What is my battery status?",
    "Open Camera" to "Open Camera",
    "Open Wi-Fi" to "Open Wi-Fi settings",
    "Navigate" to "Navigate to ",
    "Remember" to "Remember that "
)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = viewModel {
        val memoryStore = MemoryStore(context)
        MainScreenViewModel(
            chatHistoryStore = ChatHistoryStore(context),
            memoryStore = memoryStore,
            agentTools = AndroidAgentTools(context, memoryStore),
            updateChecker = UpdateChecker(context)
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wakeWordRunning by WakeWordService.isRunning.collectAsStateWithLifecycle()

    val voiceController = remember(context) {
        VoiceController(context) { spoken ->
            viewModel.sendPrompt(spoken)
        }
    }
    val voiceState by voiceController.state.collectAsStateWithLifecycle()

    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceController.toggleListening()
        }
    }

    val wakePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micOk = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (micOk) {
            WakeWordService.start(context)
        }
    }

    var attachedImage by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedImage = ScreenContextHolder.decodeBitmapFromUri(context, uri)
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshChats()
        viewModel.refreshMemories()
        onPauseOrDispose {}
    }

    MainScreenContent(
        state = state,
        isListening = voiceState.isListening,
        partialTranscript = voiceState.partialText,
        wakeWordEnabled = wakeWordRunning,
        attachedImage = attachedImage,
        onPickImage = {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onClearImage = { attachedImage = null },
        onSendPrompt = { prompt ->
            val img = attachedImage
            attachedImage = null
            viewModel.sendPrompt(prompt, img)
        },
        onNewChat = viewModel::startNewChat,
        onSelectChat = viewModel::loadChatIntoView,
        onDeleteChat = viewModel::deleteChat,
        onClearChats = viewModel::clearChats,
        onAddMemory = viewModel::addMemory,
        onDeleteMemory = viewModel::deleteMemory,
        onClearMemories = viewModel::clearMemories,
        onToggleWakeWord = { enable ->
            if (!enable) {
                WakeWordService.stop(context)
            } else {
                val needed = buildList {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (needed.isEmpty()) {
                    WakeWordService.start(context)
                } else {
                    wakePermissionLauncher.launch(needed.toTypedArray())
                }
            }
        },
        onRefreshStatus = viewModel::refreshStatus,
        onDownloadModel = viewModel::downloadModel,
        onWarmupModel = viewModel::warmupModel,
        onCheckUpdates = viewModel::checkForUpdates,
        onOpenUrl = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        onMicClick = {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                voiceController.toggleListening()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onOpenAssistantSettings = { openDefaultAssistantSettings(context) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreenContent(
    state: GemmAHomeState,
    isListening: Boolean,
    partialTranscript: String,
    wakeWordEnabled: Boolean,
    attachedImage: Bitmap? = null,
    onPickImage: () -> Unit = {},
    onClearImage: () -> Unit = {},
    onSendPrompt: (String) -> Unit,
    onNewChat: () -> Unit,
    onSelectChat: (ChatRecord) -> Unit,
    onDeleteChat: (String) -> Unit = {},
    onClearChats: () -> Unit,
    onAddMemory: (String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearMemories: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onRefreshStatus: () -> Unit,
    onDownloadModel: () -> Unit,
    onWarmupModel: () -> Unit,
    onCheckUpdates: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onMicClick: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showModelSheet by remember { mutableStateOf(false) }
    var showMemorySheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }
    var showDrawerSearch by remember { mutableStateOf(false) }
    var drawerSearchQuery by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(partialTranscript) {
        if (partialTranscript.isNotEmpty()) {
            draft = partialTranscript
        }
    }

    LaunchedEffect(state.activeTurns.size, state.activeTurns.lastOrNull()?.response?.length) {
        if (state.activeTurns.isNotEmpty()) {
            listState.animateScrollToItem(state.activeTurns.lastIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = GeminiDrawerBg,
                drawerContentColor = GeminiTextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // 1:1 Gemini Drawer Header: Title + Search + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GemmA",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = GeminiTextPrimary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                        Row {
                            IconButton(onClick = {
                                showDrawerSearch = !showDrawerSearch
                                if (!showDrawerSearch) drawerSearchQuery = ""
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search chats",
                                    tint = GeminiTextPrimary
                                )
                            }
                            IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close drawer",
                                    tint = GeminiTextPrimary
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showDrawerSearch) {
                        OutlinedTextField(
                            value = drawerSearchQuery,
                            onValueChange = { drawerSearchQuery = it },
                            placeholder = { Text("Search recent chats", color = GeminiTextSecondary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // 1:1 Gemini Pill "New chat" button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                onNewChat()
                                scope.launch { drawerState.close() }
                            },
                        shape = RoundedCornerShape(50),
                        color = GeminiPillBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = GeminiTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "New chat",
                                color = GeminiTextPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    DrawerMenuRow(
                        icon = Icons.Default.Psychology,
                        label = "Memory (${state.memories.size})",
                        onClick = {
                            scope.launch { drawerState.close() }
                            showMemorySheet = true
                        }
                    )

                    DrawerMenuRow(
                        icon = Icons.Default.Policy,
                        label = "Privacy & Terms",
                        onClick = {
                            scope.launch { drawerState.close() }
                            showPrivacySheet = true
                        }
                    )

                    // Wake Word Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleWakeWord(!wakeWordEnabled) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = if (wakeWordEnabled) GeminiAccentBlue else GeminiTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Hey GemmA wake word",
                                color = GeminiTextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = onToggleWakeWord
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFF232528)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.labelLarge,
                            color = GeminiTextSecondary
                        )
                        if (state.recentChats.isNotEmpty()) {
                            TextButton(onClick = onClearChats) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear recent chats",
                                    tint = GeminiTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Clear", color = GeminiTextSecondary)
                            }
                        }
                    }

                    val filteredChats = remember(state.recentChats, drawerSearchQuery) {
                        if (drawerSearchQuery.isBlank()) {
                            state.recentChats
                        } else {
                            state.recentChats.filter {
                                it.prompt.contains(drawerSearchQuery, ignoreCase = true) ||
                                    it.response.contains(drawerSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredChats.isEmpty()) {
                        Text(
                            text = if (state.recentChats.isEmpty()) "No recent chats yet." else "No matching chats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GeminiTextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredChats, key = { it.id }) { chat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onSelectChat(chat)
                                            scope.launch { drawerState.close() }
                                        }
                                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = chat.prompt,
                                            color = GeminiTextPrimary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (chat.timestampMs > 0L) {
                                            Text(
                                                text = DateUtils.getRelativeTimeSpanString(
                                                    chat.timestampMs,
                                                    System.currentTimeMillis(),
                                                    DateUtils.MINUTE_IN_MILLIS
                                                ).toString(),
                                                color = GeminiTextSecondary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteChat(chat.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete chat",
                                            tint = GeminiTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredChats.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    }

                    // Bottom Drawer Footer (matches Gemini's bottom account/settings bar)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFF232528)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "GemmA • Gemma Agent",
                                color = GeminiTextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "ON-DEVICE",
                                color = GeminiAccentBlue,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = {
                            scope.launch { drawerState.close() }
                            onOpenAssistantSettings()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Default Assistant Settings",
                                tint = GeminiTextPrimary
                            )
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(GeminiBgGlow, GeminiBgTop),
                        center = Offset(540f, 2250f),
                        radius = 1450f
                    )
                )
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1:1 Gemini Top Bar (no letter circle on top right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            TwoLineMenuIcon()
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showModelSheet = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GemmA",
                                color = GeminiTextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Gemma 4",
                                color = GeminiTextSecondary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Model status",
                                tint = GeminiTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(onClick = onNewChat) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New chat",
                            tint = GeminiTextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Center Content: Empty greeting or active conversation
                if (state.activeTurns.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            GemmASparkleIcon(size = 44.dp)
                            Text(
                                text = "Let's get started",
                                color = GeminiTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Normal
                            )
                            if (state.availability == ModelAvailability.DOWNLOADABLE) {
                                FilledTonalButton(onClick = onDownloadModel) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download Gemma 4")
                                }
                            } else if (state.availability == ModelAvailability.DOWNLOADING) {
                                Text(
                                    text = state.statusDetail ?: "Downloading Gemma 4...",
                                    color = GeminiTextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(state.activeTurns) { turn ->
                            ConversationTurnItem(turn = turn)
                        }
                    }
                }

                // Expandable quick agent actions row
                AnimatedVisibility(
                    visible = showQuickActions,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AgentQuickActions.forEach { (label, promptText) ->
                            AssistChip(
                                onClick = {
                                    showQuickActions = false
                                    if (promptText.endsWith(" ")) {
                                        draft = promptText
                                    } else {
                                        onSendPrompt(promptText)
                                    }
                                },
                                label = { Text(label, color = GeminiTextPrimary) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = GeminiChipBg),
                                border = null
                            )
                        }
                    }
                }

                // Attached image preview
                if (attachedImage != null) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = GeminiChipBg,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = attachedImage.asImageBitmap(),
                                contentDescription = "Selected image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = "Image attached",
                                color = GeminiTextPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                            IconButton(
                                onClick = onClearImage,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = GeminiTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 1:1 Gemini Bottom Composer Pill
                val submitDraft = {
                    val trimmed = draft.trim()
                    if (trimmed.isNotEmpty() || attachedImage != null) {
                        onSendPrompt(trimmed.ifEmpty { "Describe this image." })
                        draft = ""
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = GeminiPillBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showQuickActions = !showQuickActions }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agent tools",
                                tint = GeminiTextPrimary
                            )
                        }

                        IconButton(onClick = onPickImage) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Upload image",
                                tint = GeminiAccentBlue
                            )
                        }

                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            textStyle = TextStyle(color = GeminiTextPrimary, fontSize = 16.sp),
                            cursorBrush = SolidColor(GeminiAccentBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitDraft() }),
                            maxLines = 4,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (draft.isEmpty()) {
                                    Text(
                                        text = if (isListening) "Listening..." else "Ask GemmA",
                                        color = if (isListening) GeminiAccentBlue else GeminiTextSecondary,
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        IconButton(onClick = onMicClick) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isListening) "Stop listening" else "Voice input",
                                tint = if (isListening) GeminiListeningRed else GeminiTextPrimary
                            )
                        }

                        val canSend = draft.isNotBlank() || attachedImage != null
                        val sendScale by animateFloatAsState(
                            targetValue = if (canSend) 1.06f else 0.96f,
                            label = "send_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .scale(sendScale)
                                .clip(CircleShape)
                                .background(if (canSend) GeminiAccentBlue else GeminiChipBg)
                                .clickable(enabled = canSend, onClick = submitDraft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = if (canSend) GeminiBgTop else GeminiTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSheet = false },
            containerColor = GeminiDrawerBg,
            contentColor = GeminiTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GemmA • Gemma Agent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onRefreshStatus) {
                        Text("Refresh", color = GeminiAccentBlue)
                    }
                }

                val statusText = when (state.availability) {
                    ModelAvailability.CHECKING -> "Checking AICore availability..."
                    ModelAvailability.AVAILABLE -> "Gemma 4 is ready on-device"
                    ModelAvailability.DOWNLOADABLE -> "Gemma 4 is ready to download"
                    ModelAvailability.DOWNLOADING -> "Downloading Gemma 4..."
                    ModelAvailability.UNAVAILABLE -> "AICore / Gemma 4 unavailable"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.availability == ModelAvailability.CHECKING || state.isWarmingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                    Text(text = statusText, color = GeminiTextPrimary)
                }

                if (state.availability == ModelAvailability.DOWNLOADING && state.totalDownloadBytes > 0L) {
                    val progress = (state.downloadProgressBytes.toFloat() / state.totalDownloadBytes.toFloat())
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }

                state.statusDetail?.let { detail ->
                    Text(text = detail, style = MaterialTheme.typography.bodySmall, color = GeminiTextSecondary)
                }

                when (state.availability) {
                    ModelAvailability.DOWNLOADABLE -> {
                        Button(onClick = onDownloadModel, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Download Gemma 4")
                        }
                    }
                    ModelAvailability.AVAILABLE -> {
                        FilledTonalButton(
                            onClick = onWarmupModel,
                            enabled = !state.isWarmingUp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.isWarmingUp) "Warming up..." else "Warm up Gemma 4")
                        }
                    }
                    else -> Unit
                }

                OutlinedButton(
                    onClick = {
                        showModelSheet = false
                        onOpenAssistantSettings()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Set as Default Digital Assistant")
                }

                HorizontalDivider(color = Color(0xFF232528))

                when (val update = state.updateStatus) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        Button(
                            onClick = { onOpenUrl(update.apkUrl ?: update.releaseUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Update available: ${update.latestVersion}")
                        }
                    }
                    is UpdateCheckResult.UpToDate -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "App is up to date (v${update.currentVersion})",
                                style = MaterialTheme.typography.bodySmall,
                                color = GeminiTextSecondary
                            )
                            TextButton(onClick = onCheckUpdates, enabled = !state.isCheckingUpdates) {
                                Text(if (state.isCheckingUpdates) "Checking..." else "Check", color = GeminiAccentBlue)
                            }
                        }
                    }
                    is UpdateCheckResult.Unavailable -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = update.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = GeminiTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onCheckUpdates, enabled = !state.isCheckingUpdates) {
                                Text(if (state.isCheckingUpdates) "Checking..." else "Retry", color = GeminiAccentBlue)
                            }
                        }
                    }
                    null -> {
                        TextButton(
                            onClick = onCheckUpdates,
                            enabled = !state.isCheckingUpdates,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (state.isCheckingUpdates) "Checking for updates..." else "Check for app updates",
                                color = GeminiAccentBlue
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showMemorySheet) {
        var newMemoryText by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showMemorySheet = false },
            containerColor = GeminiDrawerBg,
            contentColor = GeminiTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GemmA Memory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (state.memories.isNotEmpty()) {
                        TextButton(onClick = onClearMemories) {
                            Text("Clear all", color = GeminiListeningRed)
                        }
                    }
                }

                Text(
                    text = "Facts remembered here are injected into GemmA's on-device prompt across chats. You can also say \"Remember that...\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = GeminiTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemoryText,
                        onValueChange = { newMemoryText = it },
                        placeholder = { Text("Add a memory...", color = GeminiTextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    FilledTonalButton(
                        onClick = {
                            if (newMemoryText.isNotBlank()) {
                                onAddMemory(newMemoryText)
                                newMemoryText = ""
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }

                if (state.memories.isEmpty()) {
                    Text(
                        text = "No memories saved yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GeminiTextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.memories, key = { it.id }) { item ->
                            Surface(
                                color = GeminiPillBg,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.fact,
                                        color = GeminiTextPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onDeleteMemory(item.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete memory",
                                            tint = GeminiTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showPrivacySheet) {
        ModalBottomSheet(
            onDismissRequest = { showPrivacySheet = false },
            containerColor = GeminiDrawerBg,
            contentColor = GeminiTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Privacy Policy & Terms of Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "100% Local On-Device Agent",
                    style = MaterialTheme.typography.labelLarge,
                    color = GeminiAccentBlue
                )
                Text(
                    text = "• All prompts, images, screen context, and voice transcriptions are processed locally via Android AICore (Gemma 4). Nothing is sent to external AI servers.\n" +
                        "• Recent chats and saved memories are stored only in your device's private app storage and can be deleted individually or cleared anytime.\n" +
                        "• Zero analytics, zero ads, and zero tracking SDKs. Update checks only run when you tap Check.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GeminiTextSecondary
                )
                Text(
                    text = "Terms of Service (MIT)",
                    style = MaterialTheme.typography.labelLarge,
                    color = GeminiAccentBlue
                )
                Text(
                    text = "GemmA is open-source software provided under the MIT License without warranty. Always verify AI-generated responses and device actions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GeminiTextSecondary
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DrawerMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GeminiTextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = GeminiTextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ConversationTurnItem(turn: ConversationTurn) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (turn.image != null) {
                Image(
                    bitmap = turn.image.asImageBitmap(),
                    contentDescription = "Uploaded image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
            Surface(
                color = GeminiChipBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = turn.prompt,
                    color = GeminiTextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            GemmASparkleIcon(
                size = 22.dp,
                isAnimating = turn.isStreaming,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = turn.actionSummary != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    turn.actionSummary?.let { summary ->
                        Surface(
                            color = GeminiChipBg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (turn.actionSuccess) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.ErrorOutline
                                    },
                                    contentDescription = null,
                                    tint = if (turn.actionSuccess) ActionSuccessGreen else ActionErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = summary,
                                    color = GeminiTextPrimary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                if (turn.isStreaming && turn.response.isEmpty()) {
                    ThinkingDotsIndicator()
                } else {
                    Text(
                        text = if (turn.isStreaming) "${turn.response} ▌" else turn.response,
                        color = GeminiTextPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun TwoLineMenuIcon() {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(2.dp)
                .background(GeminiTextPrimary, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(2.dp)
                .background(GeminiTextPrimary, RoundedCornerShape(1.dp))
        )
    }
}

private fun openDefaultAssistantSettings(context: Context) {
    val voiceIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
    val fallbackIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    val target = if (voiceIntent.resolveActivity(context.packageManager) != null) {
        voiceIntent
    } else {
        fallbackIntent
    }
    context.startActivity(target)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    GemmATheme(darkTheme = true) {
        MainScreenContent(
            state = GemmAHomeState(availability = ModelAvailability.AVAILABLE),
            isListening = false,
            partialTranscript = "",
            wakeWordEnabled = false,
            onSendPrompt = {},
            onNewChat = {},
            onSelectChat = {},
            onClearChats = {},
            onAddMemory = {},
            onDeleteMemory = {},
            onClearMemories = {},
            onToggleWakeWord = {},
            onRefreshStatus = {},
            onDownloadModel = {},
            onWarmupModel = {},
            onMicClick = {},
            onOpenAssistantSettings = {}
        )
    }
}
