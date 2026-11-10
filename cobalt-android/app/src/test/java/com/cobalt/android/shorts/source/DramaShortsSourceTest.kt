package com.cobalt.android.shorts.source

import com.cobalt.android.shorts.model.ShortsSourceType
import com.cobalt.android.shorts.model.StreamKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DramaShortsSourceTest {

    @Test
    fun `m3u playlist yields titled HLS items`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://img.example/logo.png" group-title="Movies",24 Hour Free Movies
            https://cdn.example.com/stream/index.m3u8
            #EXTINF:-1,Something With No Logo
            http://stream.example.net/other/index.m3u8
        """.trimIndent()

        val items = DramaShortsSource.parseM3u(m3u, "movies")

        assertEquals(2, items.size)
        val first = items.first()
        assertEquals("24 Hour Free Movies", first.title)
        assertEquals("Movies", first.authorName)
        assertEquals("https://cdn.example.com/stream/index.m3u8", first.streamUrl)
        assertEquals("https://img.example/logo.png", first.thumbnailUrl)
        assertEquals(StreamKind.HLS, first.streamKind)
        assertEquals(ShortsSourceType.DRAMA, first.source)
    }

    @Test
    fun `m3u ignores non-media and header-only entries`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1,A channel whose URL is a web page
            https://example.com/watch/123
            #EXTINF:-1,No URL follows this
        """.trimIndent()

        assertTrue(DramaShortsSource.parseM3u(m3u, "series").isEmpty())
    }

    @Test
    fun `generic catalogue reads a bare array`() {
        val json = """
            [
              {"title":"Dared to Love the Nerd","cover":"https://cdn.example/1.jpg",
               "videoUrl":"https://cdn.example/1/master.m3u8"},
              {"name":"Wrong Jacket","poster":"https://cdn.example/2.jpg",
               "playUrl":"https://cdn.example/2/stream.mp4"}
            ]
        """.trimIndent()

        val items = DramaShortsSource.parseGenericCatalogue(json, "DramaShorts")

        assertEquals(2, items.size)
        assertEquals("Dared to Love the Nerd", items[0].title)
        assertEquals(StreamKind.HLS, items[0].streamKind)
        assertEquals("https://cdn.example/1.jpg", items[0].thumbnailUrl)
        assertEquals("Wrong Jacket", items[1].title)
        assertEquals(StreamKind.PROGRESSIVE, items[1].streamKind)
    }

    @Test
    fun `generic catalogue walks nested list wrappers`() {
        val json = """
            {"success":true,"data":{"list":[
              {"bookName":"Alpha Daddy","coverUrl":"https://cdn.example/a.jpg",
               "m3u8":"https://cdn.example/a/index.m3u8"}
            ]}}
        """.trimIndent()

        val items = DramaShortsSource.parseGenericCatalogue(json, "DramaBox")

        assertEquals(1, items.size)
        assertEquals("Alpha Daddy", items[0].title)
        assertEquals("DramaBox", items[0].authorName)
        assertEquals(StreamKind.HLS, items[0].streamKind)
    }

    @Test
    fun `generic catalogue drops cards with no usable stream`() {
        val json = """
            [{"title":"No Stream Here","cover":"https://cdn.example/x.jpg"},
             {"title":"Relative Path","videoUrl":"/media/1.m3u8"},
             {"title":"Real One","videoUrl":"https://cdn.example/ok.m3u8"}]
        """.trimIndent()

        val items = DramaShortsSource.parseGenericCatalogue(json, "Shortical")

        assertEquals(1, items.size)
        assertEquals("Real One", items[0].title)
    }

    @Test
    fun `generic catalogue tolerates html instead of json`() {
        val html = "<!DOCTYPE html><html><body>Cloudflare</body></html>"
        assertTrue(DramaShortsSource.parseGenericCatalogue(html, "FlickReels").isEmpty())
    }

    @Test
    fun `ids are stable for the same stream across refreshes`() {
        val json = """
            [{"title":"Same Title","videoUrl":"https://cdn.example/same.m3u8"}]
        """.trimIndent()

        val a = DramaShortsSource.parseGenericCatalogue(json, "X")
        val b = DramaShortsSource.parseGenericCatalogue(json, "X")

        assertEquals(a.first().videoId, b.first().videoId)
    }
}
