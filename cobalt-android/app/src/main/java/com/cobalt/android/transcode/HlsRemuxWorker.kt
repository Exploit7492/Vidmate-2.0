package com.cobalt.android.transcode

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cobalt.android.db.HistoryRepository
import com.cobalt.android.db.entities.HistoryEntity
import com.cobalt.android.db.entities.HistoryItemType
import com.cobalt.android.download.DownloadRecord
import com.cobalt.android.download.DownloadRepository
import com.cobalt.android.download.DownloadStatus
import com.cobalt.android.download.MediaStoreWriter
import com.cobalt.android.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Finishes an HLS download: [com.cobalt.android.download.HlsDownloadEngine]
 * leaves a raw concatenated MPEG-TS file in the app's private storage, which
 * no gallery or player indexes properly. This worker stream-copies that file
 * into a real MP4 in MediaStore ([TranscodeProfile.REMUX_MP4] — copy, no
 * re-encode) and then deletes the temp `.ts`.
 *
 * Unlike [TranscodeWorker], this updates the *same* [DownloadRecord] row the
 * download started on rather than inserting a new output row: an HLS job has
 * no user-visible raw artefact worth keeping, so one queue entry that goes
 * DOWNLOADING → CONVERTING → COMPLETE is the honest model. It also reads its
 * input from a plain filesystem path, not a `content://` URI, because the
 * temp file never went through MediaStore — see
 * [FfmpegTranscoder.transcodeFile].
 */
class HlsRemuxWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val sourceRecordId = inputData.getLong(KEY_SOURCE_RECORD_ID, -1L)
        val tempPath = inputData.getString(KEY_TEMP_PATH) ?: return Result.failure()
        val requestedName = inputData.getString(KEY_OUTPUT_NAME).orEmpty()
        val originalUrl = inputData.getString(KEY_ORIGINAL_URL).orEmpty()

        val repository = DownloadRepository(applicationContext)
        val historyRepository = HistoryRepository(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)
        val mediaStoreWriter = MediaStoreWriter(applicationContext)
        val transcoder = FfmpegTranscoder(applicationContext)

        val tempFile = File(tempPath)
        if (!tempFile.exists()) {
            repository.updateStatus(sourceRecordId, DownloadStatus.FAILED)
            notificationHelper.showFailed(sourceRecordId, requestedName)
            return Result.failure()
        }

        val baseName = requestedName.substringBeforeLast('.').ifBlank { "video" }
        val outputFilename = "$baseName.mp4"
        val profile = TranscodeProfile.REMUX_MP4

        val opened = mediaStoreWriter.open(outputFilename, profile.mimeType)
        if (opened == null) {
            repository.updateStatus(sourceRecordId, DownloadStatus.FAILED)
            notificationHelper.showFailed(sourceRecordId, outputFilename)
            tempFile.delete()
            return Result.failure()
        }
        // ffmpeg writes through its own fd via the SAF bridge; close the
        // OutputStream MediaStoreWriter hands back so nothing else holds it.
        opened.stream.close()

        val durationMs = probeDurationMs(tempFile.absolutePath)

        return try {
            val result = transcoder.transcodeFile(
                inputPath = tempFile.absolutePath,
                outputUri = opened.uri,
                profile = profile,
                totalDurationMs = durationMs
            ) { percent ->
                // FFmpegKit's statistics callback thread, not doWork()'s
                // coroutine — same runBlocking rationale as TranscodeWorker.
                kotlinx.coroutines.runBlocking {
                    repository.updateTranscodeProgress(sourceRecordId, percent.toLong())
                }
                notificationHelper.updateProgress(sourceRecordId, percent, 100, label = "Preparing MP4…")
            }

            if (result.success) {
                mediaStoreWriter.finalize(opened.uri)
                repository.updateMediaStoreUri(sourceRecordId, opened.uri.toString())
                repository.updateStatus(sourceRecordId, DownloadStatus.COMPLETE)
                notificationHelper.showComplete(sourceRecordId, outputFilename, opened.uri, profile.mimeType)
                historyRepository.record(
                    HistoryEntity(
                        itemType = HistoryItemType.DOWNLOAD.name,
                        refId = sourceRecordId.toString(),
                        title = outputFilename,
                        thumbnailUrl = "",
                        sourceUrl = originalUrl
                    )
                )
                Result.success()
            } else {
                Log.e(TAG, "Remux failed for row $sourceRecordId: ${result.failureReason}")
                mediaStoreWriter.delete(opened.uri)
                repository.updateStatus(sourceRecordId, DownloadStatus.FAILED)
                notificationHelper.showFailed(sourceRecordId, outputFilename)
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Remux threw for row $sourceRecordId", e)
            mediaStoreWriter.delete(opened.uri)
            repository.updateStatus(sourceRecordId, DownloadStatus.FAILED)
            notificationHelper.showFailed(sourceRecordId, outputFilename)
            Result.failure()
        } finally {
            // The .ts is only ever an intermediate; it is never resumable
            // (the engine restarts the playlist from the top), so keeping it
            // around would just leak storage.
            tempFile.delete()
        }
    }

    private suspend fun probeDurationMs(path: String): Long = withContext(Dispatchers.IO) {
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(path)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        }.getOrDefault(0L)
    }

    private inline fun <T : MediaMetadataRetriever, R> T.use(block: (T) -> R): R =
        try { block(this) } finally { release() }

    companion object {
        private const val TAG = "HlsRemuxWorker"
        private const val KEY_SOURCE_RECORD_ID = "sourceRecordId"
        private const val KEY_TEMP_PATH = "tempPath"
        private const val KEY_OUTPUT_NAME = "outputName"
        private const val KEY_ORIGINAL_URL = "originalUrl"

        fun enqueue(
            ctx: Context,
            sourceRecordId: Long,
            tempPath: String,
            outputName: String,
            originalUrl: String
        ) {
            val data = workDataOf(
                KEY_SOURCE_RECORD_ID to sourceRecordId,
                KEY_TEMP_PATH to tempPath,
                KEY_OUTPUT_NAME to outputName,
                KEY_ORIGINAL_URL to originalUrl
            )
            val request = OneTimeWorkRequestBuilder<HlsRemuxWorker>()
                .setInputData(data)
                .addTag("hls_remux_$sourceRecordId")
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "hls_remux_$sourceRecordId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
