package com.kairo.agent.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class MemoryItem(
    val id: String,
    val fact: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

/**
 * Persists long-term user memories so GemmA remembers facts and preferences across sessions.
 */
class MemoryStore(context: Context) {

    private val memoryFile = File(context.applicationContext.filesDir, "gemma_memory.json")

    fun loadMemories(): List<MemoryItem> {
        if (!memoryFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(memoryFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val fact = obj.optString("fact").trim()
                    if (fact.isNotEmpty()) {
                        add(
                            MemoryItem(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                fact = fact,
                                createdAtMs = obj.optLong("createdAtMs", 0L)
                            )
                        )
                    }
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun normalizeForDedup(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").replace(Regex("\\s+"), " ").trim()

    fun addMemory(fact: String): Boolean {
        val cleaned = fact.trim()
        if (cleaned.isEmpty()) return false

        val normNew = normalizeForDedup(cleaned)
        if (normNew.isEmpty()) return false
        val newWords = normNew.split(" ").filter { it.length > 2 }.toSet()

        val existing = loadMemories()
        val isDuplicate = existing.any { item ->
            val normOld = normalizeForDedup(item.fact)
            if (normOld == normNew || normOld.contains(normNew) || normNew.contains(normOld)) {
                true
            } else if (newWords.size >= 3) {
                val oldWords = normOld.split(" ").filter { it.length > 2 }.toSet()
                val overlap = newWords.intersect(oldWords).size
                overlap.toFloat() / maxOf(newWords.size, oldWords.size).toFloat() >= 0.75f
            } else {
                false
            }
        }
        if (isDuplicate) {
            return false
        }

        val updated = buildList {
            add(MemoryItem(id = UUID.randomUUID().toString(), fact = cleaned))
            addAll(existing)
        }.take(MAX_MEMORIES)

        writeMemories(updated)
        return true
    }

    fun deleteMemory(id: String) {
        val updated = loadMemories().filterNot { it.id == id }
        writeMemories(updated)
    }

    fun deleteMatching(query: String): Int {
        val clean = query.trim()
        if (clean.isEmpty()) return 0
        val current = loadMemories()
        val remaining = current.filterNot { it.fact.contains(clean, ignoreCase = true) }
        val removed = current.size - remaining.size
        if (removed > 0) writeMemories(remaining)
        return removed
    }

    fun clearMemories() {
        runCatching {
            if (memoryFile.exists()) memoryFile.delete()
        }
    }

    fun formatForPrompt(): String {
        val items = loadMemories()
        if (items.isEmpty()) return ""
        return buildString {
            appendLine("Known facts about the user (Memory):")
            items.forEach { appendLine("- ${it.fact}") }
        }
    }

    private fun writeMemories(items: List<MemoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("fact", item.fact)
                    put("createdAtMs", item.createdAtMs)
                }
            )
        }
        runCatching { memoryFile.writeText(array.toString()) }
    }

    private companion object {
        const val MAX_MEMORIES = 50
    }
}
