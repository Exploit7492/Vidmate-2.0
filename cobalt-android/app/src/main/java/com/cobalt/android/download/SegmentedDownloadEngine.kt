package com.cobalt.android.download

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong

/**
 * Segmented, resumable, per-part-failover download engine.
 *
 * This is the piece that brings the app up to the capability of VidMate's
 * download core (see the RE report §4.1/§5.3): a transfer is a list of byte
 * ranges ([Segment]) fetched in parallel, each with its own URL and its own
 * resume offset, all written into one seekable temp file at their absolute
 * offsets. It is *not* a port of VidMate's code — that is not available — but
 * it implements the same design, which is the argument for the capability
 * being real rather than nominal:
 *
 *  - **Multi-segment / range** ([SegmentList.split]) — split the file into N
 *    ranges and fetch them concurrently, so throughput is not bounded by one
 *    TCP connection's share of the link.
 *  - **Per-part URL/path fail-over** — a part that keeps failing retries
 *    against its [Segment.alternateUrl] before the whole download is failed,
 *    so one bad CDN edge does not sink the transfer.
 *  - **Resume** — [Segment.written] is persisted on every checkpoint and the
 *    temp file is written in place (never truncated mid-transfer), so a
 *    process kill or a cancel resumes by re-requesting only the missing bytes.
 *    A resume is refused if the origin's ETag changed, so two versions of a
 *    file are never spliced together.
 *  - **Cancellation** — the engine runs inside the caller's coroutine scope,
 *    so cancelling that scope aborts every in-flight request; the part list is
 *    persisted from a `NonCancellable` block first so the row is still
 *    resumable afterwards.
 *
 * The caller owns the temp file's lifetime and moves the finished file into
 * MediaStore; the engine only ever writes to the temp file.
 */
class SegmentedDownloadEngine(private val client: OkHttpClient) {

    data class Probe(
        val totalBytes: Long,
        val rangeSupported: Boolean,
        val etag: String,
        val finalUrl: String
    )

    data class Progress(val downloaded: Long, val total: Long, val bytesPerSecond: Long)

    sealed interface Outcome {
        /** Every part was written to [tempFile]. */
        data class Completed(val tempFile: File, val totalBytes: Long) : Outcome
        /** The transfer failed after exhausting retries/fail-over. */
        data class Failed(val message: String, val networkError: Boolean) : Outcome
    }

    /**
     * Cheap capability probe: asks for the first byte only. A `206` with a
     * `Content-Range` means the origin supports ranges and we learned the
     * total size and ETag from it; a `200` means no range support (the body is
     * the whole file, which we discard here and let the caller re-request as a
     * single stream).
     */
    suspend fun probe(url: String, headers: Map<String, String>): Probe = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .header("Range", "bytes=0-0")
            .build()
        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val etag = response.header("ETag").orEmpty()
            if (response.code == 206) {
                val contentRange = response.header("Content-Range").orEmpty()
                // "bytes 0-0/12345"
                val total = contentRange.substringAfterLast('/', "-1").trim().toLongOrNull() ?: -1L
                Probe(total, total > 0, etag, finalUrl)
            } else {
                val len = response.header("Content-Length")?.toLongOrNull() ?: -1L
                Probe(len, false, etag, finalUrl)
            }
        }
    }

    /**
     * Runs the segmented transfer. [existingSegments] (from a prior attempt's
     * persisted state) drive resume; pass an empty list to start fresh. The
     * part list is handed back through [onSegments] whenever it changes so the
     * caller can persist it on the row.
     */
    suspend fun download(
        tempFile: File,
        headers: Map<String, String>,
        existingSegments: List<Segment>,
        existingEtag: String,
        probe: Probe,
        maxParts: Int,
        onSegments: suspend (List<Segment>, String) -> Unit,
        onProgress: suspend (Progress) -> Unit
    ): Outcome = coroutineScope {
        tempFile.parentFile?.mkdirs()

        // Resume is only valid if the origin's ETag still matches what we
        // recorded. Otherwise the file changed under us and the offsets mean
        // nothing — start over.
        val canResume = existingSegments.isNotEmpty() &&
            existingEtag.isNotBlank() &&
            existingEtag == probe.etag &&
            existingSegments.lastOrNull()?.end == probe.totalBytes - 1

        val segments: MutableList<Segment> = if (canResume) {
            Log.i(TAG, "resuming ${existingSegments.size} parts, etag=$existingEtag")
            existingSegments.toMutableList()
        } else {
            if (tempFile.exists()) tempFile.delete()
            SegmentList.split(probe.totalBytes, maxParts)
                .map { it.copy(url = probe.finalUrl) }
                .toMutableList()
        }

        // Persist the part list up front: if the process dies during the very
        // first seconds of the transfer, the row still knows the split and the
        // ETag, so the next attempt resumes rather than restarts.
        onSegments(snapshot(segments), probe.etag)

        val total = probe.totalBytes
        val downloaded = AtomicLong(SegmentList.totalWritten(segments))
        val semaphore = Semaphore(maxParts.coerceAtLeast(1))

        // Rolling speed window, updated by the shared progress emitter.
        var windowBytes = downloaded.get()
        var windowStart = System.currentTimeMillis()

        suspend fun emit() {
            val now = System.currentTimeMillis()
            val elapsed = now - windowStart
            if (elapsed >= 400) {
                val bytes = downloaded.get()
                val speed = ((bytes - windowBytes) * 1000L / elapsed).coerceAtLeast(0L)
                windowBytes = bytes
                windowStart = now
                onProgress(Progress(bytes, total, speed))
            }
        }

        try {
            val tasks = segments.mapIndexed { index, seg ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        runPart(
                            tempFile = tempFile,
                            segmentIndex = index,
                            segment = seg,
                            headers = headers,
                            downloaded = downloaded,
                            onWritten = { updated ->
                                synchronized(segments) { segments[index] = updated }
                                emit()
                            },
                            onCheckpoint = { updated ->
                                synchronized(segments) { segments[index] = updated }
                                onSegments(snapshot(segments), probe.etag)
                            }
                        )
                    }
                }
            }
            tasks.awaitAll()
            onProgress(Progress(downloaded.get(), total, 0L))
            Outcome.Completed(tempFile, total)
        } catch (e: CancellationException) {
            // Persist the exact resume state before unwinding — this block must
            // not itself be cancelled, or the row would lose the offsets.
            withContext(NonCancellable) {
                onSegments(snapshot(segments), probe.etag)
            }
            throw e
        } catch (e: PartFailedException) {
            withContext(NonCancellable) {
                onSegments(snapshot(segments), probe.etag)
            }
            Outcome.Failed(e.message ?: "download failed", e.networkError)
        }
    }

    /**
     * Fetches one part, resuming from [Segment.written] and writing at the
     * part's absolute offset. Retries transient failures against the primary
     * URL, then against [Segment.alternateUrl] if one is set; only after both
     * are exhausted does it throw, which fails the whole download.
     */
    private suspend fun runPart(
        tempFile: File,
        segmentIndex: Int,
        segment: Segment,
        headers: Map<String, String>,
        downloaded: AtomicLong,
        onWritten: suspend (Segment) -> Unit,
        onCheckpoint: suspend (Segment) -> Unit
    ) {
        var current = segment
        if (current.isComplete) return

        val urls = listOfNotNull(
            current.url.takeIf { it.isNotBlank() },
            current.alternateUrl.takeIf { it.isNotBlank() }
        ).distinct()
        if (urls.isEmpty()) throw PartFailedException("part $segmentIndex has no URL", networkError = false)

        var lastError: IOException? = null
        for (urlIndex in urls.indices) {
            val attemptUrl = urls[urlIndex]
            // Per-host retries with a small linear backoff.
            for (attempt in 0 until MAX_ATTEMPTS_PER_URL) {
                if (attempt > 0) kotlinx.coroutines.delay(attempt * 500L)
                try {
                    current = fetchPart(tempFile, current, attemptUrl, headers, downloaded, onWritten, onCheckpoint)
                    return
                } catch (e: IOException) {
                    currentCoroutineContext().ensureActive()
                    lastError = e
                    Log.w(TAG, "part $segmentIndex attempt $attempt on $attemptUrl failed: ${e.message}")
                }
            }
        }
        throw PartFailedException(
            "part $segmentIndex failed on all URLs: ${lastError?.message}",
            networkError = lastError != null
        )
    }

    private suspend fun fetchPart(
        tempFile: File,
        segment: Segment,
        url: String,
        headers: Map<String, String>,
        downloaded: AtomicLong,
        onWritten: suspend (Segment) -> Unit,
        onCheckpoint: suspend (Segment) -> Unit
    ): Segment = withContext(Dispatchers.IO) {
        val rangeStart = segment.start + segment.written
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .header("Range", "bytes=$rangeStart-${segment.end}")
            .build()

        var written = segment.written
        // Cancelling the scope must unblock the (blocking) socket read, not
        // wait out the read timeout. Completing the job with a
        // CancellationException cancels the call, which surfaces as an
        // IOException; the ensureActive() checks below then re-raise the
        // cancellation instead of letting it be treated as a retryable error.
        val call = client.newCall(request)
        val registration = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }
        try {
        call.execute().use { response ->
            // 206 is the expected range response. A 200 means the server
            // ignored the Range header and is about to send the whole file
            // from byte 0 — accepting that would corrupt the part, so treat
            // it as a failure for this URL (the caller may fail over).
            if (response.code != 206) {
                throw IOException("expected 206 for range, got ${response.code}")
            }
            val body = response.body ?: throw IOException("empty body")

            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.seek(rangeStart)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var sinceCheckpoint = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n == -1) break
                        raf.write(buffer, 0, n)
                        written += n
                        downloaded.addAndGet(n.toLong())
                        sinceCheckpoint += n
                        val updated = segment.copy(written = written)
                        onWritten(updated)
                        if (sinceCheckpoint >= CHECKPOINT_BYTES) {
                            sinceCheckpoint = 0
                            onCheckpoint(updated)
                        }
                    }
                }
            }
        }
        } finally {
            registration?.dispose()
        }
        val final = segment.copy(written = written)
        onCheckpoint(final)
        if (!final.isComplete) {
            throw IOException("short read: got ${final.written} of ${final.end - final.start + 1}")
        }
        final
    }

    /**
     * Single-stream fallback for origins that do not honour `Range` (probe
     * returned a `200`). Streams the whole body into [tempFile] with the same
     * cancellation behaviour as the segmented path. Returns the byte count.
     * Not resumable — the caller leaves the row's `rangeSupported` false.
     */
    suspend fun downloadSingleStream(
        tempFile: File,
        url: String,
        headers: Map<String, String>,
        onProgress: suspend (Progress) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            .build()
        val call = client.newCall(request)
        val registration = currentCoroutineContext()[Job]?.invokeOnCompletion { cause ->
            if (cause is CancellationException) call.cancel()
        }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("empty body")
                val total = body.contentLength()
                tempFile.parentFile?.mkdirs()
                var written = 0L
                var windowStart = System.currentTimeMillis()
                var windowBytes = 0L
                FileOutputStream(tempFile).use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n == -1) break
                            out.write(buffer, 0, n)
                            written += n
                            val now = System.currentTimeMillis()
                            if (now - windowStart >= 400) {
                                val speed = ((written - windowBytes) * 1000L) / (now - windowStart)
                                windowBytes = written
                                windowStart = now
                                onProgress(Progress(written, total, speed))
                            }
                        }
                    }
                }
                onProgress(Progress(written, total, 0L))
                written
            }
        } finally {
            registration?.dispose()
        }
    }

    /** A consistent copy of the part list, taken under the list's own lock. */
    private fun snapshot(segments: List<Segment>): List<Segment> =
        synchronized(segments) { segments.toList() }

    private class PartFailedException(message: String, val networkError: Boolean) : IOException(message)

    companion object {
        private const val TAG = "SegmentedDownload"
        private const val MAX_ATTEMPTS_PER_URL = 3
        private const val CHECKPOINT_BYTES = 4L * 1024 * 1024
    }
}
