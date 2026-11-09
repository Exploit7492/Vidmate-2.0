package com.cobalt.android.music

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * A saved music playlist (Phase G4).
 *
 * Tracks are stored as newline-separated media URIs rather than a join table:
 * a playlist here is a private, user-created list of already-on-device tracks,
 * the whole list is always read and written together, and there is no query
 * that needs to look a track up across playlists. A normalised
 * playlist/track/playlist_track schema would be three tables and two joins for
 * data that is only ever consumed as one blob.
 */
@Entity(tableName = "music_playlists")
data class MusicPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val trackUris: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** The individual track URIs in this playlist, blanks dropped. */
    val uris: List<String>
        get() = trackUris.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
}

@Dao
interface MusicPlaylistDao {
    @Query("SELECT * FROM music_playlists ORDER BY createdAt DESC")
    suspend fun getAll(): List<MusicPlaylist>

    @Insert
    suspend fun insert(playlist: MusicPlaylist): Long

    @Query("DELETE FROM music_playlists WHERE id = :id")
    suspend fun deleteById(id: Long)

    companion object {
        /** Joins track URIs into the stored form. Inverse of [MusicPlaylist.uris]. */
        fun encode(uris: List<String>): String = uris.joinToString("\n")
    }
}
