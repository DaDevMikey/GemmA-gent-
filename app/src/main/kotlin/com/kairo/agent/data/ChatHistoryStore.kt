package com.kairo.agent.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SavedTurn(
    val prompt: String,
    val response: String,
    val actionSummary: String? = null
)

data class ChatRecord(
    val id: String,
    val prompt: String,
    val response: String,
    val actionSummary: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val turns: List<SavedTurn> = emptyList()
)

/**
 * Persists recent Kairo conversations (multi-turn sessions) to app-private storage as JSON.
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
                    val turnsArray = obj.optJSONArray("turns")
                    val parsedTurns = if (turnsArray != null) {
                        buildList {
                            for (j in 0 until turnsArray.length()) {
                                val t = turnsArray.getJSONObject(j)
                                add(
                                    SavedTurn(
                                        prompt = t.optString("prompt"),
                                        response = t.optString("response"),
                                        actionSummary = t.optString("actionSummary").takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                        }
                    } else {
                        emptyList()
                    }
                    val p = obj.optString("prompt")
                    val r = obj.optString("response")
                    val a = obj.optString("actionSummary").takeIf { it.isNotBlank() }
                    add(
                        ChatRecord(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            prompt = p,
                            response = r,
                            actionSummary = a,
                            timestampMs = obj.optLong("timestampMs", 0L),
                            turns = parsedTurns.ifEmpty {
                                if (p.isNotBlank() || r.isNotBlank()) listOf(SavedTurn(p, r, a)) else emptyList()
                            }
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun saveChat(prompt: String, response: String, actionSummary: String?, sessionId: String? = null) {
        val trimmedPrompt = prompt.trim()
        val trimmedResponse = response.trim()
        if (trimmedPrompt.isEmpty() && trimmedResponse.isEmpty()) return

        val newTurn = SavedTurn(
            prompt = trimmedPrompt,
            response = trimmedResponse,
            actionSummary = actionSummary?.takeIf { it.isNotBlank() }
        )
        val existingChats = loadChats()
        val existingMatch = if (!sessionId.isNullOrBlank()) existingChats.firstOrNull { it.id == sessionId } else null

        val updatedRecord = if (existingMatch != null) {
            existingMatch.copy(
                turns = existingMatch.turns + newTurn,
                timestampMs = System.currentTimeMillis()
            )
        } else {
            ChatRecord(
                id = sessionId ?: UUID.randomUUID().toString(),
                prompt = trimmedPrompt,
                response = trimmedResponse,
                actionSummary = newTurn.actionSummary,
                timestampMs = System.currentTimeMillis(),
                turns = listOf(newTurn)
            )
        }

        val updated = buildList {
            add(updatedRecord)
            addAll(existingChats.filterNot { it.id == updatedRecord.id })
        }.take(MAX_CHATS)

        writeChats(updated)
    }

    fun deleteChat(id: String) {
        val updated = loadChats().filterNot { it.id == id }
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
                    val turnsArr = JSONArray()
                    chat.turns.forEach { t ->
                        turnsArr.put(
                            JSONObject().apply {
                                put("prompt", t.prompt)
                                put("response", t.response)
                                put("actionSummary", t.actionSummary.orEmpty())
                            }
                        )
                    }
                    put("turns", turnsArr)
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
