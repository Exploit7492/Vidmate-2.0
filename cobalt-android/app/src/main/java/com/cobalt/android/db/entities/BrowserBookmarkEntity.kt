package com.cobalt.android.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved bookmark in the in-app browser. URL is the natural key. */
@Entity(tableName = "browser_bookmarks")
data class BrowserBookmarkEntity(
    @PrimaryKey val url: String,
    val title: String,
    val savedAt: Long
)
