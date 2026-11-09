package com.cobalt.android.download

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadRepository(context: Context) {
    private val dao = DownloadDatabase.getInstance(context).downloadDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    val allDownloads: LiveData<List<DownloadRecord>> = dao.getAllLive()
    val activeDownloads: LiveData<List<DownloadRecord>> = dao.getActiveLive()

    suspend fun insert(record: DownloadRecord): Long = dao.insert(record)

    fun updateStatusAsync(id: Long, status: DownloadStatus) =
        scope.launch { dao.updateStatus(id, status) }

    suspend fun updateStatus(id: Long, status: DownloadStatus) =
        dao.updateStatus(id, status)

    suspend fun updateProgress(id: Long, bytes: Long, total: Long) =
        dao.updateProgress(id, bytes, total, DownloadStatus.DOWNLOADING)

    /** Progress tick that also records the measured speed for the queue UI. */
    suspend fun updateProgressWithSpeed(id: Long, bytes: Long, total: Long, speed: Long) =
        dao.updateProgressWithSpeed(id, bytes, total, speed, DownloadStatus.DOWNLOADING)

    // Phase 20: same shape as updateProgress above (reuses bytes/total as
    // a 0..100 percent — see TranscodeWorker's KDoc), but keeps the row in
    // CONVERTING rather than clobbering it back to DOWNLOADING, which is
    // what calling the plain updateProgress() above would silently do.
    suspend fun updateTranscodeProgress(id: Long, percent: Long) =
        dao.updateProgress(id, percent, 100L, DownloadStatus.CONVERTING)

    suspend fun updateMediaStoreUri(id: Long, uri: String) =
        dao.updateMediaStoreUri(id, uri)

    suspend fun incrementRetry(id: Long) = dao.incrementRetry(id)

    suspend fun getById(id: Long): DownloadRecord? = dao.getById(id)

    suspend fun clearHistory() = dao.clearHistory()

    /** Removes finished rows (complete/failed/cancelled) from history. */
    suspend fun clearCompleted() = dao.clearCompleted()

    suspend fun resetStuckDownloads() {
        dao.resetStuckDownloads()
    }

    /** Persists the segmented engine's part list + the ETag it is valid against. */
    suspend fun updateSegments(id: Long, segmentsJson: String, etag: String, rangeSupported: Boolean) =
        dao.updateSegments(id, segmentsJson, etag, rangeSupported)

    /** Returns true if a mid-transfer row was actually flipped to CANCELLED. */
    suspend fun markCancelled(id: Long): Boolean = dao.markCancelled(id) > 0

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
