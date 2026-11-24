package com.kairo.agent.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

data class CapturedNotification(
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestampMs: Long
)

object NotificationCatchUpStore {
    private val notifications = CopyOnWriteArrayList<CapturedNotification>()
    private const val MAX_ITEMS = 25

    fun add(item: CapturedNotification) {
        notifications.add(0, item)
        while (notifications.size > MAX_ITEMS) {
            notifications.removeAt(notifications.lastIndex)
        }
    }

    fun getRecent(): List<CapturedNotification> = notifications.toList()

    fun clear() = notifications.clear()

    fun isAccessGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        val flat = ComponentName(context, KairoNotificationListenerService::class.java).flattenToString()
        return enabled.contains(flat)
    }

    fun formatSummary(): String {
        val items = getRecent()
        if (items.isEmpty()) return "No recent notifications captured."
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        return items.take(15).joinToString("\n") { n ->
            "[${fmt.format(Date(n.timestampMs))}] ${n.appLabel} - ${n.title}: ${n.text}"
        }
    }
}

/**
 * Captures incoming messaging/app notifications locally so Kairo can summarize unread messages ("Catch me up").
 */
class KairoNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName || (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrElse { pkg }

        NotificationCatchUpStore.add(
            CapturedNotification(
                packageName = pkg,
                appLabel = appLabel,
                title = title,
                text = text,
                timestampMs = sbn.postTime
            )
        )
    }
}
