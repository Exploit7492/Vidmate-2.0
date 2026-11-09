package com.cobalt.android.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans on-device media through `MediaStore` (Phase D4 video / D5 audio).
 *
 * Queries `MediaStore.Files` rather than the `Video`/`Audio` collections
 * specifically, because the app's own completed downloads land in the
 * **Downloads** collection (`MediaStoreWriter`), which the Video/Audio
 * collections do not always expose — querying `Files` with a `media_type`
 * filter is the one query that returns everything playable on the device,
 * wherever it lives, including this app's downloads.
 *
 * Returns plain [LocalMediaItem]s so the sort/filter/search logic (D3/D6) can
 * be unit tested on the JVM; the Android query is the only part that needs a
 * device.
 */
class LocalMediaRepository(context: Context) {

    private val appContext = context.applicationContext

    enum class Kind { VIDEO, AUDIO }

    /** Loads all video + audio the device reports. Call from a background thread. */
    suspend fun load(): List<LocalMediaItem> = withContext(Dispatchers.IO) {
        (queryKind(Kind.VIDEO) + queryKind(Kind.AUDIO))
    }

    private fun queryKind(kind: Kind): List<LocalMediaItem> {
        val mediaType = if (kind == Kind.VIDEO) MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        else MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val out = ArrayList<LocalMediaItem>()
        runCatching {
            appContext.contentResolver.query(
                collection, projection, selection, arrayOf(mediaType.toString()), null
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val pathCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    out += LocalMediaItem(
                        uri = uri.toString(),
                        displayName = c.getString(nameCol) ?: "(unnamed)",
                        mimeType = c.getString(mimeCol) ?: "application/octet-stream",
                        sizeBytes = c.getLong(sizeCol),
                        durationMs = c.getLong(durCol),
                        dateAddedSec = c.getLong(dateCol),
                        relativePath = c.getString(pathCol) ?: "",
                        kind = kind
                    )
                }
            }
        }
        return out
    }
}

/** One playable item found on the device. Plain data — no Android types beyond
 *  the URI string — so sorting/filtering is JVM-testable. */
data class LocalMediaItem(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
    val relativePath: String = "",
    val kind: LocalMediaRepository.Kind
)
