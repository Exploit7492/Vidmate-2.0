package com.cobalt.android.util

import android.content.Context

/**
 * Options for the home-screen download widget (Phase H7 follow-up). Kept in its
 * own prefs file rather than [SettingsRepository] because the widget provider
 * reads it from a broadcast receiver where the whole settings object would be
 * wasteful, and because these are presentation-only knobs.
 */
class WidgetSettings(context: Context) {

    private val prefs = context.getSharedPreferences("cobalt_widget", Context.MODE_PRIVATE)

    /** Show the "N downloading" line. */
    var showCount: Boolean
        get() = prefs.getBoolean("show_count", true)
        set(v) { prefs.edit().putBoolean("show_count", v).apply() }

    /** Show the top item's name and percentage. */
    var showProgress: Boolean
        get() = prefs.getBoolean("show_progress", true)
        set(v) { prefs.edit().putBoolean("show_progress", v).apply() }

    /** Compact mode drops the "Open Cobalt" action line. */
    var compact: Boolean
        get() = prefs.getBoolean("compact", false)
        set(v) { prefs.edit().putBoolean("compact", v).apply() }

    /**
     * Animated mode: the widget cycles a frame index on each refresh so the
     * leading glyph spins (RemoteViews cannot run an animation, so the provider
     * advances the frame itself on every update tick).
     */
    var animated: Boolean
        get() = prefs.getBoolean("animated", false)
        set(v) { prefs.edit().putBoolean("animated", v).apply() }

    /** Persisted animation frame, advanced by the provider when [animated]. */
    var frame: Int
        get() = prefs.getInt("frame", 0)
        set(v) { prefs.edit().putInt("frame", v % FRAMES).apply() }

    companion object {
        /** Number of spinner frames; the glyphs live in the presenter. */
        const val FRAMES = com.cobalt.android.widget.WidgetPresenter.FRAMES
    }
}
