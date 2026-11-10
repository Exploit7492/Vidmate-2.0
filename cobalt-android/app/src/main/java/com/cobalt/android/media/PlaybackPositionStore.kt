package com.cobalt.android.media

import android.content.Context

/**
 * Remembers where the user stopped watching each file so a later play resumes
 * there instead of restarting (VidMate keeps a per-file "continue watching"
 * position). Keyed by the media URI, value in milliseconds; a position is only
 * worth keeping once past [MIN_SAVE_MS] and is dropped near the end so a
 * finished file restarts cleanly.
 */
class PlaybackPositionStore(context: Context) {

    private val prefs = context.getSharedPreferences("cobalt_playback", Context.MODE_PRIVATE)

    fun positionMillis(uri: String): Long = prefs.getLong(key(uri), 0L)

    fun save(uri: String, positionMs: Long, durationMs: Long) {
        val editor = prefs.edit()
        if (positionMs < MIN_SAVE_MS || (durationMs > 0 && positionMs > durationMs - MIN_SAVE_MS)) {
            editor.remove(key(uri))
        } else {
            editor.putLong(key(uri), positionMs)
        }
        editor.apply()
    }

    fun clear(uri: String) = prefs.edit().remove(key(uri)).apply()

    private fun key(uri: String) = "pos_" + uri.hashCode()

    companion object {
        /** Ignore the first few seconds; below this it's not really "resume". */
        const val MIN_SAVE_MS = 10_000L
    }
}
