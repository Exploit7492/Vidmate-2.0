package com.cobalt.android.feed.comments

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches a YouTube video's comments via Innertube (Phase E11).
 *
 * Comments are never inlined in the `next` response; you ask for the `next`
 * page, read the comments continuation token out of it, then POST that token to
 * `browse` to get the comments themselves. This does exactly those two calls
 * and hands the JSON to [CommentsParser].
 *
 * Unauthenticated: this reads only public comments, on the same public web
 * client key the feed sources use. There is no sign-in (E9 is excluded), so
 * there is no "post a comment" path and none is implied.
 */
class CommentsRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    /** The first page of comments for [videoId], or null on any failure. */
    suspend fun fetch(videoId: String): CommentsParser.Page? = withContext(Dispatchers.IO) {
        val token = runCatching { fetchContinuationToken(videoId) }.getOrNull() ?: return@withContext null
        runCatching {
            val body = JSONObject().apply {
                put("continuation", token)
                put("context", webContext())
            }
            val response = post("https://www.youtube.com/youtubei/v1/browse?key=$WEB_CLIENT_KEY", body)
                ?: return@withContext null
            CommentsParser.parseComments(JSONObject(response))
        }.getOrNull()
    }

    private fun fetchContinuationToken(videoId: String): String? {
        val body = JSONObject().apply {
            put("videoId", videoId)
            put("context", webContext())
        }
        val response = post("https://www.youtube.com/youtubei/v1/next?key=$WEB_CLIENT_KEY", body)
            ?: return null
        return CommentsParser.commentContinuationToken(JSONObject(response))
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

        // Same public, unauthenticated web key the feed sources embed.
        private const val WEB_CLIENT_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    }
}
