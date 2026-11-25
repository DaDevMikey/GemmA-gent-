package com.kairo.agent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.kairo.agent.AssistantOverlayActivity
import com.kairo.agent.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Optional foreground service that listens for "Hey GemmA" / "GemmA" and launches the overlay.
 */
class WakeWordService : Service() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var shouldListen = false
    private var lastTriggerMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        _isRunning.value = true
        shouldListen = true
        mainHandler.post { startListeningCycle() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startListeningCycle() {
        if (!shouldListen || !SpeechRecognizer.isRecognitionAvailable(this)) return

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit

                    override fun onError(error: Int) {
                        scheduleRestart(delayMs = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 800L else 350L)
                    }

                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        checkWakePhrase(text)
                        scheduleRestart(delayMs = 300L)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        checkWakePhrase(partial)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { speechRecognizer?.startListening(intent) }
            .onFailure { scheduleRestart(delayMs = 1000L) }
    }

    private fun checkWakePhrase(rawTranscript: String) {
        val normalized = rawTranscript.lowercase(Locale.ROOT)
        val triggered = WAKE_PHRASES.any { normalized.contains(it) }
        val now = System.currentTimeMillis()
        if (triggered && now - lastTriggerMs > COOLDOWN_MS) {
            lastTriggerMs = now
            val overlayIntent = Intent(this, AssistantOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(AssistantOverlayActivity.EXTRA_AUTO_LISTEN, true)
            }
            startActivity(overlayIntent)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!shouldListen) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ startListeningCycle() }, delayMs)
    }

    override fun onDestroy() {
        shouldListen = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        _isRunning.value = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GemmA Wake Word",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Listens for \"Hey GemmA\" in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WakeWordService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kairo Wake Word Active")
            .setContentText("Say \"Hey Kairo\" to open the agent overlay")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gemma_wake_word"
        private const val NOTIFICATION_ID = 4201
        private const val ACTION_STOP = "com.kairo.agent.action.STOP_WAKE_WORD"
        private const val COOLDOWN_MS = 3500L
        // Recognizes "Kairo" (/kaj/roo/) and common speech-to-text phonetic matches
        private val WAKE_PHRASES = listOf(
            "hey kairo", "ok kairo", "hi kairo", "kairo",
            "hey cairo", "ok cairo", "cairo",
            "hey kyro", "ok kyro", "kyro",
            "kai roo", "ky roo", "kairoo", "kaiju", "karo"
        )

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }
}
