package com.ambient.launcher

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { processNotification(it) }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("NotificationService", "Notification from $packageName: $title - $text")

        // Reading Apps
        if (packageName.contains("perlego") || packageName.contains("audible")) {
            // Update ReadingState (using a singleton or message bus for simplicity in this prototype)
            // In a real app, we'd bind to a ViewModel or use a Repository.
            ReadingServiceBus.updateReading(ReadingState(
                title = title,
                author = text,
                sourceApp = packageName
            ))
        }

        // Media Apps (if they don't use MediaSession correctly)
        // This can be expanded to catch Spotify, etc.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Clear state if the notification is dismissed?
    }
}

object ReadingServiceBus {
    private var listener: ((ReadingState?) -> Unit)? = null

    fun setListener(l: ((ReadingState?) -> Unit)?) {
        listener = l
    }

    fun updateReading(state: ReadingState?) {
        listener?.invoke(state)
    }
}
