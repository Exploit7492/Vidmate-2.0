package com.cobalt.android.status

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.cobalt.android.download.MediaStoreWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * WhatsApp status saver (Phase E8).
 *
 * Scans the well-known on-device directories WhatsApp keeps viewed statuses in
 * and lets the user copy one into their own Downloads collection. Copying (not
 * moving) is deliberate — the status file belongs to WhatsApp, and a saver that
 * deletes the original would corrupt the other app's state.
 *
 * Scope, stated honestly: this works on Android 10 and below, where the status
 * directory is readable with storage permission. From Android 11 the status
 * folder sits inside WhatsApp's app-scoped storage and is **not** readable by
 * another app at all — no permission grants it. So on Android 11+ the scan
 * simply finds nothing and the screen says so, rather than offering a button
 * that could never work. Rooted devices and WhatsApp Business are handled by
 * including their directory variants in [candidateDirs].
 */
class StatusRepository(private val context: Context) {

    data class StatusFile(val file: File, val isVideo: Boolean, val name: String)

    /** All status directories we know how to look in, existing ones only. */
    fun candidateDirs(): List<File> {
        val root = Environment.getExternalStorageDirectory()
        val paths = listOf(
            "WhatsApp/Media/.Statuses",
            "WhatsApp Business/Media/.Statuses",
            "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
        )
        return paths.map { File(root, it) }.filter { it.isDirectory }
    }

    /** Lists media in every readable status directory, newest first. */
    suspend fun scan(): List<StatusFile> = withContext(Dispatchers.IO) {
        candidateDirs()
            .flatMap { dir ->
                (dir.listFiles() ?: emptyArray()).mapNotNull { f ->
                    val lower = f.name.lowercase()
                    when {
                        !f.isFile -> null
                        lower.endsWith(".mp4") -> StatusFile(f, isVideo = true, name = f.name)
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ->
                            StatusFile(f, isVideo = false, name = f.name)
                        else -> null
                    }
                }
            }
            .sortedByDescending { it.file.lastModified() }
    }

    /**
     * Copies [status] into the user's Downloads collection via
     * [MediaStoreWriter], so it shows up in the gallery and in Cobalt's own
     * library like any other saved file. Returns the new URI, or null on
     * failure.
     */
    suspend fun save(status: StatusFile): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = if (status.isVideo) "video/mp4" else "image/jpeg"
            val writer = MediaStoreWriter(context)
            val opened = writer.open(status.name, mime) ?: return@runCatching null
            opened.stream.use { out -> status.file.inputStream().use { it.copyTo(out) } }
            writer.finalize(opened.uri)
            opened.uri
        }.getOrNull()
    }
}
