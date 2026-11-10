package com.cobalt.android.util

/**
 * The Library's file-detail/info model. [sizeBytes] is the exact byte count;
 * [formattedSize], [extension] and [resolutionLabel] are derived so the detail
 * sheet needs no extra logic.
 */
data class FileDetail(
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val absolutePath: String,
    val durationSeconds: Long = 0,
    val width: Int = 0,
    val height: Int = 0
) {
    val formattedSize: String get() = StorageUsage.format(sizeBytes)

    val extension: String get() = displayName.substringAfterLast('.', "").lowercase()

    /** "1920×1080" when known, else blank. */
    val resolutionLabel: String
        get() = if (width > 0 && height > 0) "${width}×$height" else ""

    /** "3:07" / "1:02:03"; blank when unknown. */
    val formattedDuration: String
        get() = if (durationSeconds <= 0) "" else formatDuration(durationSeconds)

    companion object {
        fun formatDuration(totalSeconds: Long): String {
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
        }
    }
}
