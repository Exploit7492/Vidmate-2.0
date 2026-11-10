package com.cobalt.android.feed.explore

import com.cobalt.android.feed.InnertubeClient
import com.cobalt.android.feed.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Resolves a real YouTube playlist (`…/playlist?list=…`, or any watch URL that
 * carries `list=`) to its track list, via InnerTube `browse` with
 * `browseId=VL<playlistId>` (Phase "use Innertube fully", closes matrix 3.18).
 *
 * Public and unauthenticated. A playlist that belongs to a signed-in account
 * ("Watch later", "Liked") has no public page, so [load] returns null for those
 * — the caller falls back to the line-per-URL batch, which is the honest
 * behaviour without an account.
 */
class PlaylistRepository {

    data class Page(val title: String, val tracks: List<FeedItem>, val nextToken: String?)

    suspend fun load(url: String): Page? = withContext(Dispatchers.IO) {
        val id = ExploreRepository.playlistId(url) ?: return@withContext null
        browse("VL$id")
    }

    suspend fun loadMore(token: String): Page? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply { put("continuation", token) }
        InnertubeClient.post("browse", body)?.let { toPage(it) }
    }

    private fun browse(browseId: String): Page? =
        InnertubeClient.post("browse", JSONObject().apply { put("browseId", browseId) })
            ?.let { toPage(it) }

    private fun toPage(json: JSONObject): Page {
        val parsed = ExploreParser.parse(json)
        return Page(playlistTitle(json).orEmpty(), parsed.items, parsed.nextToken)
    }

    /** The playlist header title (`pageHeaderRenderer` / `playlistHeaderRenderer`). */
    private fun playlistTitle(json: JSONObject): String? {
        val candidates = listOf("pageHeaderRenderer", "playlistHeaderRenderer")
        for (key in candidates) {
            val title = findTitle(json, key)
            if (!title.isNullOrBlank()) return title
        }
        return null
    }

    private fun findTitle(node: Any?, key: String): String? {
        when (node) {
            is JSONObject -> {
                node.optJSONObject(key)?.let { header ->
                    header.optJSONObject("title")?.let { titleNode ->
                        val s = titleNode.optString("simpleText")
                        if (s.isNotBlank()) return s
                    }
                    header.optJSONObject("pageTitle")?.let { pageTitle ->
                        val s = runsText(pageTitle)
                        if (s.isNotBlank()) return s
                    }
                }
                val keys = node.keys()
                while (keys.hasNext()) findTitle(node.opt(keys.next()), key)?.let { return it }
            }
            is JSONArray -> for (i in 0 until node.length()) findTitle(node.opt(i), key)?.let { return it }
        }
        return null
    }

    /** Concatenates a `{runs:[{text}]}` node, or "" when it is not one. */
    private fun runsText(node: JSONObject): String {
        val runs = node.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) sb.append(runs.getJSONObject(i).optString("text"))
        return sb.toString()
    }
}
