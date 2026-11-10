package com.cobalt.android.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.cobalt.android.R

class NotificationHelper(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Read live (not cached) so a Settings change takes effect on the next
    // notification without restarting anything.
    private val settings get() = SettingsRepository(context)

    /** Completion notifications respect the "notify when a download finishes"
     *  toggle; progress/foreground notifications respect the master switch. */
    private fun completeAllowed() = settings.notificationsEnabled && settings.notifyOnComplete
    private fun anyAllowed() = settings.notificationsEnabled

    companion object {
        const val CHANNEL_ID = "cobalt_downloads"
        const val FOREGROUND_ID = 1
        private const val BASE_ID = 1000
    }

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_downloads_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_downloads_desc)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildForegroundNotification(): Notification {
        // The "keep-alive notice" setting controls whether the always-on
        // foreground notice is shown; the service still runs either way.
        val title = if (settings.keepAliveNotice) context.getString(R.string.app_name) else ""
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("Downloading…")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    fun updateProgress(recordId: Long, bytes: Int, total: Int, label: String = "Downloading…") {
        if (!anyAllowed()) return
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(label)
            .setProgress(total.coerceAtLeast(1), bytes, total <= 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
        manager.notify((BASE_ID + recordId).toInt(), notif)
    }

    fun showComplete(recordId: Long, filename: String, uri: Uri, mimeType: String = "video/*") {
        if (!completeAllowed()) return
        val openIntent = PendingIntent.getActivity(
            context, recordId.toInt(),
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(filename)
            .setContentText("Saved to Downloads/Cobalt")
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_open), openIntent)
            .build()
        manager.notify((BASE_ID + recordId).toInt(), notif)
    }

    fun showFailed(recordId: Long, filename: String) {
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(filename)
            .setContentText("Download failed")
            .setAutoCancel(true)
            .build()
        manager.notify((BASE_ID + recordId).toInt(), notif)
    }

    fun showStorageFull() {
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.not_enough_storage))
            .setOngoing(true)
            .build()
        manager.notify(BASE_ID - 1, notif)
    }

    fun showRetryReady(recordId: Long, originalUrl: String, filename: String) {
        if (!anyAllowed()) return
        val tapIntent = PendingIntent.getActivity(
            context, recordId.toInt(),
            Intent(context, com.cobalt.android.MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, originalUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.retry_notification_title))
            .setContentText(filename.ifBlank { originalUrl })
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
        manager.notify((BASE_ID + recordId).toInt(), notif)
    }

    fun cancel(recordId: Long) = manager.cancel((BASE_ID + recordId).toInt())
}
