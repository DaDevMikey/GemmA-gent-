package com.gemma.assistant.service

import android.app.assist.AssistStructure
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.gemma.assistant.AssistantOverlayActivity
import com.gemma.assistant.agent.ScreenContextHolder

/**
 * Captures on-screen text and screenshot when GemmA is invoked as the system assistant,
 * then launches the translucent GemmA overlay.
 */
class GemmAVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return object : VoiceInteractionSession(this) {

            override fun onHandleAssist(state: AssistState) {
                super.onHandleAssist(state)
                val structure = state.assistStructure ?: return
                val extracted = extractTextFromStructure(structure)
                if (extracted.isNotBlank()) {
                    ScreenContextHolder.extractedScreenText = extracted
                }
            }

            override fun onHandleScreenshot(screenshot: Bitmap?) {
                super.onHandleScreenshot(screenshot)
                if (screenshot != null) {
                    ScreenContextHolder.capturedScreenshot =
                        ScreenContextHolder.scaleDownIfNeeded(screenshot)
                }
            }

            override fun onShow(args: Bundle?, showFlags: Int) {
                super.onShow(args, showFlags)
                val intent = Intent(context, AssistantOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(AssistantOverlayActivity.EXTRA_FROM_ASSIST_SESSION, true)
                }
                context.startActivity(intent)
                hide()
            }
        }
    }

    private fun extractTextFromStructure(structure: AssistStructure): String {
        val lines = LinkedHashSet<String>()
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode ?: continue
            collectNodeText(root, lines)
        }
        return lines.joinToString("\n").take(3000)
    }

    private fun collectNodeText(node: AssistStructure.ViewNode, out: MutableSet<String>) {
        if (out.size >= 120) return
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let(out::add)
        for (i in 0 until node.childCount) {
            collectNodeText(node.getChildAt(i), out)
        }
    }
}
