package com.cobalt.android.feed

import android.content.Context
import android.util.Log
import com.cobalt.android.feed.db.FeedCacheDao
import com.cobalt.android.feed.db.FeedDatabase
import com.cobalt.android.feed.db.toCacheEntity
import com.cobalt.android.feed.db.toFeedItem
import com.cobalt.android.feed.model.FeedItem
import com.cobalt.android.feed.source.FeedSource
import com.cobalt.android.feed.source.InnertubeFeedSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * Home feed repository. Same structure as `shorts.ShortsFeedRepository`
 * (parallel per-source fetch with timeout + exponential backoff, de-dupe,
 * Room cache, cache fallback on total failure) so the two feeds behave
 * consistently — but currently ships a single live source
 * ([InnertubeFeedSource]). The list-based shape is kept so a second source
 * (e.g. an Invidious recommendations endpoint) can be added later without
 * changing callers.
 */
class FeedRepository(
    private val cacheDao: FeedCacheDao,
    private val sources: List<FeedSource>
) {
    constructor(context: Context) : this(
        cacheDao = FeedDatabase.getInstance(context).feedCacheDao(),
        sources = listOf(InnertubeFeedSource())
    )

    data class FeedPage(val items: List<FeedItem>, val isFromCache: Boolean)

    private data class SourceBackoff(val consecutiveFailures: Int, val retryAfterMillis: Long)
    private val backoff = HashMap<String, SourceBackoff>()

    suspend fun loadFeed(perSourceCount: Int = PER_SOURCE_PAGE_SIZE): FeedPage =
        withContext(Dispatchers.IO) {
            val perSourceResults = coroutineScope {
                sources.map { source ->
                    async { fetchFromSourceWithBackoff(source, perSourceCount) }
                }.awaitAll()
            }

            val deduped = dedupeById(perSourceResults.flatten())

            if (deduped.isNotEmpty()) {
                cacheDao.upsertAll(deduped.map { it.toCacheEntity() })
                cacheDao.evictStale(System.currentTimeMillis() - CACHE_TTL_MS)
                FeedPage(deduped, isFromCache = false)
            } else {
                FeedPage(
                    items = cacheDao.getRecent(perSourceCount * sources.size).map { it.toFeedItem() },
                    isFromCache = true
                )
            }
        }

    private suspend fun fetchFromSourceWithBackoff(source: FeedSource, count: Int): List<FeedItem> {
        val key = source.type.name
        val existing = backoff[key]
        if (existing != null && System.currentTimeMillis() < existing.retryAfterMillis) {
            Log.d(TAG, "Skipping $key, backing off for another " +
                "${existing.retryAfterMillis - System.currentTimeMillis()}ms")
            return emptyList()
        }

        return runCatching {
            withTimeoutOrNull(SOURCE_TIMEOUT_MS) { source.fetchFeed(count) }
        }.fold(
            onSuccess = { result ->
                if (result == null) {
                    Log.w(TAG, "Feed source $key timed out")
                    recordFailure(key)
                    emptyList()
                } else {
                    backoff.remove(key)
                    result
                }
            },
            onFailure = { e ->
                Log.w(TAG, "Feed source $key failed: ${e.message}")
                recordFailure(key)
                emptyList()
            }
        )
    }

    private fun recordFailure(key: String) {
        val failures = (backoff[key]?.consecutiveFailures ?: 0) + 1
        val delayMs = (BASE_BACKOFF_MS shl (failures - 1).coerceAtMost(MAX_BACKOFF_SHIFT))
            .coerceAtMost(MAX_BACKOFF_MS)
        backoff[key] = SourceBackoff(failures, System.currentTimeMillis() + delayMs)
        Log.w(TAG, "$key backing off for ${delayMs}ms ($failures consecutive failures)")
    }

    private fun dedupeById(items: List<FeedItem>): List<FeedItem> {
        val seen = LinkedHashMap<String, FeedItem>()
        for (item in items) seen.putIfAbsent(item.videoId, item)
        return seen.values.toList()
    }

    companion object {
        private const val TAG = "FeedRepository"
        private const val PER_SOURCE_PAGE_SIZE = 12
        private val CACHE_TTL_MS = TimeUnit.HOURS.toMillis(6)
        private val SOURCE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20)
        private const val BASE_BACKOFF_MS = 30_000L
        private const val MAX_BACKOFF_MS = 15 * 60_000L
        private const val MAX_BACKOFF_SHIFT = 8
    }
}
