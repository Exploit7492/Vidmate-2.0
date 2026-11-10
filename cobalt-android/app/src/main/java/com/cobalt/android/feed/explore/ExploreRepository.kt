package com.cobalt.android.feed.explore

import com.cobalt.android.feed.InnertubeClient
import com.cobalt.android.feed.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The Innertube browse/search surfaces beyond the Home feed (Phase "use
 * Innertube fully"): in-app search, the trending grid, generic continuation
 * paging, YouTube-Music discovery, and playlist track listing.
 *
 * All of it is public and unauthenticated — the same public web-client key the
 * rest of the feed stack uses. Nothing here reads or writes a YouTube account,
 * so there is no sign-in, no OAuth and no credential of any kind.
 */
class ExploreRepository {

    /** Search results for a query, with the "About N results" headline. */
    suspend fun search(query: String): SearchPage? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply { put("query", query) }
        InnertubeClient.post("search", body)?.let { json ->
            val page = ExploreParser.parse(json)
            SearchPage(page.items, page.nextToken, ExploreParser.estimatedResultText(json))
        }
    }

    /** The next page of any browse/search/playlist result, by continuation. */
    suspend fun continueFrom(token: String): ExploreParser.Page? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply { put("continuation", token) }
        InnertubeClient.post("browse", body)?.let { ExploreParser.parse(it) }
    }

    /** Trending videos (`FEtrending`). */
    suspend fun trending(): ExploreParser.Page? = browse("FEtrending")

    /** YouTube Music discovery feed (`FEmusic_home`) — audio-first items. */
    suspend fun musicHome(): ExploreParser.Page? = browse("FEmusic_home")

    private fun browse(browseId: String): ExploreParser.Page? =
        InnertubeClient.post("browse", JSONObject().apply { put("browseId", browseId) })
            ?.let { ExploreParser.parse(it) }

    data class SearchPage(
        val items: List<FeedItem>,
        val nextToken: String?,
        val estimatedResults: String?
    )

    companion object {
        /** Extracts a YouTube playlist id (`list=…`) from a URL, if present. */
        fun playlistId(url: String): String? =
            Regex("""[?&]list=([A-Za-z0-9_-]+)""").find(url.trim())?.groupValues?.get(1)
    }
}
