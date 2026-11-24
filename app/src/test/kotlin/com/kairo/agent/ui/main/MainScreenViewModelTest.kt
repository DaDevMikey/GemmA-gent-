package com.kairo.agent.ui.main

import com.kairo.agent.engine.ModelAvailability
import junit.framework.TestCase.assertEquals
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun homeState_defaultsToChecking() {
        val state = GemmAHomeState()
        assertEquals(ModelAvailability.CHECKING, state.availability)
    }
}
