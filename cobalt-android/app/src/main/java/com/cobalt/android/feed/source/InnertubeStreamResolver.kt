package com.cobalt.android.feed.source

import com.cobalt.android.feed.model.StreamKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Resolves a single video ID into a directly playable stream URL for inline
 * playback on the Home feed. Uses the Innertube `player` endpoint with the
 * ANDROID client, the same mechanism `shorts.source.InnertubeShortsSource`
 * uses (that client's player response returns progressive `formats[]` URLs
 * that are already fully resolved — no JS signature-cipher step).
 *
 * Kept separate from [InnertubeFeedSource] deliberately: listing the feed
 * must stay cheap (a couple of browse/search calls, no per-item work), so
 * the expensive per-video `player` call happens only when the user actually
 * taps play — not for every tile that scrolls past. A feed tile with an
 * unresolved stream still renders; this is the on-demand upgrade path.
 */
class InnertubeStreamResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    data class ResolvedStream(val url: String, val kind: StreamKind)

    /** Returns a playable stream for [videoId], or null if none is
     * available (private/age-gated/region-blocked, or the client keys
     * drifted — see [InnertubeFeedSource]'s maintenance note). */
    suspend fun resolve(videoId: String): ResolvedStream? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID")
                    put("clientVersion", ANDROID_CLIENT_VERSION)
                    put("androidSdkVersion", 34)
                })
            })
        }

        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?key=$ANDROID_CLIENT_KEY")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .header("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 14) gzip")
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                parsePlayerResponse(response.body?.string().orEmpty())
            }
        }.getOrNull()
    }

    /** Parsed out of [resolve] so it can be unit-tested against synthetic
     * Innertube JSON without a network call — the playability/format
     * selection logic is the part most likely to break when YouTube's
     * response shape drifts. */
    internal fun parsePlayerResponse(json: String): ResolvedStream? {
        if (json.isBlank()) return null
        val root = JSONObject(json)

        val status = root.optJSONObject("playabilityStatus")?.optString("status")
        if (status != "OK") return null

        val streamingData = root.optJSONObject("streamingData") ?: return null

        var bestUrl: String? = null
        var bestBitrate = -1
        streamingData.optJSONArray("formats")?.let { formats ->
            for (i in 0 until formats.length()) {
                val f = formats.getJSONObject(i)
                val url = f.optString("url").takeIf { it.isNotBlank() } ?: continue
                val bitrate = f.optInt("bitrate", 0)
                if (bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestUrl = url
                }
            }
        }

        val hls = streamingData.optString("hlsManifestUrl").takeIf { it.isNotBlank() }
        return when {
            bestUrl != null -> ResolvedStream(bestUrl!!, StreamKind.PROGRESSIVE)
            hls != null -> ResolvedStream(hls, StreamKind.HLS)
            else -> null
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val ANDROID_CLIENT_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
        private const val ANDROID_CLIENT_VERSION = "19.09.37"
    }
}
