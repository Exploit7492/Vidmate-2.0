package com.cobalt.android

import com.cobalt.android.feed.channel.ChannelParser
import com.cobalt.android.feed.channel.ChannelRepository
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the channel feed parser + URL detection (H5). */
class ChannelParserTest {

    @Test fun extractsBrowseIdFromChannelUrl() {
        assertEquals(
            "UC1234567890123456789012",
            ChannelParser.channelBrowseId("https://www.youtube.com/channel/UC1234567890123456789012")
        )
        assertNull(ChannelParser.channelBrowseId("https://www.youtube.com/@someone"))
    }

    @Test fun extractsHandle() {
        assertEquals("some.one-x", ChannelParser.channelHandle("https://youtube.com/@some.one-x/videos"))
    }

    @Test fun detectsChannelUrls() {
        assertTrue(ChannelRepository.isChannelUrl("https://www.youtube.com/channel/UC1234567890123456789012"))
        assertTrue(ChannelRepository.isChannelUrl("https://www.youtube.com/@handle"))
        assertFalse(ChannelRepository.isChannelUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test fun resolvesBrowseIdFromResolveUrlResponse() {
        val json = JSONObject(
            """{"endpoint":{"navigationEndpoint":{"browseEndpoint":{"browseId":"UCabcdefghijklmnopqrstuv"}}}}"""
        )
        assertEquals("UCabcdefghijklmnopqrstuv", ChannelParser.resolvedBrowseId(json))
    }

    @Test fun parsesGridDedupesAndReadsChannelName() {
        val json = JSONObject(
            """
            {"header":{"pageHeaderRenderer":{"pageTitle":{"simpleText":"My Channel"}}},
             "contents":[{"richItemRenderer":{"content":{"videoRenderer":{
                "videoId":"abcdefghijk",
                "title":{"runs":[{"text":"First Video"}]},
                "ownerText":{"runs":[{"text":"My Channel"}]},
                "viewCountText":{"simpleText":"1.2M views"},
                "lengthText":{"simpleText":"3:12"}}}}},
              {"richItemRenderer":{"content":{"videoRenderer":{
                "videoId":"abcdefghijk","title":{"simpleText":"dup"}}}}},
              {"continuationItemRenderer":{"continuationEndpoint":{
                "continuationCommand":{"token":"MORE"}}}}]}
            """.trimIndent()
        )
        val page = ChannelParser.parse(json)
        assertEquals("My Channel", page.channelName)
        assertEquals(1, page.videos.size)
        assertEquals("First Video", page.videos.first().title)
        assertEquals(192L, page.videos.first().durationSeconds)
        assertEquals("MORE", page.nextToken)
    }
}
