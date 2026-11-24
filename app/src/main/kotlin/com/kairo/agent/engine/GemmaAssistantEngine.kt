package com.kairo.agent.engine

import android.graphics.Bitmap
import android.util.Base64
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.kairo.agent.data.AgentSettings
import com.kairo.agent.data.EngineBackendMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class ModelAvailability {
    CHECKING,
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE
}

/**
 * Unified local inference engine supporting:
 * 1) Android AICore (Gemma 4 via ML Kit GenAI Prompt API)
 * 2) Local OpenAI-compatible HTTP endpoint (for non-AICore devices running local models like Z Fold 8 / llama.cpp / Ollama)
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
        thinkHarder: Boolean = false,
        settings: AgentSettings = AgentSettings()
    ): Flow<String> {
        val useLocalHttp = when (settings.backendMode) {
            EngineBackendMode.LOCAL_HTTP -> true
            EngineBackendMode.AICORE -> false
            EngineBackendMode.AUTO -> false // handled dynamically below
        }

        if (useLocalHttp) {
            return streamFromLocalHttp(
                userQuery = userQuery,
                image = image,
                endpointUrl = settings.localEndpointUrl,
                modelName = settings.localModelName,
                temperatureValue = if (thinkHarder) 0.2f else 0.4f
            )
        }

        return callbackFlow {
            val status = if (settings.backendMode == EngineBackendMode.AUTO) {
                runCatching { generativeClient.checkStatus() }.getOrNull()
            } else {
                FeatureStatus.AVAILABLE
            }

            if (settings.backendMode == EngineBackendMode.AUTO && status != FeatureStatus.AVAILABLE) {
                // Fallback to local HTTP endpoint if AICore isn't available on this device
                val localJob = launch {
                    try {
                        streamFromLocalHttp(
                            userQuery = userQuery,
                            image = image,
                            endpointUrl = settings.localEndpointUrl,
                            modelName = settings.localModelName,
                            temperatureValue = if (thinkHarder) 0.2f else 0.4f
                        ).collect { trySend(it) }
                        close()
                    } catch (t: Throwable) {
                        close(t)
                    }
                }
                awaitClose { localJob.cancel() }
                return@callbackFlow
            }

            val temp = if (thinkHarder) 0.2f else 0.4f
            val promptRequest = if (image != null) {
                generateContentRequest(ImagePart(image), TextPart(userQuery)) {
                    temperature = temp
                    if (thinkHarder) enableThinking = true
                }
            } else {
                generateContentRequest(TextPart(userQuery)) {
                    temperature = temp
                    if (thinkHarder) enableThinking = true
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
    }

    private fun streamFromLocalHttp(
        userQuery: String,
        image: Bitmap?,
        endpointUrl: String,
        modelName: String,
        temperatureValue: Float
    ): Flow<String> = flow {
        val conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            readTimeout = 60000
            setRequestProperty("Content-Type", "application/json")
        }

        val contentArray = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", userQuery))
            if (image != null) {
                val baos = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                put(
                    JSONObject().put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64"))
                )
            }
        }

        val payload = JSONObject().apply {
            put("model", modelName)
            put("stream", true)
            put("temperature", temperatureValue.toDouble())
            put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put("content", contentArray)
                )
            )
        }

        conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            throw IllegalStateException("Local model endpoint error ($code): $err")
        }

        conn.inputStream.bufferedReader().useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (!trimmed.startsWith("data:")) continue
                val data = trimmed.removePrefix("data:").trim()
                if (data == "[DONE]") break
                runCatching {
                    val obj = JSONObject(data)
                    val delta = obj.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?.optString("content")
                        .orEmpty()
                    if (delta.isNotEmpty()) emit(delta)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun shutdown() {
        runCatching { generativeClient.close() }
    }
}
