package com.cobalt.android.feed.source

import com.cobalt.android.feed.model.StreamKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InnertubeFeedSourceTest {

    private val source = InnertubeFeedSource()

    @Test
    fun `parses simpleText video renderer`() {
        val items = source.parseBrowse(
            browseJson("dQw4w9WgXcQ", "Never Gonna Give You Up", "\"3:33\"")
        )
        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("dQw4w9WgXcQ", item.videoId)
        assertEquals("Never Gonna Give You Up", item.title)
        assertEquals(213L, item.durationSeconds) // 3:33
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", item.watchUrl)
    }

    @Test
    fun `parses runs-based title and owner`() {
        val json = """
        {"contents":{"a":{"videoRenderer":{
          "videoId":"abcdefghijk",
          "title":{"runs":[{"text":"Part "},{"text":"One"}]},
          "ownerText":{"runs":[{"text":"Some "},{"text":"Channel"}]},
          "lengthText":{"simpleText":"1:02:33"},
          "viewCountText":{"simpleText":"1,234,567 views"},
          "publishedTimeText":{"simpleText":"3 days ago"},
          "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/abcdefghijk/hq720.jpg"}]}
        }}}}
        """.trimIndent()
        val items = source.parseBrowse(json)
        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("Part One", item.title)
        assertEquals("Some Channel", item.channelName)
        assertEquals(1 * 3600 + 2 * 60 + 33L, item.durationSeconds)
        assertEquals("1,234,567 views", item.viewCountText)
        assertEquals("3 days ago", item.publishedText)
        assertEquals("https://i.ytimg.com/vi/abcdefghijk/hq720.jpg", item.thumbnailUrl)
    }

    @Test
    fun `parses duration formats`() {
        assertEquals(45L, source.parseBrowse(browseJson("vid0000000a", "t", "\"0:45\"")).first().durationSeconds)
        assertEquals(192L, source.parseBrowse(browseJson("vid0000000b", "t", "\"3:12\"")).first().durationSeconds)
        assertEquals(3753L, source.parseBrowse(browseJson("vid0000000c", "t", "\"1:02:33\"")).first().durationSeconds)
    }

    @Test
    fun `ignores renderers with a non-11-char videoId`() {
        assertTrue(source.parseBrowse(browseJson("tooShort", "t", "\"1:00\"")).isEmpty())
    }

    @Test
    fun `falls back to a thumbnail url when none is provided`() {
        val json = """{"x":{"videoRenderer":{"videoId":"noThumb0001","title":{"simpleText":"t"}}}}"""
        val item = source.parseBrowse(json).first()
        assertEquals("https://i.ytimg.com/vi/noThumb0001/hqdefault.jpg", item.thumbnailUrl)
    }

    @Test
    fun `blank or malformed input yields no items`() {
        assertTrue(source.parseBrowse(null).isEmpty())
        assertTrue(source.parseBrowse("").isEmpty())
        assertTrue(source.parseSearch("{}").isEmpty())
    }

    private fun browseJson(videoId: String, title: String, lengthSimple: String) = """
    {"contents":{"a":{"videoRenderer":{
      "videoId":"$videoId",
      "title":{"simpleText":"$title"},
      "ownerText":{"simpleText":"Channel"},
      "lengthText":{"simpleText":$lengthSimple}
    }}}}
    """.trimIndent()
}

class InnertubeStreamResolverTest {

    private val resolver = InnertubeStreamResolver()

    @Test
    fun `picks the highest-bitrate progressive format`() {
        val json = """
        {
          "playabilityStatus":{"status":"OK"},
          "streamingData":{"formats":[
            {"url":"https://x/low.mp4","bitrate":100000},
            {"url":"https://x/high.mp4","bitrate":900000},
            {"url":"https://x/mid.mp4","bitrate":400000}
          ]}
        }
        """.trimIndent()
        val resolved = resolver.parsePlayerResponse(json)
        assertEquals("https://x/high.mp4", resolved?.url)
        assertEquals(StreamKind.PROGRESSIVE, resolved?.kind)
    }

    @Test
    fun `falls back to HLS manifest when no progressive format`() {
        val json = """
        {"playabilityStatus":{"status":"OK"},
         "streamingData":{"formats":[],"hlsManifestUrl":"https://x/master.m3u8"}}
        """.trimIndent()
        val resolved = resolver.parsePlayerResponse(json)
        assertEquals("https://x/master.m3u8", resolved?.url)
        assertEquals(StreamKind.HLS, resolved?.kind)
    }

    @Test
    fun `returns null for a non-OK playability status`() {
        val json = """{"playabilityStatus":{"status":"LOGIN_REQUIRED"}}"""
        assertNull(resolver.parsePlayerResponse(json))
    }

    @Test
    fun `returns null when there are no playable streams`() {
        assertNull(resolver.parsePlayerResponse("""{"playabilityStatus":{"status":"OK"}}"""))
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(resolver.parsePlayerResponse(""))
    }
}
