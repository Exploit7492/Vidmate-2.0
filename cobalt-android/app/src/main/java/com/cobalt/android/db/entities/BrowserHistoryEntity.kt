package com.cobalt.android.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One visited page in the in-app browser's history. */
@Entity(tableName = "browser_history")
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val visitedAt: Long
)
