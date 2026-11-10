package com.cobalt.android.widget

/**
 * Pure text logic for the home-screen widget, split out of the provider so it
 * is JVM-testable without an Android `RemoteViews`. The provider reads the DB
 * and prefs, then asks this for the two strings to paint.
 */
object WidgetPresenter {

    data class Text(val title: String, val body: String)

    /** Spinner glyphs indexed by frame; the provider advances the frame. */
    val SPINNER = listOf("◐", "◓", "◑", "◒", "◐", "◓", "◑", "◒")
    const val FRAMES = 8

    /**
     * [count] active downloads, [topName]/[topPct] describing the first one.
     * [glyph] is the already-chosen spinner frame ("" when not animated).
     */
    fun text(
        appName: String,
        count: Int,
        topName: String,
        topPct: Int,
        showCount: Boolean,
        showProgress: Boolean,
        glyph: String = ""
    ): Text {
        if (count <= 0) return Text(appName, "No active downloads")
        val title = if (showCount) "$glyph$count downloading" else appName
        val body = if (showProgress) "${topName.ifBlank { "download" }}\n$topPct%" else ""
        return Text(title, body)
    }
}
