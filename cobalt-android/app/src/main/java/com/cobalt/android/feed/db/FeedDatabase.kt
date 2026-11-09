package com.cobalt.android.feed.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Cache for the Home feed, mirroring `shorts.db.ShortsDatabase`'s shape and
 * rationale (a separate DB from the download queue; caching a resolved feed
 * is a different domain). Kept separate from `ShortsDatabase` too, so the
 * two feeds can be evicted/rebuilt independently.
 */
@Database(entities = [FeedCacheEntity::class], version = 1, exportSchema = false)
abstract class FeedDatabase : RoomDatabase() {
    abstract fun feedCacheDao(): FeedCacheDao

    companion object {
        @Volatile private var INSTANCE: FeedDatabase? = null

        fun getInstance(context: Context): FeedDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FeedDatabase::class.java,
                    "cobalt_feed_cache.db"
                ).build().also { INSTANCE = it }
            }
    }
}
