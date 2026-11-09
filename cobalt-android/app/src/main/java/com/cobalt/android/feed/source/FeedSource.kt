package com.cobalt.android.feed.source

import com.cobalt.android.feed.model.FeedItem

/**
 * One backend capable of producing a page of [FeedItem]s for the Home feed.
 * Mirrors `shorts.source.ShortsSource`'s contract: each implementation is
 * independently failable — a source throwing must never empty the whole
 * feed, only shrink it for that refresh (see `FeedRepository`, which
 * catches per-source).
 */
interface FeedSource {
    val type: com.cobalt.android.feed.model.FeedSourceType

    /**
     * Returns up to [count] fresh items. Implementations should avoid
     * returning items already seen this session where practical, but the
     * repository also de-dupes by videoId as a backstop.
     */
    suspend fun fetchFeed(count: Int): List<FeedItem>
}
