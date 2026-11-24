package com.kairo.agent.agent

import android.graphics.Bitmap
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.engine.GemmaAssistantEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class NerdStats(
    val tokensPerSec: Float = 0f,
    val ttftMs: Long = 0L,
    val ramUsedMb: Long = 0L
)

sealed interface AgentStreamEvent {
    data class PartialText(val text: String, val stats: NerdStats) : AgentStreamEvent
    data class RunningTool(val toolName: String) : AgentStreamEvent
    data class ToolCompleted(val summary: String, val success: Boolean) : AgentStreamEvent
    data class Finished(
        val finalText: String,
        val actionSummary: String?,
        val actionSuccess: Boolean,
        val stats: NerdStats
    ) : AgentStreamEvent
}

/**
 * Runs a multi-turn, two-step on-device agent loop with:
 * - Conversation history retention
 * - Persona switching
 * - Automatic <memory> extraction
 * - Live tok/s, TTFT, and RAM stats
 */
class AgentRunner(
    private val engine: GemmaAssistantEngine,
    private val tools: AndroidAgentTools
) {
    fun run(
        userPrompt: String,
        image: Bitmap? = null,
        screenText: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        persona: AgentPersona = AgentPersona.DEFAULT,
        thinkHarder: Boolean = false,
        settings: AgentSettings = AgentSettings()
    ): Flow<AgentStreamEvent> = flow {
        val startMs = System.currentTimeMillis()
        var firstTokenMs = 0L
        var chunkCount = 0

        val initialPrompt = tools.buildAgentPrompt(
            userMessage = userPrompt,
            screenText = screenText,
            conversationHistory = conversationHistory,
            persona = persona,
            rootPowerMode = settings.rootPowerMode
        )
        val firstTurnRaw = StringBuilder()

        engine.streamResponse(
            userQuery = initialPrompt,
            image = image,
            thinkHarder = thinkHarder,
            settings = settings
        ).collect { chunk ->
            if (firstTokenMs == 0L) firstTokenMs = System.currentTimeMillis()
            chunkCount += chunk.split(Regex("\\s+")).count { it.isNotEmpty() }.coerceAtLeast(1)
            firstTurnRaw.append(chunk)
            val visible = tools.stripToolCallMarkup(firstTurnRaw.toString())
            if (visible.isNotEmpty() && !tools.hasToolCall(firstTurnRaw.toString())) {
                emit(AgentStreamEvent.PartialText(visible, computeStats(startMs, firstTokenMs, chunkCount)))
            }
        }

        val rawOutput = firstTurnRaw.toString()
        val autoSavedMemories = tools.extractAndSaveMemories(rawOutput)

        if (!tools.hasToolCall(rawOutput)) {
            val clean = tools.stripToolCallMarkup(rawOutput)
            val memSummary = if (autoSavedMemories.isNotEmpty()) "Remembered: ${autoSavedMemories.first()}" else null
            emit(
                AgentStreamEvent.Finished(
                    finalText = clean,
                    actionSummary = memSummary,
                    actionSuccess = true,
                    stats = computeStats(startMs, firstTokenMs, chunkCount)
                )
            )
            return@flow
        }

        val detectedTool = tools.extractToolName(rawOutput) ?: "action"
        emit(AgentStreamEvent.RunningTool(detectedTool))

        val actionResult = tools.executeFromModelOutput(
            rawOutput = rawOutput,
            attachedImage = image,
            rootPowerMode = settings.rootPowerMode
        )
        val summary = actionResult.summary
        if (summary != null) {
            emit(AgentStreamEvent.ToolCompleted(summary = summary, success = actionResult.success))
        }

        val followUpPrompt = tools.buildFollowUpPrompt(
            userMessage = userPrompt,
            toolName = actionResult.toolName ?: detectedTool,
            toolResult = actionResult.resultData ?: summary.orEmpty(),
            success = actionResult.success,
            persona = persona
        )

        val secondTurnRaw = StringBuilder()
        engine.streamResponse(
            userQuery = followUpPrompt,
            thinkHarder = thinkHarder,
            settings = settings
        ).collect { chunk ->
            if (firstTokenMs == 0L) firstTokenMs = System.currentTimeMillis()
            chunkCount += chunk.split(Regex("\\s+")).count { it.isNotEmpty() }.coerceAtLeast(1)
            secondTurnRaw.append(chunk)
            val visible = tools.stripToolCallMarkup(secondTurnRaw.toString())
            if (visible.isNotEmpty()) {
                emit(AgentStreamEvent.PartialText(visible, computeStats(startMs, firstTokenMs, chunkCount)))
            }
        }

        val finalClean = tools.stripToolCallMarkup(secondTurnRaw.toString())
            .ifEmpty { tools.stripToolCallMarkup(rawOutput) }
            .ifEmpty { summary.orEmpty() }

        emit(
            AgentStreamEvent.Finished(
                finalText = finalClean,
                actionSummary = summary,
                actionSuccess = actionResult.success,
                stats = computeStats(startMs, firstTokenMs, chunkCount)
            )
        )
    }

    private fun computeStats(startMs: Long, firstTokenMs: Long, tokens: Int): NerdStats {
        val now = System.currentTimeMillis()
        val elapsedSec = ((now - startMs).coerceAtLeast(1L)) / 1000f
        val ttft = if (firstTokenMs > 0L) (firstTokenMs - startMs).coerceAtLeast(0L) else 0L
        val rt = Runtime.getRuntime()
        val ramMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)
        return NerdStats(
            tokensPerSec = tokens / elapsedSec,
            ttftMs = ttft,
            ramUsedMb = ramMb
        )
    }
}
