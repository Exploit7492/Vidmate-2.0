package com.cobalt.android.feed.source

import java.util.concurrent.atomic.AtomicInteger

/**
 * Rotating pool of evergreen search terms for the Home feed, the same
 * pattern `shorts.source.ShortsQueryFeeder` uses (and for the same reason:
 * neither NewPipeExtractor nor Invidious exposes a usable trending feed, so
 * search against a broad, rotating pool is what actually fills the list).
 *
 * Kept as a separate object from `ShortsQueryFeeder` because the two pools
 * have different intents — Home wants regular-length, broadly-interesting
 * video, Shorts wants short-form — and letting a user's custom Shorts pool
 * silently rewrite the Home feed (or vice-versa) would be surprising.
 */
object FeedQueryFeeder {

    private val DEFAULT_SEED_QUERIES = listOf(
        "documentary", "explained", "tech review", "science", "history",
        "cooking recipe", "travel vlog", "movie review", "gaming", "music video",
        "how to", "interviews", "nature", "space", "cars", "sports highlights",
        "finance explained", "productivity", "photography", "diy projects",
        "book review", "language learning", "workout", "news analysis",
        "art tutorial", "stand-up comedy", "podcast clip", "live performance"
    )

    @Volatile
    private var activeQueries: List<String> = DEFAULT_SEED_QUERIES

    private val cursor = AtomicInteger(0)

    /** Swaps the active pool; an empty list resets to the shipped default. */
    fun applyCustomQueries(queries: List<String>) {
        activeQueries = queries.ifEmpty { DEFAULT_SEED_QUERIES }
        cursor.set(0)
    }

    /** Returns the next [count] queries in rotation (wrapping around). */
    fun nextQueries(count: Int): List<String> {
        val pool = activeQueries
        val start = cursor.getAndAdd(count)
        return (0 until count).map { i -> pool[(start + i).mod(pool.size)] }
    }
}
