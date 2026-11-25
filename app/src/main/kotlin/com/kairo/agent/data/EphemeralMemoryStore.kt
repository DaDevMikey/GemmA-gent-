package com.kairo.agent.data

import android.content.ClipboardManager
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class EphemeralMemoryEntry(
    val text: String,
    val source: String, // "clipboard" or "screen"
    val timestampMs: Long
)

/**
 * Rolling 7-day local-only ephemeral memory of clipboard snippets and screen context.
 * Never leaves the device.
 */
class EphemeralMemoryStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("kairo_ephemeral_memory", Context.MODE_PRIVATE)

    fun recordClipboardIfPresent() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = cm.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
        if (text.length in 2..1000) {
            addEntry(text, "clipboard")
        }
    }

    fun addEntry(text: String, source: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val now = System.currentTimeMillis()
        val cutoff = now - SEVEN_DAYS_MS
        val existing = loadEntries().filter { it.timestampMs >= cutoff && it.text != trimmed }
        val updated = (listOf(EphemeralMemoryEntry(trimmed, source, now)) + existing).take(MAX_ENTRIES)
        val arr = JSONArray()
        updated.forEach { entry ->
            arr.put(JSONObject().apply {
                put("text", entry.text)
                put("source", entry.source)
                put("ts", entry.timestampMs)
            })
        }
        prefs.edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    fun loadEntries(): List<EphemeralMemoryEntry> {
        val raw = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        val cutoff = now - SEVEN_DAYS_MS
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val ts = obj.optLong("ts", 0L)
                    if (ts >= cutoff) {
                        add(
                            EphemeralMemoryEntry(
                                text = obj.optString("text"),
                                source = obj.optString("source", "clipboard"),
                                timestampMs = ts
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun search(query: String): List<EphemeralMemoryEntry> {
        recordClipboardIfPresent()
        val entries = loadEntries()
        if (query.isBlank()) return entries.take(8)
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
        if (terms.isEmpty()) return entries.take(8)
        return entries.filter { entry ->
            val lower = entry.text.lowercase()
            terms.any { lower.contains(it) }
        }.take(8)
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
        const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L
        const val MAX_ENTRIES = 60
    }
}
