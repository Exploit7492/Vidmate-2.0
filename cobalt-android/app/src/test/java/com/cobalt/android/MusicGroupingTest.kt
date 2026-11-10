package com.cobalt.android

import com.cobalt.android.media.LocalMediaItem
import com.cobalt.android.media.LocalMediaRepository
import com.cobalt.android.media.MusicMetadata
import com.cobalt.android.ui.music.MusicGrouping
import org.junit.Assert.assertEquals
import org.junit.Test

/** JVM tests for album/artist grouping (§8). */
class MusicGroupingTest {

    private fun track(name: String, id: Long) = LocalMediaItem(
        uri = "content://media/audio/$id",
        displayName = name,
        mimeType = "audio/mpeg",
        sizeBytes = 1,
        durationMs = 1,
        dateAddedSec = 1,
        kind = LocalMediaRepository.Kind.AUDIO
    )

    private val a = track("a.mp3", 1)
    private val b = track("b.mp3", 2)
    private val c = track("c.mp3", 3)

    @Test fun groups_by_album_sorted() {
        val tags = mapOf(
            1L to MusicMetadata.Tags("Artist1", "Zeta"),
            2L to MusicMetadata.Tags("Artist2", "Alpha"),
            3L to MusicMetadata.Tags("Artist1", "Zeta")
        )
        val groups = MusicGrouping.byAlbum(listOf(a, b, c), tags) { it.uri.substringAfterLast('/').toLong() }
        assertEquals(listOf("Alpha", "Zeta"), groups.map { it.title })
        assertEquals(listOf(a, c), groups.first { it.title == "Zeta" }.tracks)
    }

    @Test fun missing_tags_fall_into_unknown() {
        val groups = MusicGrouping.byAlbum(listOf(a), emptyMap()) { it.uri.substringAfterLast('/').toLong() }
        assertEquals("Unknown", groups.single().title)
    }

    @Test fun groups_by_artist() {
        val tags = mapOf(
            1L to MusicMetadata.Tags("Ann", "X"),
            2L to MusicMetadata.Tags("Bob", "Y")
        )
        val groups = MusicGrouping.byArtist(listOf(a, b), tags) { it.uri.substringAfterLast('/').toLong() }
        assertEquals(listOf("Ann", "Bob"), groups.map { it.title })
    }
}
