package com.cobalt.android.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI

/**
 * Fetches an HLS stream to a single local file by downloading every media
 * segment and concatenating them in order.
 *
 * The segmented byte-range engine ([SegmentedDownloadEngine]) can't touch an
 * HLS URL: a `.m3u8` is a text manifest, not the media, so a ranged GET over
 * it would just re-fetch the playlist. This engine is the manifest-aware
 * counterpart — it walks the playlist, pulls each `.ts`/fMP4 segment, and
 * writes them back-to-back into one file. A master playlist is resolved to its
 * highest-bandwidth variant first, so a caller that only has the master URL
 * (which is what most sources publish) still gets the best quality without a
 * separate probe step.
 *
 * The concatenated result is MPEG-TS unless the stream is fMP4 (`#EXT-X-MAP`
 * present, in which case the init segment is written first). MPEG-TS is not a
 * container the gallery indexes well, so [com.cobalt.android.download.DownloadService]
 * hands the finished file to ffmpeg for a stream-copy remux to MP4 — see
 * `HlsDownloadService` usage and `TranscodeProfile.REMUX_MP4`.
 */
class HlsDownloadEngine(private val client: OkHttpClient) {

    data class Result(val file: File, val segmentCount: Int, val finalUrl: String)

    private data class MediaPlaylist(
        val url: String,
        val segments: List<String>,
        val totalDurationSeconds: Double,
        val initSegment: String?
    )

    suspend fun download(
        url: String,
        headers: Map<String, String>,
        tempFile: File,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result = withContext(Dispatchers.IO) {
        val playlist = resolveMediaPlaylist(url, headers)
        if (playlist.segments.isEmpty()) throw IOException("HLS playlist has no segments")

        tempFile.parentFile?.mkdirs()
        val total = playlist.segments.size.toLong()
        var written = 0L

        FileOutputStream(tempFile).use { out ->
            // fMP4 streams need their initialisation segment before any media
            // segment or the concatenation won't decode.
            playlist.initSegment?.let { init ->
                out.write(fetchBytes(init, headers, playlist.url))
            }
            for (segment in playlist.segments) {
                currentCoroutineContext().ensureActive()
                out.write(fetchBytes(segment, headers, playlist.url))
                written++
                onProgress(written, total)
            }
        }
        Result(tempFile, playlist.segments.size, playlist.url)
    }

    /** Resolves a master playlist down to its highest-bandwidth media playlist. */
    private suspend fun resolveMediaPlaylist(url: String, headers: Map<String, String>): MediaPlaylist {
        var current = url
        // One master → one variant is the normal case; the loop tolerates a
        // variant that is itself another master (rare but legal).
        repeat(4) {
            val text = fetchText(current, headers)
            if (text.contains("#EXT-X-STREAM-INF")) {
                current = selectVariant(text, current)
            } else {
                return parseMediaPlaylist(text, current)
            }
        }
        return parseMediaPlaylist(fetchText(current, headers), current)
    }

    private fun selectVariant(master: String, baseUrl: String): String {
        var bestBandwidth = -1L
        var bestUri: String? = null
        val lines = master.lineSequence().map { it.trim() }.toList()
        for (i in lines.indices) {
            val line = lines[i]
            if (!line.startsWith("#EXT-X-STREAM-INF")) continue
            val bandwidth = Regex("""BANDWIDTH=(\d+)""").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val uri = lines.getOrNull(i + 1)?.takeIf { it.isNotBlank() && !it.startsWith("#") } ?: continue
            if (bandwidth > bestBandwidth) {
                bestBandwidth = bandwidth
                bestUri = uri
            }
        }
        return bestUri?.let { absolute(it, baseUrl) }
            ?: throw IOException("Master playlist has no variants")
    }

    private fun parseMediaPlaylist(text: String, baseUrl: String): MediaPlaylist {
        val segments = mutableListOf<String>()
        var init: String? = null
        var duration = 0.0
        var pendingExtinf = false
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-MAP") -> {
                    Regex("""URI="([^"]+)"""").find(line)?.groupValues?.get(1)?.let {
                        init = absolute(it, baseUrl)
                    }
                }
                line.startsWith("#EXTINF") -> {
                    pendingExtinf = true
                    line.substringAfter(':').substringBefore(',').toDoubleOrNull()?.let { duration += it }
                }
                line.isNotEmpty() && !line.startsWith("#") && pendingExtinf -> {
                    segments += absolute(line, baseUrl)
                    pendingExtinf = false
                }
            }
        }
        return MediaPlaylist(baseUrl, segments, duration, init)
    }

    private fun absolute(uri: String, baseUrl: String): String =
        runCatching { URI(baseUrl).resolve(uri).toString() }.getOrDefault(uri)

    private fun fetchText(url: String, headers: Map<String, String>): String =
        execute(url, headers).use { it.body?.string() ?: throw IOException("empty manifest") }

    private fun fetchBytes(url: String, headers: Map<String, String>, referer: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .apply { headers.forEach { (k, v) -> header(k, v) } }
            // Segment servers commonly 403 without the playlist as referer.
            .header("Referer", referer)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
            return response.body?.bytes() ?: throw IOException("empty segment $url")
        }
    }

    private fun execute(url: String, headers: Map<String, String>) =
        client.newCall(
            Request.Builder()
                .url(url)
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()
        ).execute().also {
            if (!it.isSuccessful) {
                val code = it.code
                it.close()
                throw IOException("HTTP $code for $url")
            }
        }

    companion object {
        /** True for a URL whose path ends in `.m3u8` (query string ignored). */
        fun isHlsUrl(url: String): Boolean =
            url.substringBefore('?').substringBefore('#').endsWith(".m3u8", ignoreCase = true)
    }
}
