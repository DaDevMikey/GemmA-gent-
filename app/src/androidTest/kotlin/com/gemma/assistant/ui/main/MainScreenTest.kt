package com.gemma.assistant.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.gemma.assistant.engine.ModelAvailability
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.gemma.assistant.ui.main.MainScreenContent]. */
class MainScreenTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setup() {
        composeTestRule.setContent {
            MainScreenContent(
                state = GemmAHomeState(availability = ModelAvailability.AVAILABLE),
                isListening = false,
                partialTranscript = "",
                wakeWordEnabled = false,
                onSendPrompt = {},
                onNewChat = {},
                onSelectChat = {},
                onClearChats = {},
                onAddMemory = {},
                onDeleteMemory = {},
                onClearMemories = {},
                onToggleWakeWord = {},
                onRefreshStatus = {},
                onDownloadModel = {},
                onWarmupModel = {},
                onMicClick = {},
                onOpenAssistantSettings = {}
            )
        }
    }

    @Test
    fun brandAndGreeting_areDisplayed() {
        composeTestRule.onNodeWithText("GemmA").assertExists()
        composeTestRule.onNodeWithText("Let's get started").assertExists()
    }
}
