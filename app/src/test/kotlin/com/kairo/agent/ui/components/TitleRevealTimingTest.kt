package com.kairo.agent.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleRevealTimingTest {
    @Test fun emptyTitleHasNoPendingAnimation() = assertEquals(0L, TitleRevealTiming.durationMs(0))
    @Test fun oneGlyphUsesTheFullSettleDuration() = assertEquals(1000L, TitleRevealTiming.durationMs(1))
    @Test fun finalGlyphGetsItsOwnFullSettleAfterTheSevenFrameStagger() {
        assertEquals(2050L, TitleRevealTiming.durationMs(10))
        assertEquals(116.66667f, TitleRevealTiming.STAGGER_MS, .0001f)
    }
}
