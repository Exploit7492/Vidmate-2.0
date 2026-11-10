package com.cobalt.android.recommend

import android.content.Context
import com.cobalt.android.download.DownloadDatabase
import com.cobalt.android.feed.explore.ExploreRepository
import com.cobalt.android.feed.model.FeedItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Builds a short, explained list of recommendations from what the user has
 * liked and watched, then hands them to [RecommendationNotifier].
 *
 * "Because you watched X" is derived locally: the seeds are the user's own
 * liked + history titles, the queries come from [TasteProfile], and the results
 * come from the same public InnerTube search the Explore screen uses. Nothing
 * is uploaded and no account is involved — the taste profile never leaves the
 * device.
 */
class RecommendationRepository(private val context: Context) {

    private val db = DownloadDatabase.getInstance(context)
    private val explore = ExploreRepository()
    private val notifier = RecommendationNotifier(context)

    data class Recommendation(
        val item: FeedItem,
        val reason: String
    )

    /**
     * Derives the taste profile and resolves up to [limit] fresh, unwatched
     * recommendations. Returns an empty list when there isn't enough signal
     * (fewer than two seeds) or nothing new came back.
     */
    suspend fun build(limit: Int = 3): List<Recommendation> = withContext(Dispatchers.IO) {
        val liked = db.likedDao().getRecent(SEED_LIMIT)
        val history = db.historyDao().getRecent(SEED_LIMIT)

        val likedTitles = liked.map { it.title }
        val historyTitles = history.map { it.title }

        if (likedTitles.size + historyTitles.size < 2) return@withContext emptyList()

        val channelByTitle = liked.associate { it.title to it.authorName }
        val queries = TasteProfile.derive(likedTitles, historyTitles, channelOf = { channelByTitle[it] })
        if (queries.isEmpty()) return@withContext emptyList()

        val seenIds = (liked.map { it.videoId } + db.historyDao().allRefIds()).toHashSet()

        val results = ArrayList<Recommendation>()
        for (query in queries) {
            if (results.size >= limit) break
            val page = explore.search(query) ?: continue
            for (item in page.items) {
                if (results.size >= limit) break
                if (item.videoId in seenIds) continue
                seenIds.add(item.videoId)
                results.add(Recommendation(item, TasteProfile.reason(query, likedTitles, historyTitles)))
            }
        }
        results
    }

    /** Builds recommendations and posts them as notifications. Returns how many. */
    suspend fun notifyIfAny(limit: Int = 3): Int {
        val recs = build(limit)
        recs.forEachIndexed { index, rec ->
            notifier.show(
                id = RecommendationNotifier.BASE_ID + index,
                title = "${rec.reason} — ${rec.item.title}",
                videoUrl = rec.item.watchUrl,
                thumbnailHint = rec.item.thumbnailUrl
            )
        }
        return recs.size
    }

    companion object {
        private const val SEED_LIMIT = 25
    }
}
