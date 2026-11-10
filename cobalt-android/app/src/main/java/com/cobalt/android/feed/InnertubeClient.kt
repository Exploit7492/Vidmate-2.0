package com.cobalt.android.feed

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * The one place the app talks to YouTube's public InnerTube API.
 *
 * Every feed/search/channel/comment surface used to carry its own copy of the
 * endpoint prefix, the JSON media type, the WEB client key and the request
 * context. They all made the same call, so they now share this. The key below
 * is the public, unauthenticated one youtube.com's own web client ships in its
 * page source — it is not a private credential and needs no sign-in, which is
 * what lets the whole YouTube surface work without a Google account (there is
 * deliberately no OAuth path; see the matrix, row 3.19).
 *
 * All calls are best-effort: a failure returns null and the caller degrades,
 * rather than throwing into a UI coroutine.
 */
object InnertubeClient {

    private val JSON_MEDIA_TYPE = "application/json".toMediaType()

    /** Public web-client key, identical to the one youtube.com embeds. */
    const val WEB_CLIENT_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private const val WEB_CLIENT_VERSION = "2.20240701.00.00"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** POSTs [body] (with the standard web context merged in) to [path]. */
    fun post(path: String, body: JSONObject, mergeContext: Boolean = true): JSONObject? {
        if (mergeContext && !body.has("context")) body.put("context", webContext())
        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/$path?key=$WEB_CLIENT_KEY")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()?.let { JSONObject(it) }
            }
        }.getOrNull()
    }

    fun webContext(): JSONObject = JSONObject().apply {
        put("client", JSONObject().apply {
            put("clientName", "WEB")
            put("clientVersion", WEB_CLIENT_VERSION)
        })
    }
}
