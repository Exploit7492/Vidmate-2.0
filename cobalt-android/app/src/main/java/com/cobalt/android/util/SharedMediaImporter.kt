package com.cobalt.android.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies a `content://` URI the user shared into us (a video or image from
 * another app's share sheet) into the device's Downloads collection, so the
 * share target is useful for media files and not only for links.
 *
 * Returns the public `content://` URI of the copy (MediaStore on API 29+,
 * the app's external Downloads dir below that), or null on failure. The
 * caller posts the completion notification.
 */
object SharedMediaImporter {

    suspend fun import(context: Context, source: Uri, mimeType: String): Uri? =
        withContext(Dispatchers.IO) {
            val name = displayName(context, source) ?: "shared_${System.currentTimeMillis()}"
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    copyToMediaStore(context, source, name, mimeType)
                } else {
                    copyToExternalFiles(context, source, name)
                }
            }.getOrNull()
        }

    private fun displayName(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()

    private fun copyToMediaStore(
        context: Context, source: Uri, name: String, mimeType: String
    ): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(target).use { out ->
            if (out == null) return null
            resolver.openInputStream(source).use { input ->
                if (input == null) return null
                input.copyTo(out)
            }
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(target, values, null, null)
        return target
    }

    private fun copyToExternalFiles(context: Context, source: Uri, name: String): Uri? {
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            ""
        ).apply { mkdirs() }
        val out = File(dir, name)
        context.contentResolver.openInputStream(source).use { input ->
            if (input == null) return null
            out.outputStream().use { input.copyTo(it) }
        }
        return Uri.fromFile(out)
    }
}
