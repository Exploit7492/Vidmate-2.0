package com.cobalt.android

import com.cobalt.android.util.ContentLanguageRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for the "language of content" rule (VidMate parity, §1). */
class ContentLanguageRuleTest {

    @Test fun any_always_allows() {
        assertTrue(ContentLanguageRule.allows("any", "en", listOf("fr", "de", "es")))
    }

    @Test fun blank_preference_disables_filtering() {
        assertTrue(ContentLanguageRule.allows("single", "", listOf("fr")))
    }

    @Test fun single_keeps_only_preferred() {
        assertTrue(ContentLanguageRule.allows("single", "en", listOf("en", "en-US")))
        assertFalse(ContentLanguageRule.allows("single", "en", listOf("en", "fr")))
    }

    @Test fun max2_allows_two_distinct_languages() {
        assertTrue(ContentLanguageRule.allows("max2", "en", listOf("en", "fr")))
        assertFalse(ContentLanguageRule.allows("max2", "en", listOf("en", "fr", "de")))
    }

    @Test fun max2_ignores_region_subtags() {
        assertTrue(ContentLanguageRule.allows("max2", "en", listOf("en-US", "en-GB")))
    }

    @Test fun allows_item_tracks_running_set() {
        val seen = mutableSetOf("en")
        assertTrue(ContentLanguageRule.allowsItem("max2", "en", "fr", seen))
        seen += "fr"
        assertFalse(ContentLanguageRule.allowsItem("max2", "en", "de", seen))
    }
}
