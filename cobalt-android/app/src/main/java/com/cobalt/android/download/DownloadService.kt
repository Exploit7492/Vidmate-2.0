package com.cobalt.android.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.cobalt.android.db.HistoryRepository
import com.cobalt.android.db.entities.HistoryEntity
import com.cobalt.android.db.entities.HistoryItemType
import com.cobalt.android.transcode.TranscodeProfile
import com.cobalt.android.transcode.TranscodeWorker
import com.cobalt.android.util.NotificationHelper
import com.cobalt.android.util.SettingsRepository
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DownloadService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repository: DownloadRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var mediaStoreWriter: MediaStoreWriter

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // Phase B: the segmented/resumable engine. A single OkHttpClient is shared
    // so its connection pool is reused across the parallel part requests.
    private val engine by lazy { SegmentedDownloadEngine(okHttpClient) }

    override fun onCreate() {
        super.onCreate()
        repository = DownloadRepository(this)
        historyRepository = HistoryRepository(this)
        notificationHelper = NotificationHelper(this)
        mediaStoreWriter = MediaStoreWriter(this)
        // Reset any downloads stuck in DOWNLOADING from a previous process kill
        scope.launch { repository.resetStuckDownloads() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground() on main thread before any coroutines
        startForeground()

        when (intent?.action) {
            ACTION_HTTPS -> {
                val record = DownloadRecord(
                    originalUrl = intent.getStringExtra(EXTRA_ORIGINAL_URL) ?: "",
                    cobaltUrl = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY,
                    filename = intent.getStringExtra(EXTRA_FILENAME) ?: "download",
                    mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "application/octet-stream",
                    cookies = intent.getStringExtra(EXTRA_COOKIES) ?: "",
                    userAgent = intent.getStringExtra(EXTRA_USER_AGENT) ?: ""
                )
                // Phase 20: not persisted on `record` itself — a transcode
                // target describes what should happen *after* this raw
                // download finishes, not a property of the raw file being
                // fetched, so it only lives in this closure (and then on
                // the separate output row TranscodeWorker inserts).
                val transcodeProfile = intent.getStringExtra(EXTRA_TRANSCODE_PROFILE)
                    ?.let { TranscodeProfile.decode(it) }
                scope.launch {
                    val id = repository.insert(record)
                    processHttps(record.copy(id = id), transcodeProfile)
                }
            }
            ACTION_BLOB -> {
                val tempPath = intent.getStringExtra(EXTRA_TEMP_PATH) ?: return START_NOT_STICKY
                val record = DownloadRecord(
                    originalUrl = intent.getStringExtra(EXTRA_ORIGINAL_URL) ?: "",
                    filename = intent.getStringExtra(EXTRA_FILENAME) ?: "download",
                    mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "application/octet-stream",
                    isBlobDownload = true,
                    tempFilePath = tempPath
                )
                scope.launch {
                    val id = repository.insert(record)
                    processBlob(record.copy(id = id))
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun processHttps(record: DownloadRecord, transcodeProfile: TranscodeProfile? = null) {
        active.incrementAndGet()
        // Register this transfer so a cancel tap can reach it (see
        // CancellationRegistry / DownloadCanceller).
        val selfJob = coroutineContext[Job]
        if (selfJob != null) CancellationRegistry.register(record.id, selfJob)

        val headers = buildMap {
            if (record.cookies.isNotBlank()) put("Cookie", record.cookies)
            if (record.userAgent.isNotBlank()) put("User-Agent", record.userAgent)
        }
        // The partial file lives in filesDir (not cacheDir) so it survives long
        // enough to resume, and its name is derived from the row id so no extra
        // column is needed to find it again.
        val partialDir = File(filesDir, "partial").apply { mkdirs() }
        val tempFile = File(partialDir, "download_${record.id}.part")

        try {
            repository.updateStatus(record.id, DownloadStatus.DOWNLOADING)

            // D2: magnet links and .torrent files go through libtorrent, not
            // the HTTP engine.
            if (TorrentEngine.isTorrent(record.cobaltUrl)) {
                downloadTorrent(record)
                return
            }

            val probe = engine.probe(record.cobaltUrl, headers)
            if (probe.totalBytes <= 0) throw IOException("Unknown content length")

            // SABR streams must never be range-split (the byte windows aren't
            // independent), so they always take the single-stream path.
            if (probe.rangeSupported && !SabrDetector.isSabr(record.cobaltUrl, record.mimeType)) {
                val existing = SegmentList.decode(record.segmentsJson)
                val outcome = engine.download(
                    tempFile = tempFile,
                    headers = headers,
                    existingSegments = existing,
                    existingEtag = record.etag,
                    probe = probe,
                    maxParts = SettingsRepository(this@DownloadService).parallelSegments,
                    onSegments = { segs, etag ->
                        repository.updateSegments(record.id, SegmentList.encode(segs), etag, true)
                    },
                    onProgress = { p ->
                        repository.updateProgressWithSpeed(record.id, p.downloaded, p.total, p.bytesPerSecond)
                        notificationHelper.updateProgress(record.id, p.downloaded.toInt(), p.total.toInt())
                    }
                )
                if (outcome is SegmentedDownloadEngine.Outcome.Failed) {
                    throw IOException(outcome.message)
                }
            } else {
                // Origin ignores Range — single streaming GET, not resumable.
                engine.downloadSingleStream(tempFile, probe.finalUrl, headers) { p ->
                    repository.updateProgressWithSpeed(record.id, p.downloaded, p.total, p.bytesPerSecond)
                    notificationHelper.updateProgress(record.id, p.downloaded.toInt(), p.total.toInt())
                }
            }

            // Move the finished temp file into MediaStore (SAF), the same
            // finalize path the blob and single-stream downloads use.
            val opened = mediaStoreWriter.open(record.filename, record.mimeType)
                ?: throw IOException("MediaStore open failed")
            try {
                tempFile.inputStream().use { input ->
                    opened.stream.use { out -> input.copyTo(out) }
                }
                mediaStoreWriter.finalize(opened.uri)
            } catch (e: Exception) {
                mediaStoreWriter.delete(opened.uri)
                throw e
            }
            tempFile.delete()
            repository.updateMediaStoreUri(record.id, opened.uri.toString())
            repository.updateStatus(record.id, DownloadStatus.COMPLETE)
            // Clear resume state — the file is complete, nothing to resume.
            repository.updateSegments(record.id, "", "", false)
            recordDownloadHistory(record)
            notificationHelper.showComplete(record.id, record.filename, opened.uri, record.mimeType)
            if (transcodeProfile != null) {
                TranscodeWorker.enqueue(
                    applicationContext, sourceRecordId = record.id,
                    sourceUri = opened.uri, profile = transcodeProfile
                )
            }
        } catch (e: CancellationException) {
            // The user cancelled. Mark the row and clear its notification; the
            // partial file + persisted segments are left in place so the user
            // can resume by re-downloading the same URL. NonCancellable because
            // the coroutine is already cancelled and a suspend DB write would
            // otherwise abort immediately.
            withContext(NonCancellable) { repository.markCancelled(record.id) }
            notificationHelper.cancel(record.id)
            throw e
        } catch (e: UnknownHostException) {
            handleNetworkFail(record)
        } catch (e: IOException) {
            if (e.message?.contains("ENOSPC") == true) {
                notificationHelper.showStorageFull()
                repository.updateStatus(record.id, DownloadStatus.FAILED)
            } else {
                handleNetworkFail(record)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            repository.updateStatus(record.id, DownloadStatus.FAILED)
            notificationHelper.showFailed(record.id, record.filename)
        } finally {
            CancellationRegistry.unregister(record.id)
            if (active.decrementAndGet() == 0) stopSelf()
        }
    }

    /**
     * D2: download a magnet/.torrent via [TorrentEngine]. The finished file(s)
     * are copied into MediaStore the same way HTTP downloads are, so a torrent
     * shows up in the Library like any other download.
     */
    private suspend fun downloadTorrent(record: DownloadRecord) {
        val torrentDir = File(filesDir, "torrents").apply { mkdirs() }
        val torrent = TorrentEngine(torrentDir)
        val outcome = torrent.download(record.cobaltUrl) { p ->
            repository.updateProgressWithSpeed(record.id, p.downloaded, p.total, p.rateBps)
            notificationHelper.updateProgress(record.id, p.downloaded.toInt(), p.total.toInt(), p.name)
        }
        when (outcome) {
            is TorrentEngine.Outcome.Completed -> {
                var firstUri: android.net.Uri? = null
                outcome.files.filter { it.exists() }.forEach { file ->
                    val mime = guessMime(file.name)
                    val opened = mediaStoreWriter.open(file.name, mime) ?: return@forEach
                    file.inputStream().use { input -> opened.stream.use { input.copyTo(it) } }
                    mediaStoreWriter.finalize(opened.uri)
                    if (firstUri == null) firstUri = opened.uri
                }
                repository.updateStatus(record.id, DownloadStatus.COMPLETE)
                notificationHelper.showComplete(
                    record.id, outcome.name.ifBlank { record.filename },
                    firstUri ?: android.net.Uri.EMPTY, guessMime(outcome.name)
                )
            }
            is TorrentEngine.Outcome.Failed -> {
                throw IOException(outcome.message)
            }
        }
    }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4" -> "video/mp4"; "mkv" -> "video/x-matroska"; "webm" -> "video/webm"
        "mp3" -> "audio/mpeg"; "m4a" -> "audio/mp4"; "flac" -> "audio/flac"
        "avi" -> "video/x-msvideo"; "mov" -> "video/quicktime"
        else -> "application/octet-stream"
    }

    private suspend fun processBlob(record: DownloadRecord) {
        active.incrementAndGet()
        val tempFile = File(record.tempFilePath)
        try {
            repository.updateStatus(record.id, DownloadStatus.DOWNLOADING)
            if (!tempFile.exists()) throw IOException("Temp file missing: ${record.tempFilePath}")

            val opened = mediaStoreWriter.open(record.filename, record.mimeType)
                ?: throw IOException("MediaStore open failed")
            try {
                opened.stream.use { out ->
                    tempFile.inputStream().use { input -> input.copyTo(out) }
                }
                mediaStoreWriter.finalize(opened.uri)
                repository.updateMediaStoreUri(record.id, opened.uri.toString())
                repository.updateStatus(record.id, DownloadStatus.COMPLETE)
                recordDownloadHistory(record)
                notificationHelper.showComplete(record.id, record.filename, opened.uri, record.mimeType)
            } catch (e: Exception) {
                mediaStoreWriter.delete(opened.uri)
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Blob download failed", e)
            repository.updateStatus(record.id, DownloadStatus.FAILED)
            notificationHelper.showFailed(record.id, record.filename)
        } finally {
            tempFile.delete()   // always clean up temp file
            if (active.decrementAndGet() == 0) stopSelf()
        }
    }

    // Phase 10: shared by both the HTTPS and blob complete paths so history
    // covers every way a download reaches Phase 6/7's DownloadService.
    private suspend fun recordDownloadHistory(record: DownloadRecord) {
        historyRepository.record(
            HistoryEntity(
                itemType = HistoryItemType.DOWNLOAD.name,
                refId = record.id.toString(),
                title = record.filename,
                thumbnailUrl = "",
                sourceUrl = record.originalUrl
            )
        )
    }

    private suspend fun handleNetworkFail(record: DownloadRecord) {
        repository.updateStatus(record.id, DownloadStatus.FAILED_NETWORK)
        val current = repository.getById(record.id) ?: return
        if (current.retryCount < 3) {
            repository.incrementRetry(record.id)
            RetryDownloadWorker.schedule(this, record.id, record.originalUrl, record.filename)
        } else {
            notificationHelper.showFailed(record.id, record.filename)
        }
    }

    private fun startForeground() {
        val notif = notificationHelper.buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.FOREGROUND_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.FOREGROUND_ID, notif)
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "DownloadService"

        /** Live transfer count, shared so the AIDL face can report it. */
        private val active = AtomicInteger(0)

        /** Number of transfers currently running (D5 — AIDL `activeCount()`). */
        fun activeCount(): Int = active.get()

        const val ACTION_HTTPS = "com.cobalt.android.HTTPS"
        const val ACTION_BLOB = "com.cobalt.android.BLOB"
        const val EXTRA_URL = "url"
        const val EXTRA_ORIGINAL_URL = "originalUrl"
        const val EXTRA_FILENAME = "filename"
        const val EXTRA_MIME_TYPE = "mimeType"
        const val EXTRA_COOKIES = "cookies"
        const val EXTRA_USER_AGENT = "userAgent"
        const val EXTRA_TEMP_PATH = "tempPath"
        // Phase 20: TranscodeProfile.encode()'d string, or absent entirely
        // for "download as-is, no conversion" — the overload below without
        // a `transcodeProfile` parameter is what every pre-Phase-15 call
        // site keeps using unchanged.
        const val EXTRA_TRANSCODE_PROFILE = "transcodeProfile"

        // Parallel part count. Four is a good default: enough connections to
        // saturate most links, few enough to stay polite to the origin and to
        // keep the socket count low on mobile.
        private const val MAX_PARTS = 4

        fun startHttps(
            ctx: Context, cobaltUrl: String, filename: String,
            mimeType: String, cookies: String, userAgent: String, originalUrl: String,
            transcodeProfile: TranscodeProfile? = null
        ) {
            ctx.startForegroundService(Intent(ctx, DownloadService::class.java).apply {
                action = ACTION_HTTPS
                putExtra(EXTRA_URL, cobaltUrl)
                putExtra(EXTRA_ORIGINAL_URL, originalUrl)
                putExtra(EXTRA_FILENAME, filename)
                putExtra(EXTRA_MIME_TYPE, mimeType)
                putExtra(EXTRA_COOKIES, cookies)
                putExtra(EXTRA_USER_AGENT, userAgent)
                transcodeProfile?.let { putExtra(EXTRA_TRANSCODE_PROFILE, it.encode()) }
            })
        }

        fun startBlob(
            ctx: Context, tempPath: String, filename: String,
            mimeType: String, originalUrl: String
        ) {
            ctx.startForegroundService(Intent(ctx, DownloadService::class.java).apply {
                action = ACTION_BLOB
                putExtra(EXTRA_TEMP_PATH, tempPath)
                putExtra(EXTRA_ORIGINAL_URL, originalUrl)
                putExtra(EXTRA_FILENAME, filename)
                putExtra(EXTRA_MIME_TYPE, mimeType)
            })
        }
    }
}
