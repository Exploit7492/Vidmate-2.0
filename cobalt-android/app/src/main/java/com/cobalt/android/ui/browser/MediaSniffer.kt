package com.cobalt.android.ui.browser

/**
 * Classifies URLs seen by the in-app browser as downloadable media.
 *
 * Kept free of Android types (only `String`s in, `String`s out) so the
 * classification can be unit tested on the JVM. The browser feeds it every
 * request it observes; the fragment then offers the retained candidates.
 *
 * A URL is media if its path ends in a known media extension (query string
 * ignored, since CDNs append tokens) **or** the request's `Accept` header asks
 * for video/audio (which is how an extension-less HLS/DASH manifest or a
 * signed stream URL announces itself).
 */
class MediaSniffer(private val maxCandidates: Int = 40) {

    private val seen = LinkedHashSet<String>()

    /** Records [url] if it looks like media. [acceptHeader] may be null. */
    fun observe(url: String?, acceptHeader: String?) {
        if (url.isNullOrBlank()) return
        if (seen.size >= maxCandidates) return
        if (isMedia(url, acceptHeader)) seen.add(url)
    }

    fun candidates(): List<String> = seen.toList()

    fun clear() = seen.clear()

    fun isMedia(url: String, acceptHeader: String?): Boolean {
        val lower = url.lowercase()
        if (MEDIA_EXTENSIONS.any { lower.contains(it) }) return true
        if (acceptHeader != null) {
            val a = acceptHeader.lowercase()
            if (a.contains("video/") || a.contains("audio/") || a.contains("mpegurl") ||
                a.contains("dash+xml") || a.contains("octet-stream")
            ) return true
        }
        return false
    }

    companion object {
        // Matched with `contains` rather than `endsWith` so a trailing query
        // string or fragment (e.g. "…/v.mp4?token=…") still matches.
        private val MEDIA_EXTENSIONS = listOf(
            ".mp4", ".m4v", ".mkv", ".webm", ".mov", ".avi", ".flv", ".3gp", ".ts",
            ".m3u8", ".mpd", ".mp3", ".m4a", ".aac", ".ogg", ".opus", ".flac", ".wav"
        )
    }
}
