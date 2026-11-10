package com.cobalt.android.feed.explore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JVM tests for pulling a playlist id out of a YouTube URL. */
class PlaylistIdTest {

    @Test fun extractsFromPlaylistUrl() =
        assertEquals("PLabc123_-X", ExploreRepository.playlistId("https://www.youtube.com/playlist?list=PLabc123_-X"))

    @Test fun extractsFromWatchUrlWithList() =
        assertEquals("PLxyz", ExploreRepository.playlistId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLxyz"))

    @Test fun returnsNullWhenNoListParam() =
        assertNull(ExploreRepository.playlistId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))

    @Test fun returnsNullForNonYoutube() =
        assertNull(ExploreRepository.playlistId("https://vimeo.com/12345"))
}
