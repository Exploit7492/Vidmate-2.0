package com.cobalt.android.feed.channel

import com.cobalt.android.feed.model.FeedItem
import com.cobalt.android.feed.model.FeedSourceType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses a YouTube channel page (Phase H5).
 *
 * The channel's video grid uses `richItemRenderer > videoRenderer`, which is
 * why this doesn't reuse `InnertubeFeedSource.parseVideoRenderers` — that walks
 * for a bare `videoRenderer` key and would also pick up the channel's shelf
 * rows, duplicates and all. Kept pure so it is unit tested.
 */
object ChannelParser {

    data class Page(
        val channelName: String,
        val videos: List<FeedItem>,
        val nextToken: String?
    )

    /** Channel `browseId` when the URL already carries `/channel/UC…`. */
    fun channelBrowseId(url: String): String? =
        Regex("""/channel/(UC[\w-]{22})""").find(url.trim())?.groupValues?.get(1)

    /** The `@handle` in a channel URL, or null (resolved via resolve_url). */
    fun channelHandle(url: String): String? =
        Regex("""@([A-Za-z0-9._-]+)""").find(url.trim())?.groupValues?.get(1)

    /** The `browseId` a `navigation/resolve_url` response resolves a handle to. */
    fun resolvedBrowseId(json: JSONObject): String? {
        var found: String? = null
        walk(json) { node ->
            if (found == null) {
                node.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")
                    ?.takeIf { it.startsWith("UC") }
                    ?.let { found = it }
            }
        }
        return found
    }

    fun parse(json: JSONObject): Page {
        val name = headerTitle(json) ?: ""
        val videos = LinkedHashMap<String, FeedItem>()
        walk(json) { node ->
            val item = node.optJSONObject("richItemRenderer") ?: return@walk
            val r = item.optJSONObject("content")?.optJSONObject("videoRenderer") ?: return@walk
            val id = r.optString("videoId")
            if (id.length == 11) videos.putIfAbsent(id, toItem(id, r))
        }
        var next: String? = null
        walk(json) { node ->
            if (next == null) {
                node.optJSONObject("continuationItemRenderer")
                    ?.optJSONObject("continuationEndpoint")
                    ?.optJSONObject("continuationCommand")
                    ?.optString("token")
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { next = it }
            }
        }
        return Page(name, videos.values.toList(), next)
    }

    private fun headerTitle(json: JSONObject): String? {
        var title: String? = null
        walk(json) { node ->
            if (title == null) {
                node.optJSONObject("pageHeaderRenderer")
                    ?.optJSONObject("pageTitle")
                    ?.let { title = text(it) }
            }
        }
        return title
    }

    private fun toItem(videoId: String, r: JSONObject): FeedItem {
        val thumbs = r.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumb = thumbs?.let { if (it.length() == 0) null else it.getJSONObject(it.length() - 1).optString("url") }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        return FeedItem(
            videoId = videoId,
            title = text(r.optJSONObject("title")) ?: "Untitled",
            channelName = text(r.optJSONObject("ownerText")).orEmpty(),
            thumbnailUrl = thumb,
            viewCountText = text(r.optJSONObject("viewCountText")).orEmpty(),
            publishedText = text(r.optJSONObject("publishedTimeText")).orEmpty(),
            durationSeconds = duration(text(r.optJSONObject("lengthText")).orEmpty()),
            source = FeedSourceType.INNERTUBE
        )
    }

    private fun text(node: JSONObject?): String? {
        node ?: return null
        node.optString("simpleText").takeIf { it.isNotBlank() }?.let { return it }
        val runs = node.optJSONArray("runs") ?: return null
        val sb = StringBuilder()
        for (i in 0 until runs.length()) sb.append(runs.getJSONObject(i).optString("text"))
        return sb.toString().ifBlank { null }
    }

    private fun duration(text: String): Long {
        if (text.isBlank()) return -1
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty() || parts.size != text.count { it == ':' } + 1) return -1
        return parts.fold(0L) { acc, p -> acc * 60 + p }
    }

    private fun walk(node: Any?, onMatch: (JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                onMatch(node)
                val keys = node.keys()
                while (keys.hasNext()) walk(node.opt(keys.next()), onMatch)
            }
            is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i), onMatch)
        }
    }
}
