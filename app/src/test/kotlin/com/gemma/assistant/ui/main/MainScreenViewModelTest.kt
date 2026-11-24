package com.gemma.assistant.ui.main

import com.gemma.assistant.engine.ModelAvailability
import junit.framework.TestCase.assertEquals
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun homeState_defaultsToChecking() {
        val state = GemmAHomeState()
        assertEquals(ModelAvailability.CHECKING, state.availability)
    }
}
