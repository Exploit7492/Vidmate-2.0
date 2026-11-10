package com.cobalt.android.util

/**
 * JS/WebView-driven extraction (C3): pull direct media URLs out of a rendered
 * page's HTML. VidMate does this with an injected JS hook that scans the DOM
 * plus every `<video>/<source>` and the OpenGraph tags; this is the same idea in
 * pure Kotlin so it can be unit tested and reused both from a WebView's
 * `evaluateJavascript` result and from a fetched page body.
 *
 * Pure and Android-free. Returns URLs in priority order (og:video, then
 * `<video>/<source>`, then raw `.m3u8/.mp4/.webm/.mp3` literals), de-duplicated.
 */
object WebMediaSniffer {

    private val ogVideo = Regex(
        """<meta[^>]+property=["']og:video(?::url|:secure_url)?["'][^>]+content=["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val videoSource = Regex(
        """<(?:video|source)[^>]+src=["']([^"']+\.(?:m3u8|mpd|mp4|webm|mkv|mp3|m4a)[^"']*)["']""",
        RegexOption.IGNORE_CASE
    )
    private val rawMedia = Regex(
        """["'](https?://[^"'\s]+\.(?:m3u8|mpd|mp4|webm|mkv|mp3|m4a)(?:\?[^"'\s]*)?)["']""",
        RegexOption.IGNORE_CASE
    )

    fun sniff(html: String): List<String> {
        if (html.isBlank()) return emptyList()
        val found = LinkedHashSet<String>()
        ogVideo.findAll(html).forEach { found += unescape(it.groupValues[1]) }
        videoSource.findAll(html).forEach { found += unescape(it.groupValues[1]) }
        rawMedia.findAll(html).forEach { found += unescape(it.groupValues[1]) }
        return found.map { absolutize(it) }.filter { it.isNotBlank() }.toList()
    }

    /** The mime type implied by a media URL's extension. */
    fun mimeTypeFor(url: String): String {
        val path = url.substringBefore('?').lowercase()
        return when {
            path.endsWith(".m3u8") -> "application/x-mpegURL"
            path.endsWith(".mpd") -> "application/dash+xml"
            path.endsWith(".mp4") -> "video/mp4"
            path.endsWith(".webm") -> "video/webm"
            path.endsWith(".mkv") -> "video/x-matroska"
            path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".m4a") -> "audio/mp4"
            else -> "video/*"
        }
    }

    fun filenameFor(url: String): String {
        val path = url.substringBefore('?')
        return path.substringAfterLast('/').ifBlank { "media" }
    }

    private fun unescape(s: String) = s.replace("&amp;", "&").replace("\\/", "/")

    /** Turns protocol-relative ("//cdn/..") or root-relative ("/v.mp4") into absolute. */
    private fun absolutize(url: String): String = when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}
