package com.cobalt.android.feed.model

/**
 * Everything one search query can return. YouTube's search page mixes
 * renderers — a video can sit next to a channel card or a playlist shelf —
 * so the header's single field has to be able to show all three, not just
 * videos. Kept as three lists (rather than one sealed hierarchy) because the
 * screen renders each in its own row and a "kind" flag on every item would
 * only be read by the renderer that already knows what list it is walking.
 */
data class SearchResults(
    val videos: List<FeedItem> = emptyList(),
    val channels: List<SearchChannel> = emptyList(),
    val playlists: List<SearchPlaylist> = emptyList()
) {
    val isEmpty: Boolean get() = videos.isEmpty() && channels.isEmpty() && playlists.isEmpty()

    companion object {
        val EMPTY = SearchResults()
    }
}

/** A channel result: tapping it opens the channel screen, not the player. */
data class SearchChannel(
    val channelId: String,
    val name: String,
    val avatarUrl: String,
    val subscriberText: String
) {
    /** Channel URLs are browse surfaces; the channel screen takes this form. */
    val browseUrl: String get() = "https://www.youtube.com/channel/$channelId"
}

/** A playlist result: tapping it opens the playlist download flow. */
data class SearchPlaylist(
    val playlistId: String,
    val title: String,
    val thumbnailUrl: String,
    val videoCountText: String
) {
    val playlistUrl: String get() = "https://www.youtube.com/playlist?list=$playlistId"
}
