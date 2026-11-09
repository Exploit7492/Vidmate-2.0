package com.cobalt.android

import com.cobalt.android.media.LocalMediaItem
import com.cobalt.android.media.LocalMediaQuery
import com.cobalt.android.media.LocalMediaRepository
import org.junit.Assert.assertEquals
import org.junit.Test

/** JVM tests for the local media sort/filter/search rules (D3/D6). */
class LocalMediaQueryTest {

    private fun item(
        name: String,
        size: Long = 100,
        date: Long = 1,
        duration: Long = 0,
        kind: LocalMediaRepository.Kind = LocalMediaRepository.Kind.VIDEO
    ) = LocalMediaItem(
        uri = "content://media/$name",
        displayName = name,
        mimeType = "video/mp4",
        sizeBytes = size,
        durationMs = duration,
        dateAddedSec = date,
        kind = kind
    )

    private val sample = listOf(
        item("b.mp4", size = 200, date = 3, duration = 60),
        item("a.mp4", size = 100, date = 2, duration = 120),
        item("c.mp3", size = 300, date = 1, duration = 30, kind = LocalMediaRepository.Kind.AUDIO)
    )

    @Test fun sortByDateDescending() {
        val out = LocalMediaQuery.apply(sample, sort = LocalMediaQuery.Sort.DATE_DESC)
        assertEquals(listOf("b.mp4", "a.mp4", "c.mp3"), out.map { it.displayName })
    }

    @Test fun sortByNameAscending() {
        val out = LocalMediaQuery.apply(sample, sort = LocalMediaQuery.Sort.NAME_ASC)
        assertEquals(listOf("a.mp4", "b.mp4", "c.mp3"), out.map { it.displayName })
    }

    @Test fun sortBySizeDescending() {
        val out = LocalMediaQuery.apply(sample, sort = LocalMediaQuery.Sort.SIZE_DESC)
        assertEquals(listOf("c.mp3", "b.mp4", "a.mp4"), out.map { it.displayName })
    }

    @Test fun filterToAudioOnly() {
        val out = LocalMediaQuery.apply(sample, filter = LocalMediaQuery.KindFilter.AUDIO)
        assertEquals(listOf("c.mp3"), out.map { it.displayName })
    }

    @Test fun filterToVideoOnly() {
        val out = LocalMediaQuery.apply(sample, filter = LocalMediaQuery.KindFilter.VIDEO)
        assertEquals(listOf("b.mp4", "a.mp4"), out.map { it.displayName })
    }

    @Test fun searchMatchesCaseInsensitively() {
        val out = LocalMediaQuery.apply(sample, search = "A.MP4")
        assertEquals(listOf("a.mp4"), out.map { it.displayName })
    }

    @Test fun searchCombinesWithFilter() {
        val out = LocalMediaQuery.apply(
            sample, filter = LocalMediaQuery.KindFilter.VIDEO, search = "b"
        )
        assertEquals(listOf("b.mp4"), out.map { it.displayName })
    }

    @Test fun blankSearchReturnsEverything() {
        assertEquals(3, LocalMediaQuery.apply(sample, search = "   ").size)
    }
}
