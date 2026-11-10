package com.cobalt.android.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Online lyrics search (LRCLIB — free, key-less, no account). Used by the music
 * screen when a track has no sidecar `.lrc` and the user has online lyrics on.
 * Returns plain (unsynced) lyrics text, or null when nothing is found or the
 * network fails — the caller keeps showing its "no lyrics" state.
 */
class OnlineLyricsSource(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun fetch(title: String, artist: String, durationSeconds: Long = 0): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = buildString {
                    append("https://lrclib.net/api/get?track_name=")
                    append(enc(title))
                    if (artist.isNotBlank()) { append("&artist_name="); append(enc(artist)) }
                    if (durationSeconds > 0) { append("&duration="); append(durationSeconds) }
                }
                val body = get(q) ?: get(
                    "https://lrclib.net/api/search?q=" + enc(listOf(title, artist).filter { it.isNotBlank() }.joinToString(" "))
                ) ?: return@runCatching null
                body
            }.getOrNull()
        }

    private fun get(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", "CobaltAndroid/1.0").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            parse(text)
        }
    }

    /** LRCLIB's `/get` returns one object; `/search` returns an array. */
    private fun parse(text: String): String? {
        val trimmed = text.trim()
        val obj = if (trimmed.startsWith("[")) {
            val arr = org.json.JSONArray(trimmed)
            if (arr.length() == 0) null else arr.getJSONObject(0)
        } else {
            JSONObject(trimmed)
        } ?: return null
        val plain = obj.optString("plainLyrics").takeIf { it.isNotBlank() }
        val synced = obj.optString("syncedLyrics").takeIf { it.isNotBlank() }
        return plain ?: synced
    }

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
