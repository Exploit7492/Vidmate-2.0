package com.cobalt.android.download

/**
 * Detects YouTube's SABR (server-adaptive bitrate) media URLs. SABR streams
 * carry the payload in the response body with the format metadata in a
 * `videoplayback`/`sabr=1` shape and answer `206` only for whole "sequence"
 * windows, so a byte-range split corrupts them. The engine therefore routes
 * these to the single-stream path (and never splits them), the same choice
 * VidMate's engine makes for SABR.
 */
object SabrDetector {

    fun isSabr(url: String, mimeType: String = ""): Boolean {
        val u = url.lowercase()
        if (u.contains("sabr=1") || u.contains("&sabr=")) return true
        // YouTube's adaptive (non-progressive) itag responses are SABR-shaped.
        if (u.contains("videoplayback") && (u.contains("itag=") || u.contains("/sabr/"))) return true
        if (mimeType.equals("application/vnd.yt-ump", true)) return true
        return false
    }
}
