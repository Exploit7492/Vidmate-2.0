package com.cobalt.android.feed.explore

import com.cobalt.android.feed.model.FeedItem
import com.cobalt.android.feed.model.FeedSourceType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the two InnerTube surfaces that list videos for browsing (Phase "use
 * Innertube fully"): `search` results and the `browse` trending grid.
 *
 * Both wrap videos the same two ways — a bare `videoRenderer` (search) or a
 * `richItemRenderer.content.videoRenderer` (grids) — so one walker handles
 * both, de-dupes by id and reads the trailing continuation token for "load
 * more". Kept pure so it is unit tested without a network.
 */
object ExploreParser {

    data class Page(
        val items: List<FeedItem>,
        val nextToken: String?
    )

    fun parse(root: JSONObject): Page {
        val items = LinkedHashMap<String, FeedItem>()
        walk(root) { node ->
            // Grid shape.
            node.optJSONObject("richItemRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("videoRenderer")
                ?.let { add(items, it) }
            // Flat shape (search results, playlist video list).
            node.optJSONObject("videoRenderer")?.let { add(items, it) }
        }
        return Page(items.values.toList(), continuationToken(root))
    }

    /** The `continuationItemRenderer` token anywhere in the response, if any. */
    fun continuationToken(root: JSONObject): String? {
        var token: String? = null
        walk(root) { node ->
            if (token == null) {
                node.optJSONObject("continuationItemRenderer")
                    ?.optJSONObject("continuationEndpoint")
                    ?.optJSONObject("continuationCommand")
                    ?.optString("token")
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { token = it }
            }
        }
        return token
    }

    /**
     * The headline count YouTube renders above search results
     * ("About 1,200,000 results") — surfaced so the search screen can show it.
     */
    fun estimatedResultText(root: JSONObject): String? {
        var text: String? = null
        walk(root) { node ->
            if (text == null) {
                node.optJSONObject("estimatedResults")?.let {
                    text = it.optString("simpleText").takeIf { s -> s.isNotBlank() }
                }
            }
        }
        return text
    }

    private fun add(acc: LinkedHashMap<String, FeedItem>, r: JSONObject) {
        val id = r.optString("videoId")
        if (id.length == 11) acc.putIfAbsent(id, toItem(id, r))
    }

    private fun toItem(videoId: String, r: JSONObject): FeedItem {
        val thumbs = r.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumb = thumbs?.let { if (it.length() == 0) null else it.getJSONObject(it.length() - 1).optString("url") }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        return FeedItem(
            videoId = videoId,
            title = text(r.optJSONObject("title")) ?: "Untitled",
            channelName = text(r.optJSONObject("ownerText"))
                ?: text(r.optJSONObject("longBylineText")).orEmpty(),
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
