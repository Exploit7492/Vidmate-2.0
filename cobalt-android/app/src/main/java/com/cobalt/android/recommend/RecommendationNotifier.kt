package com.cobalt.android.recommend

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cobalt.android.R

/**
 * The "because you watched X" notification channel.
 *
 * Deliberately a **separate channel** from the download one, so a user can keep
 * download alerts and turn recommendations off (or the reverse) in Android's
 * own settings — a single shared channel would couple the two.
 */
class RecommendationNotifier(private val context: Context) {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_recommendations_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_recommendations_desc)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts one recommendation card. Tapping it opens the app with
     * [openUrl] delivered as a share/text intent, which the existing
     * `MainActivity.handleIntent` path already resolves and offers to download
     * — so a notification tap lands the user straight on the picker.
     */
    fun show(id: Int, title: String, videoUrl: String, thumbnailHint: String? = null) {
        val tapIntent = PendingIntent.getActivity(
            context, id,
            Intent(context, com.cobalt.android.MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, videoUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentTitle(title)
            .setContentText("Recommended for you · tap to open")
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notif)
    }

    fun cancelAll() = manager.cancelAll()

    companion object {
        const val CHANNEL_ID = "cobalt_recommendations"
        const val BASE_ID = 2000
    }
}
