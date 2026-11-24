package com.gemma.assistant.engine

import android.graphics.Bitmap
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class ModelAvailability {
    CHECKING,
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE
}

/**
 * Wraps the ML Kit GenAI Prompt client to run Gemma 4 (text + image) on-device via AICore.
 * Uses StreamingCallback inside our own callbackFlow to avoid coroutine ABI mismatches
 * in prebuilt generateContentStream.
 */
class GemmaAssistantEngine {

    private val generativeClient by lazy { Generation.getClient() }

    suspend fun queryAvailability(): ModelAvailability {
        val status = runCatching { generativeClient.checkStatus() }.getOrNull()
        return when (status) {
            FeatureStatus.AVAILABLE -> ModelAvailability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> ModelAvailability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> ModelAvailability.DOWNLOADING
            else -> ModelAvailability.UNAVAILABLE
        }
    }

    fun downloadModel(): Flow<DownloadStatus> = generativeClient.download()

    suspend fun warmupModel() {
        runCatching { generativeClient.warmup() }
    }

    fun streamResponse(
        userQuery: String,
        image: Bitmap? = null,
        temperatureValue: Float = 0.4f
    ): Flow<String> = callbackFlow {
        val promptRequest = if (image != null) {
            generateContentRequest(ImagePart(image), TextPart(userQuery)) {
                temperature = temperatureValue
            }
        } else {
            generateContentRequest(TextPart(userQuery)) {
                temperature = temperatureValue
            }
        }

        val callback = StreamingCallback { newText ->
            if (newText.isNotEmpty()) {
                trySend(newText)
            }
        }
        val job = launch {
            try {
                generativeClient.generateContent(promptRequest, callback)
                close()
            } catch (t: Throwable) {
                close(t)
            }
        }
        awaitClose { job.cancel() }
    }

    fun shutdown() {
        runCatching { generativeClient.close() }
    }
}
