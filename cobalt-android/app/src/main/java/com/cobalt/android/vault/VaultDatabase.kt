package com.cobalt.android.vault

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.lifecycle.LiveData

/**
 * A vaulted file: media that has been moved out of public storage into the
 * app-private vault directory (Phase D7).
 *
 * The bytes live at `filesDir/vault/<id>`; this row is the index. Nothing here
 * is encrypted at rest (the vault's protection is that the directory is private
 * to the app and the screen is passcode-gated) — the design deliberately does
 * not claim encryption it does not do.
 */
@Entity(tableName = "vault_entries")
data class VaultEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String = "",
    val mimeType: String = "application/octet-stream",
    val sizeBytes: Long = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface VaultDao {
    @Insert
    suspend fun insert(entry: VaultEntry): Long

    @Query("SELECT * FROM vault_entries ORDER BY addedAt DESC")
    fun getAllLive(): LiveData<List<VaultEntry>>

    @Query("SELECT * FROM vault_entries WHERE id = :id")
    suspend fun getById(id: Long): VaultEntry?

    @Query("DELETE FROM vault_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(entities = [VaultEntry::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile private var instance: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault.db"
                ).build().also { instance = it }
            }
    }
}
