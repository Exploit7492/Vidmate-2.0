package com.cobalt.android.db.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cobalt.android.db.entities.HistoryEntity

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllLive(): LiveData<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clearAll()

    /** Most-recent history rows, for the recommendation engine (one-shot). */
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<HistoryEntity>

    @Query("SELECT refId FROM history")
    suspend fun allRefIds(): List<String>
}
