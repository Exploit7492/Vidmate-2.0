package com.cobalt.android

import com.cobalt.android.feed.comments.CommentsParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JVM tests for the Innertube comments parser (E11). */
class CommentsParserTest {

    private fun tokenJson(token: String) = """
        {"continuationItemRenderer":{"continuationEndpoint":{
          "continuationCommand":{"token":"$token"}}}}
    """.trimIndent()

    @Test fun picksCommentsTokenFromCommentsHeaderSubtree() {
        val json = JSONObject(
            """
            {"engagementPanels":[
              {"relatedSection":{"content":${
                tokenJson("RELATED_TOKEN").replace("\n", "")
              }}},
              {"commentsSection":{"commentsHeaderRenderer":{},
                 "content":${tokenJson("COMMENTS_TOKEN").replace("\n", "")}}}
            ]}
            """.trimIndent()
        )
        assertEquals("COMMENTS_TOKEN", CommentsParser.commentContinuationToken(json))
    }

    @Test fun fallsBackToFirstTokenWhenNoCommentsSection() {
        val json = JSONObject("""{"a":${tokenJson("ONLY_TOKEN").replace("\n", "")}}""")
        assertEquals("ONLY_TOKEN", CommentsParser.commentContinuationToken(json))
    }

    @Test fun noTokenReturnsNull() {
        assertNull(CommentsParser.commentContinuationToken(JSONObject("""{"a":1}""")))
    }

    @Test fun parsesCommentsAndNextToken() {
        val json = JSONObject(
            """
            {"onResponseReceivedEndpoints":[{"items":[
              {"commentThreadRenderer":{"comment":{"commentEntityPayload":{
                 "properties":{"content":{"content":"hello world"},
                               "publishedTime":"2 days ago"},
                 "author":{"name":{"content":"Alice"}},
                 "toolbar":{"likeCountNotliked":{"content":"12"}}}}}},
              ${tokenJson("NEXT_TOKEN").replace("\n", "")}
            ]}]}
            """.trimIndent()
        )
        val page = CommentsParser.parseComments(json)
        assertEquals(1, page.comments.size)
        val c = page.comments.first()
        assertEquals("Alice", c.author)
        assertEquals("hello world", c.text)
        assertEquals("12", c.likeCount)
        assertEquals("2 days ago", c.publishedTime)
        assertEquals("NEXT_TOKEN", page.nextToken)
    }
}
