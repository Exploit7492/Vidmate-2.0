package com.cobalt.android.recommend

/**
 * Turns what the user has liked and watched into a small set of search queries
 * the recommendation engine can act on (the "because you watched X" engine).
 *
 * Pure and Android-free so the derivation is unit tested. The rules are
 * deliberately simple and explainable rather than a model:
 *
 *  - **Channel affinity first.** If the same channel name shows up in more
 *    than one liked/watched title's metadata it is the strongest signal, so a
 *    "more from <channel>" style query is emitted.
 *  - **Keyword affinity.** The most frequent meaningful words across titles
 *    become topic queries ("cooking", "highlights", …).
 *  - **Seed order is recency-weighted** — the caller passes most-recent-first,
 *    and earlier items contribute their words earlier, so the freshest
 *    interest tends to lead.
 *
 * A small stop-word list keeps filler ("the", "official", "full") out of the
 * queries. If nothing usable comes out, [derive] returns an empty list and the
 * caller simply doesn't notify.
 */
object TasteProfile {

    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with",
        "official", "video", "full", "hd", "4k", "live", "feat", "ft", "vs",
        "part", "ep", "episode", "trailer", "clip", "clips", "new", "best",
        "top", "how", "what", "why", "this", "that", "you", "your", "my", "is",
        "are", "be", "it", "its", "from", "by", "at", "as", "we", "i"
    )

    private val TOKEN_SPLIT = Regex("""[^a-z0-9]+""")

    /**
     * @param likedTitles   titles the user liked, most recent first
     * @param historyTitles titles the user watched, most recent first
     * @param channelOf     optional title → channel name resolver, so channel
     *                      affinity can be counted without a second data class
     * @return up to [limit] distinct queries, best first
     */
    fun derive(
        likedTitles: List<String>,
        historyTitles: List<String>,
        channelOf: (String) -> String? = { null },
        limit: Int = 5
    ): List<String> {
        val ordered = (likedTitles + historyTitles).distinct()

        val channelCounts = LinkedHashMap<String, Int>()
        for (title in ordered) {
            val channel = channelOf(title)?.trim().orEmpty()
            if (channel.isNotBlank()) channelCounts[channel] = (channelCounts[channel] ?: 0) + 1
        }
        val topChannel = channelCounts.maxByOrNull { it.value }?.takeIf { it.value >= 2 }?.key

        val wordCounts = LinkedHashMap<String, Int>()
        for (title in ordered) {
            for (token in tokenize(title)) wordCounts[token] = (wordCounts[token] ?: 0) + 1
        }
        val keywords = wordCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .map { it.key }

        val queries = LinkedHashSet<String>()
        topChannel?.let { queries.add(it) }
        keywords.forEach { if (queries.size < limit) queries.add(it) }
        return queries.take(limit)
    }

    /** The user-facing explanation line for a [query] ("Because you watched …"). */
    fun reason(query: String, likedTitles: List<String>, historyTitles: List<String>): String {
        val source = (likedTitles + historyTitles).firstOrNull {
            tokenize(it).contains(query.lowercase())
        }
        return if (source != null) "Because you watched \"$source\"" else "Because of what you've been watching"
    }

    private fun tokenize(title: String): List<String> =
        title.lowercase()
            .split(TOKEN_SPLIT)
            .filter { it.length >= 4 && it !in STOP_WORDS }
}
