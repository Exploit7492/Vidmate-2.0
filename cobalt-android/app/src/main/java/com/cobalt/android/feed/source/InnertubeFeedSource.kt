package com.cobalt.android.feed.source

import com.cobalt.android.feed.model.FeedItem
import com.cobalt.android.feed.model.SearchChannel
import com.cobalt.android.feed.model.SearchPlaylist
import com.cobalt.android.feed.model.SearchResults
import com.cobalt.android.feed.model.FeedSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Home feed source built on YouTube's internal "Innertube" API, the same
 * one `shorts.source.InnertubeShortsSource` uses — deliberately the same
 * approach rather than a fourth parallel YouTube client (see
 * ARCHITECTURE.md Phase 23's "reuses/extends the Shorts pipeline's
 * Innertube integration" note).
 *
 * Key difference from the Shorts source: Home items are NOT streamed
 * inline. Tapping one hands its URL to cobalt (resolve → pick → download),
 * exactly like a pasted link — so this source never needs a per-video
 * `player` call to obtain a playable stream URL. The `browse`/`search`
 * responses already carry everything a feed tile renders (title, channel,
 * thumbnail, view count, published time, length), which makes this source
 * far cheaper per refresh than the Shorts one (a handful of requests
 * total, not one `player` call per candidate).
 *
 * Discovery combines:
 * 1. `browse` against the home feed (`FEwhat_to_watch`) — YouTube's own
 *    recommendation surface, `videoRenderer` entries under
 *    `richItemRenderer`.
 * 2. `search` against a rotating pool of evergreen queries
 *    ([FeedQueryFeeder]) for volume and variety.
 *
 * Shorts-length results (<= 60s) are filtered out, so Home and the Shorts
 * tab stay two genuinely different surfaces rather than the same clips in
 * two places.
 *
 * Maintenance note: `WEB_CLIENT_KEY`/client version below are the same
 * YouTube-internal values `InnertubeShortsSource` documents and drift over
 * time — if this starts returning empty pages, check those first.
 */
class InnertubeFeedSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : FeedSource {

    override val type = FeedSourceType.INNERTUBE

    override suspend fun fetchFeed(count: Int): List<FeedItem> = withContext(Dispatchers.IO) {
        val collected = LinkedHashMap<String, FeedItem>()

        // Primary: the home-feed recommendation surface.
        for (item in parseBrowse(browseHomeFeed())) {
            if (collected.size >= count * 2) break
            collected.putIfAbsent(item.videoId, item)
        }

        // Secondary: rotating search queries, for volume/variety.
        for (query in FeedQueryFeeder.nextQueries(QUERIES_PER_FETCH)) {
            if (collected.size >= count * 2) break
            for (item in parseSearch(searchRaw(query))) {
                collected.putIfAbsent(item.videoId, item)
            }
        }

        collected.values.filter { it.durationSeconds > MIN_REGULAR_SECONDS }.take(count)
    }

    /**
     * One-off search for a specific user query (the header's free-text path),
     * as opposed to the rotating evergreen query pool [fetchFeed] blends in.
     * Returns videos, channels and playlists together — YouTube's search page
     * interleaves all three renderers, so a videos-only parse would silently
     * drop the channel the user was obviously looking for.
     */
    suspend fun searchAll(query: String, count: Int = 30): SearchResults =
        withContext(Dispatchers.IO) {
            val json = searchRaw(query)
            SearchResults(
                videos = parseSearch(json).filter(::isRegularOrUnknown).take(count),
                channels = parseChannels(json).take(count),
                playlists = parsePlaylists(json).take(count)
            )
        }

    /** Videos-only view of [searchAll], kept for callers that only want tiles. */
    suspend fun searchVideos(query: String, count: Int = 30): List<FeedItem> =
        searchAll(query, count).videos

    /**
     * Regular-length video, or one whose length the parser couldn't read
     * (live streams and premieres carry no `lengthText`). Unknown lengths are
     * kept rather than dropped: a Shorts-length *known* item is left to the
     * Shorts tab, but hiding something the parser simply couldn't measure
     * would turn a parser gap into a missing search result.
     */
    private fun isRegularOrUnknown(item: FeedItem): Boolean =
        item.durationSeconds <= 0 || item.durationSeconds > MIN_REGULAR_SECONDS

    // ── Network ─────────────────────────────────────────────────────────────

    private fun browseHomeFeed(): String? = post(
        url = "https://www.youtube.com/youtubei/v1/browse?key=$WEB_CLIENT_KEY",
        body = JSONObject().apply {
            put("browseId", "FEwhat_to_watch")
            put("context", webContext())
        }
    )

    private fun searchRaw(query: String): String? = post(
        url = "https://www.youtube.com/youtubei/v1/search?key=$WEB_CLIENT_KEY",
        body = JSONObject().apply {
            put("query", query)
            put("context", webContext())
        }
    )

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

    // ── Parsing ─────────────────────────────────────────────────────────────
    // These are `internal` (rather than private) so unit tests can feed them
    // synthetic Innertube JSON directly — the response shape is the most
    // error-prone part of this class and isn't otherwise reachable without a
    // live network call.

    internal fun parseBrowse(json: String?): List<FeedItem> =
        json?.let { runCatching { parseVideoRenderers(JSONObject(it)) }.getOrDefault(emptyList()) }
            .orEmpty()

    internal fun parseSearch(json: String?): List<FeedItem> =
        json?.let { runCatching { parseVideoRenderers(JSONObject(it)) }.getOrDefault(emptyList()) }
            .orEmpty()

    internal fun parseVideoRenderers(root: JSONObject): List<FeedItem> {
        val out = mutableListOf<FeedItem>()
        walk(root, "videoRenderer") { r ->
            val id = r.optString("videoId")
            if (id.length == 11) out.add(toFeedItem(id, r))
        }
        return out
    }

    internal fun parseChannels(json: String?): List<SearchChannel> =
        json?.let { runCatching { parseChannelRenderers(JSONObject(it)) }.getOrDefault(emptyList()) }
            .orEmpty()

    internal fun parsePlaylists(json: String?): List<SearchPlaylist> =
        json?.let { runCatching { parsePlaylistRenderers(JSONObject(it)) }.getOrDefault(emptyList()) }
            .orEmpty()

    private fun parseChannelRenderers(root: JSONObject): List<SearchChannel> {
        val out = mutableListOf<SearchChannel>()
        walk(root, "channelRenderer") { r ->
            val id = r.optString("channelId")
            if (id.isNotBlank()) {
                out.add(
                    SearchChannel(
                        channelId = id,
                        name = text(r.optJSONObject("title")) ?: "Channel",
                        avatarUrl = thumbnailUrl(r.optJSONObject("thumbnail")),
                        subscriberText = text(r.optJSONObject("videoCountText"))
                            ?: text(r.optJSONObject("subscriberCountText")).orEmpty()
                    )
                )
            }
        }
        return out
    }

    private fun parsePlaylistRenderers(root: JSONObject): List<SearchPlaylist> {
        val out = mutableListOf<SearchPlaylist>()
        walk(root, "playlistRenderer") { r ->
            val id = r.optString("playlistId")
            if (id.isNotBlank()) {
                // playlistRenderer carries "thumbnails" as a bare array (not a
                // "thumbnail"/"thumbnails" object like the other renderers).
                val thumbs = r.optJSONArray("thumbnails")
                val thumb = thumbs?.let { arr ->
                    if (arr.length() == 0) null else arr.getJSONObject(arr.length() - 1).optString("url")
                }.orEmpty()
                out.add(
                    SearchPlaylist(
                        playlistId = id,
                        title = text(r.optJSONObject("title")) ?: "Playlist",
                        thumbnailUrl = thumb,
                        videoCountText = text(r.optJSONObject("videoCountText"))
                            ?: r.optString("videoCount").let { if (it.isBlank()) "" else "$it videos" }
                    )
                )
            }
        }
        return out
    }

    /** Last (largest) thumbnail URL from a `{"thumbnails":[...]}` object. */
    private fun thumbnailUrl(node: JSONObject?): String {
        val arr = node?.optJSONArray("thumbnails") ?: return ""
        if (arr.length() == 0) return ""
        return arr.getJSONObject(arr.length() - 1).optString("url")
    }

    private fun toFeedItem(videoId: String, r: JSONObject): FeedItem {
        val thumbs = r.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumbUrl = thumbs?.let { arr ->
            if (arr.length() == 0) null else arr.getJSONObject(arr.length() - 1).optString("url")
        } ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return FeedItem(
            videoId = videoId,
            title = text(r.optJSONObject("title")) ?: "Untitled",
            channelName = text(r.optJSONObject("ownerText"))
                ?: text(r.optJSONObject("longBylineText")).orEmpty(),
            thumbnailUrl = thumbUrl,
            viewCountText = text(r.optJSONObject("viewCountText")).orEmpty(),
            publishedText = text(r.optJSONObject("publishedTimeText")).orEmpty(),
            durationSeconds = parseDurationText(text(r.optJSONObject("lengthText")).orEmpty()),
            source = FeedSourceType.INNERTUBE
        )
    }

    /**
     * YouTube renders text nodes either as `{"simpleText": "..."}` or as
     * `{"runs": [{"text": "..."}, ...]}` — this handles both.
     */
    private fun text(node: JSONObject?): String? {
        node ?: return null
        node.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = node.optJSONArray("runs") ?: return null
        val sb = StringBuilder()
        for (i in 0 until runs.length()) sb.append(runs.getJSONObject(i).optString("text"))
        return sb.toString().ifBlank { null }
    }

    /** Recursively walks a JSON tree, invoking [onMatch] for every object
     * nested under [key] (e.g. `videoRenderer`). */
    private fun walk(node: Any?, key: String, onMatch: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let(onMatch)
                val keys = node.keys()
                while (keys.hasNext()) walk(node.opt(keys.next()), key, onMatch)
            }
            is JSONArray -> {
                for (i in 0 until node.length()) walk(node.opt(i), key, onMatch)
            }
        }
    }

    /** Parses "0:45" / "3:12" / "1:02:33" into total seconds; -1 if unparseable. */
    private fun parseDurationText(text: String): Long {
        if (text.isBlank()) return -1
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty() || parts.size != text.count { it == ':' } + 1) return -1
        return parts.fold(0L) { acc, p -> acc * 60 + p }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        // Public, unauthenticated Innertube key embedded in youtube.com's own
        // web client — not a private credential. Same value the Shorts
        // source uses (see its companion object for the full note).
        private const val WEB_CLIENT_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        private const val QUERIES_PER_FETCH = 3

        /** Home shows regular-length video; anything at or below this is
         * treated as Shorts-length and left to the Shorts tab, so the two
         * surfaces don't duplicate each other. */
        private const val MIN_REGULAR_SECONDS = 60L
    }
}
