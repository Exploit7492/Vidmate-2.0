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
import com.cobalt.android.util.WidgetSettings
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
        val settings = WidgetSettings(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetTitle, open)
        views.setOnClickPendingIntent(R.id.widgetAction, open)

        // Advance the spinner frame only while animated and only when there is
        // something to spin for; a static widget should not churn its frame.
        val glyph = if (settings.animated && active.isNotEmpty()) {
            val f = settings.frame
            settings.frame = f + 1
            WidgetPresenter.SPINNER[f] + " "
        } else ""

        val top = active.firstOrNull()
        val pct = if (top != null && top.totalBytes > 0)
            (top.bytesDownloaded * 100 / top.totalBytes).toInt() else 0
        val text = WidgetPresenter.text(
            appName = context.getString(R.string.widget_title),
            count = active.size,
            topName = top?.filename.orEmpty(),
            topPct = pct,
            showCount = settings.showCount,
            showProgress = settings.showProgress,
            glyph = glyph
        )
        views.setTextViewText(R.id.widgetTitle, text.title)
        views.setTextViewText(R.id.widgetBody, text.body)
        views.setViewVisibility(
            R.id.widgetAction, if (settings.compact) android.view.View.GONE else android.view.View.VISIBLE
        )
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
