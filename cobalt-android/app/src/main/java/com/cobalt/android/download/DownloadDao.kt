package com.cobalt.android.download

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DownloadRecord): Long

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET bytesDownloaded = :bytes, totalBytes = :total, status = :status WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, total: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET bytesDownloaded = :bytes, totalBytes = :total, speedBps = :speed, status = :status WHERE id = :id")
    suspend fun updateProgressWithSpeed(id: Long, bytes: Long, total: Long, speed: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("UPDATE downloads SET mediaStoreUriString = :uri WHERE id = :id")
    suspend fun updateMediaStoreUri(id: Long, uri: String)

    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllLive(): LiveData<List<DownloadRecord>>

    // Phase 20: CONVERTING added alongside the existing three so a
    // transcode-output row shows up in the same "active" queue surface
    // (DownloadQueueSheet) a normal in-progress download does, instead of
    // silently disappearing from the queue while ffmpeg runs.
    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING','CONVERTING','FAILED_NETWORK') ORDER BY timestamp DESC")
    fun getActiveLive(): LiveData<List<DownloadRecord>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING','CONVERTING','FAILED_NETWORK') ORDER BY timestamp DESC")
    suspend fun getActiveSync(): List<DownloadRecord>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadRecord?

    @Query("DELETE FROM downloads WHERE status = 'COMPLETE' OR status = 'FAILED' OR status = 'CANCELLED'")
    suspend fun clearHistory()

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETE','FAILED','FAILED_NETWORK','CANCELLED')")
    suspend fun clearCompleted()

    // Phase B: the segmented engine checkpoints its part list (and the ETag it
    // is valid against) periodically, so a resume has exact offsets to work
    // from after a process kill.
    @Query("UPDATE downloads SET segmentsJson = :segments, etag = :etag, rangeSupported = :rangeSupported WHERE id = :id")
    suspend fun updateSegments(id: Long, segments: String, etag: String, rangeSupported: Boolean)

    // Only a row that is mid-transfer can be cancelled; the status guard means
    // a cancel racing a completion cannot flip a finished download to CANCELLED.
    @Query("UPDATE downloads SET status = 'CANCELLED' WHERE id = :id AND status IN ('QUEUED','DOWNLOADING','CONVERTING','FAILED_NETWORK')")
    suspend fun markCancelled(id: Long): Int

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE downloads SET status = 'FAILED_NETWORK' WHERE status = 'DOWNLOADING'")
    suspend fun resetStuckDownloads()
}
