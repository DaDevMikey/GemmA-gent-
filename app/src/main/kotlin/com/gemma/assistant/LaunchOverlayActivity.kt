package com.gemma.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Dedicated launcher activity that immediately opens the floating GemmA overlay.
 */
class LaunchOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, AssistantOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
        finish()
    }
}
