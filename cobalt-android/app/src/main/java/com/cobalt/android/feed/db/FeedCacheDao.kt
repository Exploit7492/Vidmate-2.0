package com.cobalt.android.feed.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FeedCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FeedCacheEntity>)

    @Query("SELECT * FROM feed_cache ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<FeedCacheEntity>

    @Query("DELETE FROM feed_cache WHERE cachedAt < :olderThan")
    suspend fun evictStale(olderThan: Long)
}
