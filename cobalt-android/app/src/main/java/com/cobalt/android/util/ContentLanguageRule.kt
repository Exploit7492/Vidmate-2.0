package com.cobalt.android.util

/**
 * VidMate's "language of content" rule: keep the feed/search results from
 * drifting across too many languages. Three modes, matching the reference:
 *
 *  - [ANY]    — no filtering (default).
 *  - [SINGLE] — only the preferred language.
 *  - [MAX2]   — the preferred language plus at most one other.
 *
 * Pure and Android-free so the decision is unit tested; callers pass in the
 * language tag they detected per item (from title script or the source's own
 * `lang` field).
 */
object ContentLanguageRule {

    const val ANY = "any"
    const val SINGLE = "single"
    const val MAX2 = "max2"

    /**
     * @param rule       one of [ANY]/[SINGLE]/[MAX2]
     * @param preferred  preferred ISO-639-1 tag, e.g. "en" (blank = no preference)
     * @param itemTags   detected language tag per item, in display order
     * @return true when the item should be kept
     */
    fun allows(rule: String, preferred: String, itemTags: List<String>): Boolean {
        if (rule == ANY || preferred.isBlank()) return true
        return when (rule) {
            SINGLE -> itemTags.all { it.startsWith(preferred, ignoreCase = true) }
            MAX2 -> {
                val langs = itemTags.map { it.substringBefore('-').lowercase() }.distinct()
                langs.size <= 2 && (preferred.lowercase() in langs || langs.isEmpty())
            }
            else -> true
        }
    }

    /** True when [itemTag] itself is allowed under the rule given the running set. */
    fun allowsItem(rule: String, preferred: String, itemTag: String, seen: Set<String>): Boolean {
        if (rule == ANY || preferred.isBlank()) return true
        val tag = itemTag.substringBefore('-').lowercase()
        return when (rule) {
            SINGLE -> tag == preferred.lowercase()
            MAX2 -> tag == preferred.lowercase() || seen.size < 2 || tag in seen
            else -> true
        }
    }
}
