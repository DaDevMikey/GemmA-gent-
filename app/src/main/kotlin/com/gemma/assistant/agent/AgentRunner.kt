package com.gemma.assistant.agent

import android.graphics.Bitmap
import com.gemma.assistant.engine.GemmaAssistantEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface AgentStreamEvent {
    data class PartialText(val text: String) : AgentStreamEvent
    data class RunningTool(val toolName: String) : AgentStreamEvent
    data class ToolCompleted(val summary: String, val success: Boolean) : AgentStreamEvent
    data class Finished(
        val finalText: String,
        val actionSummary: String?,
        val actionSuccess: Boolean
    ) : AgentStreamEvent
}

/**
 * Runs a two-step on-device agent loop:
 * 1) Streams Gemma 4's initial response (or tool call).
 * 2) If a <tool_call> is emitted, executes the Android tool, then feeds the real tool result
 *    back into Gemma 4 so it can answer in natural full sentences.
 */
class AgentRunner(
    private val engine: GemmaAssistantEngine,
    private val tools: AndroidAgentTools
) {
    fun run(
        userPrompt: String,
        image: Bitmap? = null,
        screenText: String? = null
    ): Flow<AgentStreamEvent> = flow {
        val initialPrompt = tools.buildAgentPrompt(userPrompt, screenText)
        val firstTurnRaw = StringBuilder()

        engine.streamResponse(userQuery = initialPrompt, image = image).collect { chunk ->
            firstTurnRaw.append(chunk)
            val visible = tools.stripToolCallMarkup(firstTurnRaw.toString())
            if (visible.isNotEmpty() && !tools.hasToolCall(firstTurnRaw.toString())) {
                emit(AgentStreamEvent.PartialText(visible))
            }
        }

        val rawOutput = firstTurnRaw.toString()
        if (!tools.hasToolCall(rawOutput)) {
            val clean = tools.stripToolCallMarkup(rawOutput)
            emit(AgentStreamEvent.Finished(finalText = clean, actionSummary = null, actionSuccess = true))
            return@flow
        }

        val detectedTool = tools.extractToolName(rawOutput) ?: "action"
        emit(AgentStreamEvent.RunningTool(detectedTool))

        val actionResult = tools.executeFromModelOutput(rawOutput)
        val summary = actionResult.summary
        if (summary != null) {
            emit(AgentStreamEvent.ToolCompleted(summary = summary, success = actionResult.success))
        }

        // Second pass: feed the real tool result back into Gemma 4 for a full-sentence reply
        val followUpPrompt = tools.buildFollowUpPrompt(
            userMessage = userPrompt,
            toolName = actionResult.toolName ?: detectedTool,
            toolResult = actionResult.resultData ?: summary.orEmpty(),
            success = actionResult.success
        )

        val secondTurnRaw = StringBuilder()
        engine.streamResponse(userQuery = followUpPrompt).collect { chunk ->
            secondTurnRaw.append(chunk)
            val visible = tools.stripToolCallMarkup(secondTurnRaw.toString())
            if (visible.isNotEmpty()) {
                emit(AgentStreamEvent.PartialText(visible))
            }
        }

        val finalClean = tools.stripToolCallMarkup(secondTurnRaw.toString())
            .ifEmpty { tools.stripToolCallMarkup(rawOutput) }
            .ifEmpty { summary.orEmpty() }

        emit(
            AgentStreamEvent.Finished(
                finalText = finalClean,
                actionSummary = summary,
                actionSuccess = actionResult.success
            )
        )
    }
}
