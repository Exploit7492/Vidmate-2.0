package com.cobalt.android.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cobalt.android.db.entities.BrowserBookmarkEntity
import com.cobalt.android.db.entities.BrowserHistoryEntity

@Dao
interface BrowserDao {

    @Insert
    suspend fun insertHistory(entity: BrowserHistoryEntity)

    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT 200")
    suspend fun recentHistory(): List<BrowserHistoryEntity>

    @Query("DELETE FROM browser_history")
    suspend fun clearHistory()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(entity: BrowserBookmarkEntity)

    @Query("DELETE FROM browser_bookmarks WHERE url = :url")
    suspend fun deleteBookmark(url: String)

    @Query("SELECT * FROM browser_bookmarks ORDER BY savedAt DESC")
    suspend fun bookmarks(): List<BrowserBookmarkEntity>

    @Query("SELECT COUNT(*) FROM browser_bookmarks WHERE url = :url")
    suspend fun isBookmarked(url: String): Int
}
