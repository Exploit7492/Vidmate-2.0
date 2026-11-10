package com.cobalt.android.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

/**
 * Shortens a share link through a configurable shortener host (VidMate routes
 * shares through its own `sharevideo*` hosts). The host is user-configurable in
 * Settings; the built-in default is a public, key-free shortener so the feature
 * works out of the box. Any failure falls back to the original URL — a share
 * must never break because the shortener is down.
 */
class ShareLinkShortener(
    private val client: OkHttpClient = OkHttpClient(),
    private val defaultHost: String = DEFAULT_HOST
) {
    /** Returns the shortened URL, or [url] unchanged on any failure. */
    fun shorten(url: String, configuredHost: String = ""): String {
        val host = configuredHost.ifBlank { defaultHost }.trimEnd('/')
        return runCatching {
            val api = "$host/api?format=text&url=${URLEncoder.encode(url, "UTF-8")}"
            val request = Request.Builder().url(api).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim().orEmpty()
                if (response.isSuccessful && body.startsWith("http")) body else url
            }
        }.getOrDefault(url)
    }

    companion object {
        const val DEFAULT_HOST = "https://is.gd"
    }
}
