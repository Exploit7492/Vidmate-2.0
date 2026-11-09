package com.cobalt.android.download

import android.content.Context
import androidx.work.WorkManager
import com.cobalt.android.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * One entry point for "cancel this download", covering all three ways a row can
 * be in flight:
 *
 *  1. an HTTPS transfer running in `DownloadService` — cancelled through
 *     [CancellationRegistry] (which the Service registered its job with);
 *  2. a queued network retry (`RetryDownloadWorker`, a WorkManager job);
 *  3. a running transcode (`TranscodeWorker`, also WorkManager) — the worker's
 *     coroutine cancellation reaches `FfmpegTranscoder`, which calls
 *     `FFmpegKit.cancel()` on the session.
 *
 * The row is marked CANCELLED first (so the UI flips immediately and a cancel
 * racing a completion can't resurrect a finished row — see
 * `DownloadDao.markCancelled`'s status guard), then the notification is
 * cleared, then every engine is told to stop.
 */
object DownloadCanceller {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun cancel(context: Context, recordId: Long) {
        val repository = DownloadRepository(context)
        val notificationHelper = NotificationHelper(context)
        val workManager = WorkManager.getInstance(context)

        scope.launch {
            repository.markCancelled(recordId)
            notificationHelper.cancel(recordId)
            CancellationRegistry.cancel(recordId)
            workManager.cancelUniqueWork("retry_$recordId")
            workManager.cancelUniqueWork("transcode_$recordId")
            // The partial file is intentionally left in place: the persisted
            // segment list + ETag still describe it, so re-downloading the same
            // URL resumes from where the cancel happened.
        }
    }
}
