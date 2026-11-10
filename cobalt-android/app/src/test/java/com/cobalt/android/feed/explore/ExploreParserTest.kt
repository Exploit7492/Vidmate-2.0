package com.cobalt.android.feed.explore

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JVM tests for the Innertube search/browse parser. */
class ExploreParserTest {

    @Test fun parsesFlatSearchVideoRenderer() {
        val json = JSONObject(
            """
            {"contents":{"sectionListRenderer":{"contents":[{"itemSectionRenderer":{"contents":[
              {"videoRenderer":{
                "videoId":"dQw4w9WgXcQ",
                "title":{"runs":[{"text":"Never Gonna Give You Up"}]},
                "ownerText":{"runs":[{"text":"Rick Astley"}]},
                "viewCountText":{"simpleText":"1.2B views"},
                "publishedTimeText":{"simpleText":"15 years ago"},
                "lengthText":{"simpleText":"3:33"},
                "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/dQw4w9WgXcQ/hq.jpg"}]}
              }}
            ]}}]}}}
            """.trimIndent()
        )
        val page = ExploreParser.parse(json)
        assertEquals(1, page.items.size)
        val item = page.items.first()
        assertEquals("dQw4w9WgXcQ", item.videoId)
        assertEquals("Never Gonna Give You Up", item.title)
        assertEquals("Rick Astley", item.channelName)
        assertEquals(213L, item.durationSeconds)
    }

    @Test fun parsesGridRichItemRenderer() {
        val json = JSONObject(
            """
            {"contents":{"richGridRenderer":{"contents":[
              {"richItemRenderer":{"content":{"videoRenderer":{
                "videoId":"abcdefghijk","title":{"simpleText":"Grid video"}
              }}}}
            ]}}}
            """.trimIndent()
        )
        val page = ExploreParser.parse(json)
        assertEquals(1, page.items.size)
        assertEquals("Grid video", page.items.first().title)
    }

    @Test fun dedupesRepeatedVideoIds() {
        val json = JSONObject(
            """
            {"a":{"videoRenderer":{"videoId":"aaaaaaaaaaa","title":{"simpleText":"one"}}},
             "b":{"videoRenderer":{"videoId":"aaaaaaaaaaa","title":{"simpleText":"one again"}}}}
            """.trimIndent()
        )
        assertEquals(1, ExploreParser.parse(json).items.size)
    }

    @Test fun ignoresNonElevenCharIds() {
        val json = JSONObject("""{"videoRenderer":{"videoId":"short","title":{"simpleText":"x"}}}""")
        assertEquals(0, ExploreParser.parse(json).items.size)
    }

    @Test fun readsContinuationToken() {
        val json = JSONObject(
            """
            {"x":{"continuationItemRenderer":{"continuationEndpoint":
              {"continuationCommand":{"token":"TOKEN123"}}}}}
            """.trimIndent()
        )
        assertEquals("TOKEN123", ExploreParser.continuationToken(json))
        assertNull(ExploreParser.continuationToken(JSONObject("{}")))
    }

    @Test fun readsEstimatedResults() {
        val json = JSONObject("""{"e":{"estimatedResults":{"simpleText":"About 1,200,000 results"}}}""")
        assertEquals("About 1,200,000 results", ExploreParser.estimatedResultText(json))
    }

    @Test fun durationHandlesHourFormat() {
        val json = JSONObject(
            """{"videoRenderer":{"videoId":"aaaaaaaaaaa","title":{"simpleText":"t"},"lengthText":{"simpleText":"1:02:03"}}}"""
        )
        assertEquals(3723L, ExploreParser.parse(json).items.first().durationSeconds)
    }

    @Test fun missingDurationIsMinusOne() {
        val json = JSONObject("""{"videoRenderer":{"videoId":"aaaaaaaaaaa","title":{"simpleText":"t"}}}""")
        assertEquals(-1L, ExploreParser.parse(json).items.first().durationSeconds)
    }
}
