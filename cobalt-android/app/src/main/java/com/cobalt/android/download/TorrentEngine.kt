package com.cobalt.android.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.libtorrent4j.SessionManager
import org.libtorrent4j.TorrentInfo
import java.io.File

/**
 * BitTorrent / magnet downloads (D2). The reference app ships a full
 * libtorrent core (`download.bt.core.*`); this is the same capability via
 * [libtorrent4j], the maintained JNI binding to libtorrent — the native
 * `libtorrent4j.so` is packaged per-ABI (arm64-v8a, armeabi-v7a, x86, x86_64).
 *
 * Kept deliberately small and self-contained: start a session, resolve a
 * magnet to a [TorrentInfo] (DHT/LSD via the standard bootstrap routers), add
 * the torrent to a save directory, then poll its status for progress and
 * completion. Progress is surfaced through [onProgress] so the download queue
 * can show a torrent row exactly like an HTTP row.
 */
class TorrentEngine(private val saveDir: File) {
    data class TorrentProgress(val name: String, val downloaded: Long, val total: Long, val rateBps: Long)

    sealed interface Outcome {
        data class Completed(val files: List<File>, val name: String) : Outcome
        data class Failed(val message: String) : Outcome
    }

    private fun newSession(): SessionManager = SessionManager().also { it.start() }

    /**
     * Downloads [magnetOrTorrent] (a `magnet:?xt=...` URI or a path to a
     * `.torrent` file) into [saveDir]. Suspends until the torrent finishes or
     * [timeoutMillis] elapses.
     */
    suspend fun download(
        magnetOrTorrent: String,
        timeoutMillis: Long = 30 * 60 * 1000L,
        onProgress: suspend (TorrentProgress) -> Unit = {}
    ): Outcome = withContext(Dispatchers.IO) {
        val session = newSession()
        saveDir.mkdirs()
        try {
            val info: TorrentInfo = if (magnetOrTorrent.startsWith("magnet:")) {
                val bytes = session.fetchMagnet(magnetOrTorrent, /* timeoutSeconds = */ 60, saveDir)
                    ?: return@withContext Outcome.Failed("Could not resolve magnet (no peers/metadata).")
                TorrentInfo(bytes)
            } else {
                val f = File(magnetOrTorrent)
                if (!f.exists()) return@withContext Outcome.Failed("Torrent file not found.")
                TorrentInfo(f)
            }

            session.download(info, saveDir)
            val handle = session.find(info.infoHash())
                ?: return@withContext Outcome.Failed("Could not add torrent to the session.")

            val deadline = System.currentTimeMillis() + timeoutMillis
            while (System.currentTimeMillis() < deadline) {
                val st = handle.status()
                onProgress(TorrentProgress(st.name(), st.totalDone(), st.total(), st.downloadRate().toLong()))
                if (st.isFinished || st.isSeeding) {
                    val files = info.files().let { fs ->
                        (0 until fs.numFiles()).map { i -> File(saveDir, fs.filePath(i)) }
                    }
                    return@withContext Outcome.Completed(files, st.name())
                }
                delay(1000)
            }
            Outcome.Failed("Torrent timed out.")
        } catch (e: Exception) {
            Outcome.Failed(e.message ?: "Torrent failed.")
        } finally {
            runCatching { session.stop() }
        }
    }

    companion object {
        /** True for a magnet link or a `.torrent` path — the queue routes these to [TorrentEngine]. */
        fun isTorrent(link: String): Boolean =
            link.startsWith("magnet:") || link.substringBefore('?').endsWith(".torrent")
    }
}
