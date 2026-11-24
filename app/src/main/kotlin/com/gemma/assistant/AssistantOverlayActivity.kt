package com.gemma.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gemma.assistant.agent.AgentRunner
import com.gemma.assistant.agent.AgentStreamEvent
import com.gemma.assistant.agent.AndroidAgentTools
import com.gemma.assistant.agent.ScreenContextHolder
import com.gemma.assistant.data.ChatHistoryStore
import com.gemma.assistant.engine.GemmaAssistantEngine
import com.gemma.assistant.engine.ModelAvailability
import com.gemma.assistant.ui.overlay.GemmaAssistantSheet
import com.gemma.assistant.ui.theme.GemmATheme
import com.gemma.assistant.voice.VoiceController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Translucent floating agent overlay triggered by ACTION_ASSIST (power/home long-press or corner swipe)
 * or launched directly via LaunchOverlayActivity.
 */
class AssistantOverlayActivity : ComponentActivity() {

    private val engine by lazy { GemmaAssistantEngine() }
    private val agentTools by lazy { AndroidAgentTools(this) }
    private val agentRunner by lazy { AgentRunner(engine, agentTools) }
    private val chatHistory by lazy { ChatHistoryStore(this) }
    private lateinit var voiceController: VoiceController

    private var responseText by mutableStateOf("")
    private var isGenerating by mutableStateOf(false)
    private var statusMessage by mutableStateOf<String?>(null)
    private var actionSummary by mutableStateOf<String?>(null)
    private var actionSuccess by mutableStateOf(true)
    private var attachedImage by mutableStateOf<Bitmap?>(null)
    private var isScreenAttachment by mutableStateOf(false)
    private var activeGenerationJob: Job? = null

    private val requestAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceController.toggleListening()
        } else {
            statusMessage = "Microphone permission is needed for voice prompts."
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = ScreenContextHolder.decodeBitmapFromUri(this, uri)
            if (bitmap != null) {
                attachedImage = bitmap
                isScreenAttachment = false
                statusMessage = null
            } else {
                statusMessage = "Could not load selected image."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        applyInitialExtras(intent)

        voiceController = VoiceController(this) { spokenPrompt ->
            submitPrompt(spokenPrompt)
        }

        lifecycleScope.launch {
            when (engine.queryAvailability()) {
                ModelAvailability.AVAILABLE -> {
                    statusMessage = null
                    engine.warmupModel()
                }
                ModelAvailability.DOWNLOADABLE -> {
                    statusMessage = "Gemma 4 is ready to download. Open GemmA to download it first."
                }
                ModelAvailability.DOWNLOADING -> {
                    statusMessage = "Gemma 4 model is currently downloading via AICore..."
                }
                ModelAvailability.UNAVAILABLE, ModelAvailability.CHECKING -> {
                    statusMessage = "AICore / Gemma 4 is unavailable. Enable AICore Developer Preview in Settings."
                }
            }
        }

        setContent {
            val voiceState by voiceController.state.collectAsState()
            val hasScreenText = !ScreenContextHolder.extractedScreenText.isNullOrBlank() ||
                ScreenContextHolder.capturedScreenshot != null

            GemmATheme(darkTheme = true) {
                GemmaAssistantSheet(
                    responseText = responseText,
                    isGenerating = isGenerating,
                    isListening = voiceState.isListening,
                    partialTranscript = voiceState.partialText,
                    ttsEnabled = voiceState.ttsEnabled,
                    statusMessage = voiceState.voiceError ?: statusMessage,
                    actionSummary = actionSummary,
                    actionSuccess = actionSuccess,
                    attachedImage = attachedImage,
                    isScreenAttachment = isScreenAttachment,
                    hasScreenContext = hasScreenText,
                    onSend = ::submitPrompt,
                    onMicClick = ::handleMicClick,
                    onPickImage = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAttachScreen = ::attachCapturedScreen,
                    onClearAttachment = {
                        attachedImage = null
                        isScreenAttachment = false
                    },
                    onToggleTts = { voiceController.setTtsEnabled(!voiceState.ttsEnabled) },
                    onOpenFullscreen = {
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                        )
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyInitialExtras(intent)
    }

    private fun applyInitialExtras(intent: Intent?) {
        val initialResponse = intent?.getStringExtra(EXTRA_INITIAL_RESPONSE)
        val initialAction = intent?.getStringExtra(EXTRA_ACTION_SUMMARY)
        if (!initialResponse.isNullOrBlank()) {
            responseText = initialResponse
            actionSummary = initialAction
            actionSuccess = true
            statusMessage = null
        }
        if (intent?.getBooleanExtra(EXTRA_FROM_ASSIST_SESSION, false) == true) {
            attachCapturedScreen()
        }
        if (intent?.getBooleanExtra(EXTRA_AUTO_LISTEN, false) == true) {
            handleMicClick()
        }
    }

    private fun attachCapturedScreen() {
        val screenshot = ScreenContextHolder.capturedScreenshot
        val screenText = ScreenContextHolder.extractedScreenText
        if (screenshot != null || !screenText.isNullOrBlank()) {
            attachedImage = screenshot
            isScreenAttachment = true
        }
    }

    private fun handleMicClick() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            voiceController.toggleListening()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun submitPrompt(prompt: String) {
        activeGenerationJob?.cancel()
        responseText = ""
        statusMessage = null
        actionSummary = null
        actionSuccess = true
        isGenerating = true
        voiceController.stopSpeaking()

        val imageToSend = attachedImage
        val screenText = if (isScreenAttachment) ScreenContextHolder.extractedScreenText else null
        attachedImage = null
        isScreenAttachment = false

        activeGenerationJob = lifecycleScope.launch {
            agentRunner.run(userPrompt = prompt, image = imageToSend, screenText = screenText)
                .catch { error ->
                    statusMessage = error.localizedMessage ?: "Failed to generate response."
                    isGenerating = false
                }
                .collect { event ->
                    when (event) {
                        is AgentStreamEvent.PartialText -> {
                            responseText = event.text
                        }
                        is AgentStreamEvent.RunningTool -> {
                            actionSummary = "Running ${event.toolName.replace('_', ' ')}..."
                            actionSuccess = true
                        }
                        is AgentStreamEvent.ToolCompleted -> {
                            actionSummary = event.summary
                            actionSuccess = event.success
                        }
                        is AgentStreamEvent.Finished -> {
                            isGenerating = false
                            responseText = event.finalText
                            if (event.actionSummary != null) {
                                actionSummary = event.actionSummary
                                actionSuccess = event.actionSuccess
                            }
                            chatHistory.saveChat(
                                prompt = prompt,
                                response = event.finalText,
                                actionSummary = event.actionSummary
                            )
                            if (event.finalText.isNotEmpty()) {
                                voiceController.speakIfEnabled(event.finalText)
                            }
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        activeGenerationJob?.cancel()
        voiceController.release()
        engine.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_INITIAL_RESPONSE = "extra_initial_response"
        const val EXTRA_ACTION_SUMMARY = "extra_action_summary"
        const val EXTRA_AUTO_LISTEN = "extra_auto_listen"
        const val EXTRA_FROM_ASSIST_SESSION = "extra_from_assist_session"
    }
}
