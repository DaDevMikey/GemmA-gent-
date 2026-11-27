package com.kairo.agent.data

import android.graphics.Bitmap

data class OverlayHandoffTurn(
    val prompt: String,
    val response: String,
    val actionSummary: String? = null,
    val image: Bitmap? = null
)

/**
 * In-memory handoff for transferring the exact active popup conversation into MainActivity
 * without parcelable Binder size limits (which can crash on Bitmap extras).
 */
object OverlayHandoffHolder {
    @Volatile
    private var pendingTurns: List<OverlayHandoffTurn>? = null

    fun setTurns(turns: List<OverlayHandoffTurn>) {
        pendingTurns = turns
    }

    fun consumeTurns(): List<OverlayHandoffTurn>? {
        val current = pendingTurns
        pendingTurns = null
        return current
    }
}
