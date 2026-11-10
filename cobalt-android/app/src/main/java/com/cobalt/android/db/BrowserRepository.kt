package com.cobalt.android.db

import android.content.Context
import com.cobalt.android.db.entities.BrowserBookmarkEntity
import com.cobalt.android.db.entities.BrowserHistoryEntity
import com.cobalt.android.download.DownloadDatabase

/** In-app browser history + bookmarks (B6), backed by the shared database. */
class BrowserRepository(context: Context) {
    private val dao = DownloadDatabase.getInstance(context).browserDao()

    suspend fun recordVisit(url: String, title: String) =
        dao.insertHistory(BrowserHistoryEntity(url = url, title = title, visitedAt = System.currentTimeMillis()))

    suspend fun recentHistory(): List<BrowserHistoryEntity> = dao.recentHistory()

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun addBookmark(url: String, title: String) =
        dao.upsertBookmark(BrowserBookmarkEntity(url = url, title = title, savedAt = System.currentTimeMillis()))

    suspend fun removeBookmark(url: String) = dao.deleteBookmark(url)

    suspend fun bookmarks(): List<BrowserBookmarkEntity> = dao.bookmarks()

    suspend fun isBookmarked(url: String): Boolean = dao.isBookmarked(url) > 0
}
