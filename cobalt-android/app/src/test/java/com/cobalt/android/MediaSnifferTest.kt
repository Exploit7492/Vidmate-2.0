package com.cobalt.android

import com.cobalt.android.ui.browser.MediaSniffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the in-app browser's media classification. */
class MediaSnifferTest {

    private val sniffer = MediaSniffer()

    @Test fun detectsCommonVideoExtensions() {
        assertTrue(sniffer.isMedia("https://cdn.example.com/v/clip.mp4", null))
        assertTrue(sniffer.isMedia("https://cdn.example.com/hls/index.m3u8", null))
        assertTrue(sniffer.isMedia("https://cdn.example.com/dash/manifest.mpd", null))
    }

    @Test fun ignoresQueryStringAfterExtension() {
        assertTrue(sniffer.isMedia("https://cdn.example.com/v.mp4?token=abc123&t=9", null))
    }

    @Test fun detectsByAcceptHeaderWhenNoExtension() {
        assertTrue(sniffer.isMedia("https://cdn.example.com/stream?id=9", "video/mp4"))
        assertTrue(sniffer.isMedia("https://cdn.example.com/manifest", "application/vnd.apple.mpegurl"))
    }

    @Test fun rejectsNonMedia() {
        assertFalse(sniffer.isMedia("https://example.com/page.html", "text/html"))
        assertFalse(sniffer.isMedia("https://example.com/logo.png", "image/png"))
        assertFalse(sniffer.isMedia("https://example.com/app.js", "application/javascript"))
    }

    @Test fun observeDeduplicatesAndRetains() {
        sniffer.observe("https://cdn.example.com/a.mp4", null)
        sniffer.observe("https://cdn.example.com/a.mp4", null)
        sniffer.observe("https://cdn.example.com/page.html", "text/html")
        sniffer.observe("https://cdn.example.com/b.m3u8", null)
        assertEquals(
            listOf("https://cdn.example.com/a.mp4", "https://cdn.example.com/b.m3u8"),
            sniffer.candidates()
        )
    }

    @Test fun clearEmptiesCandidates() {
        sniffer.observe("https://cdn.example.com/a.mp4", null)
        sniffer.clear()
        assertTrue(sniffer.candidates().isEmpty())
    }

    @Test fun capsCandidateCount() {
        val capped = MediaSniffer(maxCandidates = 3)
        repeat(10) { capped.observe("https://cdn.example.com/$it.mp4", null) }
        assertEquals(3, capped.candidates().size)
    }
}
