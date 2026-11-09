package com.cobalt.android.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.cobalt.android.db.HistoryRepository
import com.cobalt.android.db.LikedRepository
import com.cobalt.android.db.entities.HistoryItemType
import com.cobalt.android.download.DownloadRepository
import com.cobalt.android.download.DownloadRecord
import com.cobalt.android.download.MediaStoreWriter
import com.cobalt.android.ui.history.HistoryRow
import com.cobalt.android.ui.history.HistoryRowAdapter
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Backs the Library tab — VidMate's "My Files / Downloads" surface. Exposes
 * the download queue (active + completed), plus the merged watch/download
 * history and liked rows, so one screen covers everything the user has
 * saved, downloaded or watched.
 *
 * History/liked rows are mapped to the existing [HistoryRow] display model
 * (the same one Phase 11's `HistoryFragment` uses) rather than a second row
 * type, keeping one representation of "a title + subtitle + source link".
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadRepository = DownloadRepository(application)
    private val historyRepository = HistoryRepository(application)
    private val likedRepository = LikedRepository(application)

    val activeDownloads: LiveData<List<com.cobalt.android.download.DownloadRecord>> =
        downloadRepository.activeDownloads
    val allDownloads: LiveData<List<com.cobalt.android.download.DownloadRecord>> =
        downloadRepository.allDownloads

    val historyRows: LiveData<List<HistoryRow>> = historyRepository.allHistory.map { entities ->
        entities.map { e ->
            HistoryRow(
                id = "h${e.id}",
                title = e.title.ifBlank { "untitled" },
                subtitle = (if (e.itemType == HistoryItemType.DOWNLOAD.name) "downloaded" else "watched") +
                    " · " + relativeTime(e.timestamp),
                sourceUrl = e.sourceUrl
            )
        }
    }

    val likedRows: LiveData<List<HistoryRow>> = likedRepository.allLiked.map { entities ->
        entities.map { e ->
            HistoryRow(
                id = "l${e.videoId}",
                title = e.title.ifBlank { "untitled" },
                subtitle = "${e.authorName} · ${relativeTime(e.likedAt)}",
                sourceUrl = e.watchUrl
            )
        }
    }

    /**
     * Removes a download completely: its MediaStore entry (the actual file on
     * disk), then its DB row. The MediaStore delete is best-effort — a row whose
     * file was already removed by the user (or by a factory reset) must still be
     * deletable from the queue.
     */
    fun delete(record: DownloadRecord) {
        viewModelScope.launch {
            if (record.mediaStoreUriString.isNotBlank()) {
                runCatching {
                    MediaStoreWriter(getApplication()).delete(Uri.parse(record.mediaStoreUriString))
                }
            }
            downloadRepository.deleteById(record.id)
        }
    }

    private fun relativeTime(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${TimeUnit.MINUTES.toHours(minutes)}h ago"
            minutes < 60 * 24 * 7 -> "${TimeUnit.HOURS.toDays(minutes)}d ago"
            else -> HistoryRowAdapter.formatTimestamp(millis)
        }
    }
}
