package com.cobalt.android.recommend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the taste-profile derivation behind "because you watched X". */
class TasteProfileTest {

    @Test fun keywordAffinityProducesTopicQueries() {
        val queries = TasteProfile.derive(
            likedTitles = listOf("Epic cooking recipe", "Easy cooking pasta"),
            historyTitles = listOf("Cooking basics")
        )
        assertTrue(queries.contains("cooking"))
    }

    @Test fun stopWordsAreExcluded() {
        val queries = TasteProfile.derive(
            likedTitles = listOf("The official full video"),
            historyTitles = listOf("A new official trailer")
        )
        assertFalse(queries.any { it in setOf("the", "official", "full", "video", "trailer", "new") })
    }

    @Test fun channelAffinityLeadsWhenRepeated() {
        val queries = TasteProfile.derive(
            likedTitles = listOf("Kurzgesagt space video", "Kurzgesagt black hole"),
            historyTitles = emptyList(),
            channelOf = { "Kurzgesagt" }
        )
        assertEquals("Kurzgesagt", queries.first())
    }

    @Test fun channelNeedsAtLeastTwoHits() {
        val queries = TasteProfile.derive(
            likedTitles = listOf("Some channel clip"),
            historyTitles = listOf("Another clip"),
            channelOf = { if (it.contains("Some")) "One-off" else null }
        )
        assertFalse(queries.contains("One-off"))
    }

    @Test fun respectsLimit() {
        val queries = TasteProfile.derive(
            likedTitles = listOf("gaming highlights football cricket"),
            historyTitles = listOf("gaming cricket football"),
            limit = 2
        )
        assertEquals(2, queries.size)
    }

    @Test fun emptySignalProducesNoQueries() {
        assertTrue(TasteProfile.derive(emptyList(), emptyList()).isEmpty())
    }

    @Test fun reasonNamesTheSourceTitle() {
        val reason = TasteProfile.reason(
            "cooking",
            likedTitles = listOf("Epic cooking recipe"),
            historyTitles = emptyList()
        )
        assertTrue(reason.contains("Epic cooking recipe"))
    }

    @Test fun reasonFallsBackWhenNoTitleMatches() {
        val reason = TasteProfile.reason("football", emptyList(), listOf("cooking recipe"))
        assertTrue(reason.contains("watching"))
    }
}
