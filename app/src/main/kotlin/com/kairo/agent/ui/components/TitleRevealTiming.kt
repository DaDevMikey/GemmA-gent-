package com.kairo.agent.ui.components

/** Twidget's seven-frame letter cadence, half-second fade and one-second settle. */
internal object TitleRevealTiming {
    const val STAGGER_MS = 7f / 60f * 1000f
    const val FADE_MS = 500f
    const val MOVEMENT_MS = 1000L

    fun durationMs(visibleGlyphs: Int): Long = if (visibleGlyphs <= 0) 0L
        else ((visibleGlyphs - 1) * STAGGER_MS).toLong() + MOVEMENT_MS
}
