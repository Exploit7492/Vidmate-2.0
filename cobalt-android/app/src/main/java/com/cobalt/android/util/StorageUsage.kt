package com.cobalt.android.util

/**
 * Human-readable storage totals for the Library's storage-usage display.
 * Pure so the formatting is unit tested without touching the filesystem.
 */
object StorageUsage {

    /** 1536 -> "1.5 KB", 0 -> "0 B". Binary units (1024-based), 1 decimal. */
    fun format(bytes: Long): String {
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

    /** "12 files · 340.2 MB" — the Library header line. */
    fun summary(count: Int, bytes: Long): String =
        "$count file${if (count == 1) "" else "s"} · ${format(bytes)}"
}
