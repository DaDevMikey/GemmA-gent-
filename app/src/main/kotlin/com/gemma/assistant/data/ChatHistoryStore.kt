package com.gemma.assistant.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ChatRecord(
    val id: String,
    val prompt: String,
    val response: String,
    val actionSummary: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Persists recent GemmA conversations to app-private storage as JSON.
 */
class ChatHistoryStore(context: Context) {

    private val historyFile = File(context.applicationContext.filesDir, "recent_chats.json")

    fun loadChats(): List<ChatRecord> {
        if (!historyFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(historyFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        ChatRecord(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            prompt = obj.optString("prompt"),
                            response = obj.optString("response"),
                            actionSummary = obj.optString("actionSummary").takeIf { it.isNotBlank() },
                            timestampMs = obj.optLong("timestampMs", 0L)
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun saveChat(prompt: String, response: String, actionSummary: String?) {
        val trimmedPrompt = prompt.trim()
        val trimmedResponse = response.trim()
        if (trimmedPrompt.isEmpty() && trimmedResponse.isEmpty()) return

        val updated = buildList {
            add(
                ChatRecord(
                    id = UUID.randomUUID().toString(),
                    prompt = trimmedPrompt,
                    response = trimmedResponse,
                    actionSummary = actionSummary?.takeIf { it.isNotBlank() },
                    timestampMs = System.currentTimeMillis()
                )
            )
            addAll(loadChats())
        }.take(MAX_CHATS)

        writeChats(updated)
    }

    fun clearChats() {
        runCatching {
            if (historyFile.exists()) historyFile.delete()
        }
    }

    private fun writeChats(chats: List<ChatRecord>) {
        val array = JSONArray()
        chats.forEach { chat ->
            array.put(
                JSONObject().apply {
                    put("id", chat.id)
                    put("prompt", chat.prompt)
                    put("response", chat.response)
                    put("actionSummary", chat.actionSummary.orEmpty())
                    put("timestampMs", chat.timestampMs)
                }
            )
        }
        runCatching {
            historyFile.writeText(array.toString())
        }
    }

    private companion object {
        const val MAX_CHATS = 30
    }
}
