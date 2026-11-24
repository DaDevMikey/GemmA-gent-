package com.gemma.assistant.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemma.assistant.agent.AgentRunner
import com.gemma.assistant.agent.AgentStreamEvent
import com.gemma.assistant.agent.AndroidAgentTools
import com.gemma.assistant.data.ChatHistoryStore
import com.gemma.assistant.data.ChatRecord
import com.gemma.assistant.data.MemoryItem
import com.gemma.assistant.data.MemoryStore
import com.gemma.assistant.engine.GemmaAssistantEngine
import com.gemma.assistant.engine.ModelAvailability
import com.gemma.assistant.update.UpdateCheckResult
import com.gemma.assistant.update.UpdateChecker
import com.google.mlkit.genai.common.DownloadStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConversationTurn(
    val prompt: String,
    val response: String,
    val image: Bitmap? = null,
    val actionSummary: String? = null,
    val actionSuccess: Boolean = true,
    val isStreaming: Boolean = false
)

data class GemmAHomeState(
    val availability: ModelAvailability = ModelAvailability.CHECKING,
    val downloadProgressBytes: Long = 0L,
    val totalDownloadBytes: Long = 0L,
    val isWarmingUp: Boolean = false,
    val statusDetail: String? = null,
    val recentChats: List<ChatRecord> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val activeTurns: List<ConversationTurn> = emptyList(),
    val isGenerating: Boolean = false,
    val updateStatus: UpdateCheckResult? = null,
    val isCheckingUpdates: Boolean = false
)

class MainScreenViewModel(
    private val chatHistoryStore: ChatHistoryStore? = null,
    private val memoryStore: MemoryStore? = null,
    private val agentTools: AndroidAgentTools? = null,
    private val updateChecker: UpdateChecker? = null,
    private val engine: GemmaAssistantEngine = GemmaAssistantEngine()
) : ViewModel() {

    private val _state = MutableStateFlow(GemmAHomeState())
    val state: StateFlow<GemmAHomeState> = _state.asStateFlow()

    private var activeJob: Job? = null

    init {
        refreshStatus()
        refreshChats()
        refreshMemories()
    }

    fun checkForUpdates() {
        val checker = updateChecker ?: return
        viewModelScope.launch {
            _state.update { it.copy(isCheckingUpdates = true) }
            val result = checker.checkForUpdates()
            _state.update { it.copy(isCheckingUpdates = false, updateStatus = result) }
        }
    }

    fun refreshChats() {
        val chats = chatHistoryStore?.loadChats().orEmpty()
        _state.update { it.copy(recentChats = chats) }
    }

    fun deleteChat(id: String) {
        chatHistoryStore?.deleteChat(id)
        refreshChats()
    }

    fun clearChats() {
        chatHistoryStore?.clearChats()
        _state.update { it.copy(recentChats = emptyList()) }
    }

    fun refreshMemories() {
        val items = memoryStore?.loadMemories().orEmpty()
        _state.update { it.copy(memories = items) }
    }

    fun addMemory(fact: String) {
        if (memoryStore?.addMemory(fact) == true) {
            refreshMemories()
        }
    }

    fun deleteMemory(id: String) {
        memoryStore?.deleteMemory(id)
        refreshMemories()
    }

    fun clearMemories() {
        memoryStore?.clearMemories()
        _state.update { it.copy(memories = emptyList()) }
    }

    fun startNewChat() {
        activeJob?.cancel()
        _state.update {
            it.copy(
                activeTurns = emptyList(),
                isGenerating = false,
                statusDetail = null
            )
        }
    }

    fun loadChatIntoView(record: ChatRecord) {
        activeJob?.cancel()
        _state.update {
            it.copy(
                activeTurns = listOf(
                    ConversationTurn(
                        prompt = record.prompt,
                        response = record.response,
                        actionSummary = record.actionSummary,
                        actionSuccess = true,
                        isStreaming = false
                    )
                ),
                isGenerating = false,
                statusDetail = null
            )
        }
    }

    fun sendPrompt(userPrompt: String, image: Bitmap? = null) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty() && image == null) return
        val effectivePrompt = trimmed.ifEmpty { "Describe this image." }

        activeJob?.cancel()
        val turnIndex = _state.value.activeTurns.size

        _state.update {
            it.copy(
                activeTurns = it.activeTurns + ConversationTurn(
                    prompt = effectivePrompt,
                    response = "",
                    image = image,
                    isStreaming = true
                ),
                isGenerating = true,
                statusDetail = null
            )
        }

        val runner = agentTools?.let { AgentRunner(engine, it) }

        activeJob = viewModelScope.launch {
            if (runner == null) {
                val rawBuilder = StringBuilder()
                engine.streamResponse(userQuery = effectivePrompt, image = image)
                    .catch { error ->
                        _state.update { state ->
                            val updatedTurns = state.activeTurns.toMutableList()
                            if (turnIndex in updatedTurns.indices) {
                                updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                    isStreaming = false,
                                    response = updatedTurns[turnIndex].response.ifEmpty {
                                        error.localizedMessage ?: "Failed to generate response."
                                    }
                                )
                            }
                            state.copy(activeTurns = updatedTurns, isGenerating = false, statusDetail = error.localizedMessage)
                        }
                    }
                    .onCompletion { cause ->
                        if (cause == null) {
                            val finalReply = rawBuilder.toString().trim()
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(response = finalReply, isStreaming = false)
                                }
                                state.copy(activeTurns = updatedTurns, isGenerating = false)
                            }
                            chatHistoryStore?.saveChat(prompt = effectivePrompt, response = finalReply, actionSummary = null)
                            refreshChats()
                        }
                    }
                    .collect { chunk ->
                        rawBuilder.append(chunk)
                        _state.update { state ->
                            val updatedTurns = state.activeTurns.toMutableList()
                            if (turnIndex in updatedTurns.indices) {
                                updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(response = rawBuilder.toString())
                            }
                            state.copy(activeTurns = updatedTurns)
                        }
                    }
                return@launch
            }

            runner.run(userPrompt = effectivePrompt, image = image)
                .catch { error ->
                    _state.update { state ->
                        val updatedTurns = state.activeTurns.toMutableList()
                        if (turnIndex in updatedTurns.indices) {
                            updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                isStreaming = false,
                                response = updatedTurns[turnIndex].response.ifEmpty {
                                    error.localizedMessage ?: "Failed to generate response."
                                }
                            )
                        }
                        state.copy(
                            activeTurns = updatedTurns,
                            isGenerating = false,
                            statusDetail = error.localizedMessage
                        )
                    }
                }
                .collect { event ->
                    when (event) {
                        is AgentStreamEvent.PartialText -> {
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(response = event.text)
                                }
                                state.copy(activeTurns = updatedTurns)
                            }
                        }
                        is AgentStreamEvent.RunningTool -> {
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                        actionSummary = "Running ${event.toolName.replace('_', ' ')}...",
                                        actionSuccess = true
                                    )
                                }
                                state.copy(activeTurns = updatedTurns)
                            }
                        }
                        is AgentStreamEvent.ToolCompleted -> {
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                        actionSummary = event.summary,
                                        actionSuccess = event.success
                                    )
                                }
                                state.copy(activeTurns = updatedTurns)
                            }
                        }
                        is AgentStreamEvent.Finished -> {
                            _state.update { state ->
                                val updatedTurns = state.activeTurns.toMutableList()
                                if (turnIndex in updatedTurns.indices) {
                                    updatedTurns[turnIndex] = updatedTurns[turnIndex].copy(
                                        response = event.finalText,
                                        actionSummary = event.actionSummary,
                                        actionSuccess = event.actionSuccess,
                                        isStreaming = false
                                    )
                                }
                                state.copy(activeTurns = updatedTurns, isGenerating = false)
                            }
                            chatHistoryStore?.saveChat(
                                prompt = effectivePrompt,
                                response = event.finalText,
                                actionSummary = event.actionSummary
                            )
                            refreshChats()
                            refreshMemories()
                        }
                    }
                }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.update { it.copy(availability = ModelAvailability.CHECKING, statusDetail = null) }
            val status = engine.queryAvailability()
            _state.update { it.copy(availability = status) }
            if (status == ModelAvailability.AVAILABLE) {
                engine.warmupModel()
            }
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            _state.update { it.copy(availability = ModelAvailability.DOWNLOADING, statusDetail = "Starting download...") }
            engine.downloadModel()
                .catch { error ->
                    _state.update {
                        it.copy(
                            availability = ModelAvailability.UNAVAILABLE,
                            statusDetail = error.localizedMessage ?: "Download failed."
                        )
                    }
                }
                .collect { downloadStatus ->
                    when (downloadStatus) {
                        is DownloadStatus.DownloadStarted -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.DOWNLOADING,
                                    totalDownloadBytes = downloadStatus.bytesToDownload,
                                    statusDetail = "Downloading Gemma 4..."
                                )
                            }
                        }
                        is DownloadStatus.DownloadProgress -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.DOWNLOADING,
                                    downloadProgressBytes = downloadStatus.totalBytesDownloaded
                                )
                            }
                        }
                        DownloadStatus.DownloadCompleted -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.AVAILABLE,
                                    statusDetail = "Gemma 4 is ready on-device."
                                )
                            }
                        }
                        is DownloadStatus.DownloadFailed -> {
                            _state.update {
                                it.copy(
                                    availability = ModelAvailability.UNAVAILABLE,
                                    statusDetail = downloadStatus.e.localizedMessage ?: "Download failed."
                                )
                            }
                        }
                    }
                }
        }
    }

    fun warmupModel() {
        viewModelScope.launch {
            _state.update { it.copy(isWarmingUp = true, statusDetail = "Warming up Gemma 4 in AICore...") }
            engine.warmupModel()
            _state.update { it.copy(isWarmingUp = false, statusDetail = "Model warmed up and ready.") }
        }
    }

    override fun onCleared() {
        activeJob?.cancel()
        engine.shutdown()
        super.onCleared()
    }
}
