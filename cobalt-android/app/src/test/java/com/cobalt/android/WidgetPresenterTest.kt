package com.cobalt.android

import com.cobalt.android.widget.WidgetPresenter
import org.junit.Assert.assertEquals
import org.junit.Test

/** JVM tests for the widget text logic (§9). */
class WidgetPresenterTest {

    @Test fun empty_shows_no_active() {
        val t = WidgetPresenter.text("Cobalt", 0, "", 0, true, true)
        assertEquals("Cobalt", t.title)
        assertEquals("No active downloads", t.body)
    }

    @Test fun count_and_progress() {
        val t = WidgetPresenter.text("Cobalt", 3, "movie.mp4", 42, true, true)
        assertEquals("3 downloading", t.title)
        assertEquals("movie.mp4\n42%", t.body)
    }

    @Test fun animated_prefixes_glyph() {
        val t = WidgetPresenter.text("Cobalt", 1, "a.mp4", 5, true, true, glyph = "◐ ")
        assertEquals("◐ 1 downloading", t.title)
    }

    @Test fun hiding_progress_blanks_body() {
        val t = WidgetPresenter.text("Cobalt", 2, "a.mp4", 5, true, false)
        assertEquals("", t.body)
    }

    @Test fun hiding_count_uses_app_name() {
        val t = WidgetPresenter.text("Cobalt", 2, "a.mp4", 5, false, true)
        assertEquals("Cobalt", t.title)
    }

    @Test fun blank_name_falls_back() {
        val t = WidgetPresenter.text("Cobalt", 1, "  ", 5, true, true)
        assertEquals("download\n5%", t.body)
    }
}
