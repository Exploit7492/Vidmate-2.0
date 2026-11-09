package com.cobalt.android.download

import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps a download row id to the [Job] running its transfer, so a cancel tap in
 * the queue UI can abort the in-flight coroutine in `DownloadService` directly.
 *
 * This is a process-wide singleton because the queue UI (an Activity) and the
 * transfer (a foreground Service) are different components; the Service
 * registers its job on start and unregisters it in `finally`, and the UI calls
 * [cancel] which completes the job with a CancellationException. That
 * cancellation propagates into the segmented engine, which cancels its OkHttp
 * calls and persists the resume state before unwinding.
 */
object CancellationRegistry {
    private val jobs = ConcurrentHashMap<Long, Job>()

    fun register(recordId: Long, job: Job) {
        jobs[recordId] = job
    }

    fun unregister(recordId: Long) {
        jobs.remove(recordId)
    }

    /** Returns true if a running transfer was found and cancelled. */
    fun cancel(recordId: Long): Boolean {
        val job = jobs.remove(recordId) ?: return false
        job.cancel()
        return true
    }

    fun isActive(recordId: Long): Boolean = jobs.containsKey(recordId)
}
