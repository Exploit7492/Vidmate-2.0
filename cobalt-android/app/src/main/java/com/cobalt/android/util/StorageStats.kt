package com.cobalt.android.util

import android.os.StatFs
import java.io.File

/**
 * Storage figures for the Library/queue header and the file-detail sheet.
 * [formatBytes] is shared by every screen that shows a size so the units are
 * consistent (VidMate shows the same "1.2 GB"-style figures in its library).
 */
object StorageStats {

    /** Free bytes on the volume holding the app's external files dir. */
    fun freeBytes(dir: File): Long = runCatching { StatFs(dir.absolutePath).availableBytes }.getOrDefault(0L)

    /** Total bytes on that volume. */
    fun totalBytes(dir: File): Long = runCatching { StatFs(dir.absolutePath).totalBytes }.getOrDefault(0L)

    /** Human-readable size, e.g. "1.2 GB", "512 KB", "3 B". */
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return "%.1f %s".format(value, units[unit])
    }
}
