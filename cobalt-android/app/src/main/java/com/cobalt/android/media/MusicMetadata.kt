package com.cobalt.android.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Album/artist tags for local audio, which [LocalMediaRepository] does not
 * carry (it scans `MediaStore.Files`, whose common columns stop at name/size).
 * VidMate groups the music tab by album and artist, so this pulls the extra
 * columns from the audio collection and keys them by the track's MediaStore id.
 */
object MusicMetadata {

    data class Tags(val artist: String, val album: String)

    /** id (from the track's `content://` URI) -> tags. Empty map on any failure. */
    suspend fun load(context: Context): Map<Long, Tags> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM
        )
        val out = HashMap<Long, Tags>()
        runCatching {
            context.contentResolver.query(
                collection, projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                while (c.moveToNext()) {
                    out[c.getLong(idCol)] = Tags(
                        artist = c.getString(artistCol) ?: "",
                        album = c.getString(albumCol) ?: ""
                    )
                }
            }
        }
        out
    }

    /** The MediaStore id inside a `content://media/.../<id>` track URI, or -1. */
    fun idOf(uri: String): Long =
        runCatching { ContentUris.parseId(android.net.Uri.parse(uri)) }.getOrDefault(-1L)
}
