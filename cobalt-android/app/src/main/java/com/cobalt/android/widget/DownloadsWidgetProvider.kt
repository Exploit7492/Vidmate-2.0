package com.cobalt.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cobalt.android.MainActivity
import com.cobalt.android.R
import com.cobalt.android.download.DownloadDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home-screen widget showing active downloads (Phase H7).
 *
 * Reads the download DB directly (Room is already the source of truth) on the
 * background dispatcher, then paints a small summary: a count and the
 * most-recent active item, or "no active downloads". Tapping it opens the app.
 *
 * Refresh is triggered by [notifyChanged] from the app whenever a download's
 * state changes, plus the provider's own periodic `updatePeriodMillis`. Doing
 * the DB read off the main thread matters here: `onUpdate` runs on the main
 * thread and a synchronous Room query would be an ANR waiting to happen.
 */
class DownloadsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context, manager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = DownloadDatabase.getInstance(context).downloadDao()
                val active = dao.getActiveSync()
                for (id in appWidgetIds) {
                    manager.updateAppWidget(id, buildViews(context, active))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(
        context: Context, active: List<com.cobalt.android.download.DownloadRecord>
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_downloads)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetTitle, open)
        views.setOnClickPendingIntent(R.id.widgetAction, open)

        if (active.isEmpty()) {
            views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_title))
            views.setTextViewText(R.id.widgetBody, "No active downloads")
        } else {
            views.setTextViewText(R.id.widgetTitle, "${active.size} downloading")
            val top = active.first()
            val pct = if (top.totalBytes > 0)
                (top.bytesDownloaded * 100 / top.totalBytes).toInt() else 0
            views.setTextViewText(
                R.id.widgetBody,
                "${top.filename.ifBlank { "download" }}\n$pct%"
            )
        }
        return views
    }

    companion object {
        /** Called by the app after a download state change. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DownloadsWidgetProvider::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, DownloadsWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            )
        }
    }
}
