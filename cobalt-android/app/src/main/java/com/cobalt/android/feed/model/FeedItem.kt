package com.cobalt.android.feed.model

/**
 * One item in the Home feed. Deliberately a separate type from
 * `shorts.model.ShortItem` rather than a reuse of it: a Home feed item is
 * regular-length (minutes, not seconds), carries a channel identity and
 * view count the Shorts feed has no need for, and is never handed straight
 * to ExoPlayer — tapping one opens it in cobalt (resolve → pick → download),
 * it doesn't stream inline the way a Short does. Sharing one type would
 * force every Shorts source to invent channel/view-count fields it doesn't
 * have and every Home consumer to ignore the playback fields it doesn't
 * use.
 *
 * [videoId] is the raw YouTube video ID (11-char), so items can be
 * de-duplicated across the sources that produce them.
 */
data class FeedItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    /** Formatted for display, e.g. "1.2M views" — empty when the source
     * didn't provide one. Not a number: YouTube's own "views" string is
     * already locale-formatted and this app never needs to sort by it. */
    val viewCountText: String,
    /** Formatted, e.g. "3 days ago" — empty when unavailable. */
    val publishedText: String,
    val durationSeconds: Long,
    val watchUrl: String = "https://www.youtube.com/watch?v=$videoId",
    /**
     * Directly playable stream URL (progressive/HLS/DASH), resolved lazily
     * by the source via Innertube's ANDROID-client `player` call — the same
     * mechanism `shorts.source.InnertubeShortsSource` uses. Null until
     * resolved; an item with a null stream URL still renders (thumbnail,
     * title) and falls back to opening in cobalt rather than playing
     * inline, so a failed resolve degrades to the download path instead of
     * a dead tile.
     */
    val streamUrl: String? = null,
    val streamKind: StreamKind? = null,
    val source: FeedSourceType
)

/** How [FeedItem.streamUrl] should be interpreted by the player. Mirrors
 * `shorts.model.StreamKind`. */
enum class StreamKind { PROGRESSIVE, HLS, DASH }

/** Which backend produced a [FeedItem]. Mirrors `ShortsSourceType`. */
enum class FeedSourceType { INNERTUBE, CACHE }
