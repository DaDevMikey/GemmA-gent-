package com.kairo.agent.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kairo.agent.AssistantOverlayActivity
import com.kairo.agent.MainActivity
import com.kairo.agent.R

internal fun buildWidgetPendingIntent(
    context: Context,
    requestCode: Int,
    targetClass: Class<*>,
    startVoice: Boolean = false
): PendingIntent {
    val intent = Intent(context, targetClass).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (startVoice) {
            putExtra("start_voice", true)
        }
    }
    return PendingIntent.getActivity(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/**
 * Standard Home Screen Widget for Kairo: quick-ask overlay, voice ask, and full chat launcher.
 */
class KairoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val overlayIntent = buildWidgetPendingIntent(context, 101, AssistantOverlayActivity::class.java)
        val voiceIntent = buildWidgetPendingIntent(context, 102, AssistantOverlayActivity::class.java, startVoice = true)
        val chatIntent = buildWidgetPendingIntent(context, 103, MainActivity::class.java)

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_kairo_home).apply {
                setOnClickPendingIntent(R.id.widget_ask_area, overlayIntent)
                setOnClickPendingIntent(R.id.widget_icon, overlayIntent)
                setOnClickPendingIntent(R.id.widget_voice_btn, voiceIntent)
                setOnClickPendingIntent(R.id.widget_chat_btn, chatIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

/**
 * Compact Cover-Screen Widget for foldable cover displays and lock/cover launchers.
 */
class KairoCoverWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val overlayIntent = buildWidgetPendingIntent(context, 201, AssistantOverlayActivity::class.java)
        val voiceIntent = buildWidgetPendingIntent(context, 202, AssistantOverlayActivity::class.java, startVoice = true)

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_kairo_cover).apply {
                setOnClickPendingIntent(R.id.cover_widget_root, overlayIntent)
                setOnClickPendingIntent(R.id.cover_widget_ask, overlayIntent)
                setOnClickPendingIntent(R.id.cover_widget_icon, overlayIntent)
                setOnClickPendingIntent(R.id.cover_widget_mic, voiceIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
