package com.cobalt.android.feed.channel

import com.cobalt.android.feed.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Loads a YouTube channel's video feed (Phase H5).
 *
 * A `/channel/UC…` URL already contains the browse ID; a `/@handle` URL does
 * not, so that case goes through Innertube's `navigation/resolve_url` first.
 * Public and unauthenticated, same as the rest of the feed stack.
 */
class ChannelRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun load(url: String): ChannelParser.Page? = withContext(Dispatchers.IO) {
        runCatching {
            val browseId = ChannelParser.channelBrowseId(url) ?: resolveHandle(url) ?: return@runCatching null
            val body = JSONObject().apply {
                put("browseId", browseId)
                put("context", webContext())
            }
            val json = post("https://www.youtube.com/youtubei/v1/browse?key=$WEB_CLIENT_KEY", body)
                ?: return@runCatching null
            ChannelParser.parse(JSONObject(json))
        }.getOrNull()
    }

    /** Loads the next page of a channel feed from its continuation token. */
    suspend fun loadMore(token: String): ChannelParser.Page? = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("continuation", token)
                put("context", webContext())
            }
            val json = post("https://www.youtube.com/youtubei/v1/browse?key=$WEB_CLIENT_KEY", body)
                ?: return@runCatching null
            ChannelParser.parse(JSONObject(json))
        }.getOrNull()
    }

    private fun resolveHandle(url: String): String? {
        val handle = ChannelParser.channelHandle(url) ?: return null
        val body = JSONObject().apply {
            put("url", "https://www.youtube.com/@$handle")
            put("context", webContext())
        }
        val json = post("https://www.youtube.com/youtubei/v1/navigation/resolve_url?key=$WEB_CLIENT_KEY", body)
            ?: return null
        return ChannelParser.resolvedBrowseId(JSONObject(json))
    }

    private fun post(url: String, body: JSONObject): String? {
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        }.getOrNull()
    }

    private fun webContext() = JSONObject().apply {
        put("client", JSONObject().apply {
            put("clientName", "WEB")
            put("clientVersion", "2.20240701.00.00")
        })
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val WEB_CLIENT_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

        /** True when [url] looks like a YouTube channel URL we can open (H5). */
        fun isChannelUrl(url: String): Boolean {
            val u = url.trim()
            return ChannelParser.channelBrowseId(u) != null ||
                (u.contains("youtube.com/@") && ChannelParser.channelHandle(u) != null)
        }
    }
}
