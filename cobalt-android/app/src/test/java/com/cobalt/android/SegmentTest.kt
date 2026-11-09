package com.cobalt.android

import com.cobalt.android.download.Segment
import com.cobalt.android.download.SegmentList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the segmented-download part model — the logic that decides
 * how a file is split into ranges and whether a part is done. These run on the
 * JVM (no Android), which is why [Segment]/[SegmentList] hold no Android types.
 */
class SegmentTest {

    @Test fun smallFileStaysSinglePart() {
        // 2 MiB is under the 4 MiB minimum part size, so one part covering the
        // whole file.
        val segs = SegmentList.split(2L * 1024 * 1024, maxParts = 4)
        assertEquals(1, segs.size)
        assertEquals(0L, segs[0].start)
        assertEquals(2L * 1024 * 1024 - 1, segs[0].end)
    }

    @Test fun largeFileSplitsIntoCappedParts() {
        // 100 MiB with a cap of 4 → 4 parts, contiguous, last ends at size-1.
        val total = 100L * 1024 * 1024
        val segs = SegmentList.split(total, maxParts = 4)
        assertEquals(4, segs.size)
        assertEquals(0L, segs.first().start)
        assertEquals(total - 1, segs.last().end)
        // Contiguous with no gaps or overlaps.
        for (i in 1 until segs.size) {
            assertEquals(segs[i - 1].end + 1, segs[i].start)
        }
    }

    @Test fun partSizeCapRespected() {
        // 8 MiB → by size that's 2 parts; a cap of 1 must still yield 1.
        val segs = SegmentList.split(8L * 1024 * 1024, maxParts = 1)
        assertEquals(1, segs.size)
    }

    @Test fun resumeOffsetsAreHonoured() {
        // A part half-written is not complete; its remaining bytes are the tail.
        val s = Segment(start = 0, end = 99, written = 50, url = "https://x")
        assertFalse(s.isComplete)
        assertEquals(50L, s.remaining)
        val done = s.copy(written = 100)
        assertTrue(done.isComplete)
        assertEquals(0L, done.remaining)
    }

    @Test fun jsonRoundTripPreservesFailoverUrl() {
        val segs = listOf(
            Segment(0, 99, 40, "https://primary", "https://mirror"),
            Segment(100, 199, 0, "https://primary", "")
        )
        val decoded = SegmentList.decode(SegmentList.encode(segs))
        assertEquals(segs, decoded)
    }

    @Test fun emptyEncodeDecodesToEmptyList() {
        assertTrue(SegmentList.decode(SegmentList.encode(emptyList())).isEmpty())
        assertTrue(SegmentList.decode("").isEmpty())
        assertTrue(SegmentList.decode("not json").isEmpty())
    }

    @Test fun totalWrittenSumsParts() {
        val segs = listOf(
            Segment(0, 99, 30, "u"),
            Segment(100, 199, 70, "u")
        )
        assertEquals(100L, SegmentList.totalWritten(segs))
        assertFalse(SegmentList.allComplete(segs))
        assertTrue(SegmentList.allComplete(segs.map { it.copy(written = it.end - it.start + 1) }))
    }
}
