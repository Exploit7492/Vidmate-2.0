package com.cobalt.android

import com.cobalt.android.ui.music.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JVM tests for the LRC lyrics parser (G2). */
class LrcParserTest {

    @Test fun parsesTimedLinesAndSkipsMetadata() {
        val lrc = """
            [ar:Some Artist]
            [ti:Some Song]
            [00:01.00]first line
            [00:05.50]second line
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(2, lines.size)
        assertEquals(1000L, lines[0].timeMs)
        assertEquals("first line", lines[0].text)
        assertEquals(5500L, lines[1].timeMs)
    }

    @Test fun expandsMultipleTimestampsOnOneLine() {
        val lines = LrcParser.parse("[00:10.00][01:20.00]chorus")
        assertEquals(2, lines.size)
        assertEquals(10_000L, lines[0].timeMs)
        assertEquals(80_000L, lines[1].timeMs)
        assertEquals("chorus", lines[1].text)
    }

    @Test fun linesAreSortedByTime() {
        val lines = LrcParser.parse("[00:20.00]b\n[00:05.00]a")
        assertEquals(listOf("a", "b"), lines.map { it.text })
    }

    @Test fun lineAtBeforeFirstReturnsNull() {
        val lines = LrcParser.parse("[00:05.00]a\n[00:10.00]b")
        assertNull(LrcParser.lineAt(lines, 1000))
        assertEquals("a", LrcParser.lineAt(lines, 5_000))
        assertEquals("a", LrcParser.lineAt(lines, 9_999))
        assertEquals("b", LrcParser.lineAt(lines, 10_000))
        assertEquals("b", LrcParser.lineAt(lines, 99_999))
    }
}
