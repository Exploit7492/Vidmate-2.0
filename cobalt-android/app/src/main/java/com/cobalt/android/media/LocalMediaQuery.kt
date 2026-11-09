package com.cobalt.android.media

/**
 * Sorting, filtering and search over a scanned [LocalMediaItem] list
 * (Phase D3 sort/filter, D6 search).
 *
 * Deliberately a pure object with no Android dependency so every rule is unit
 * tested on the JVM. The Files tab composes these: pick a [Sort], a [KindFilter]
 * and a search string, then render the result.
 */
object LocalMediaQuery {

    enum class Sort { DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC, DURATION_DESC }

    /** Which kinds to include; [ALL] keeps both video and audio. */
    enum class KindFilter { ALL, VIDEO, AUDIO }

    fun apply(
        items: List<LocalMediaItem>,
        sort: Sort = Sort.DATE_DESC,
        filter: KindFilter = KindFilter.ALL,
        search: String = ""
    ): List<LocalMediaItem> {
        val filtered = items.asSequence()
            .filter { matchesKind(it, filter) }
            .filter { matchesSearch(it, search) }
        return when (sort) {
            Sort.DATE_DESC -> filtered.sortedByDescending { it.dateAddedSec }
            Sort.DATE_ASC -> filtered.sortedBy { it.dateAddedSec }
            Sort.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            Sort.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            Sort.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            Sort.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
            Sort.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
        }.toList()
    }

    private fun matchesKind(item: LocalMediaItem, filter: KindFilter): Boolean = when (filter) {
        KindFilter.ALL -> true
        KindFilter.VIDEO -> item.kind == LocalMediaRepository.Kind.VIDEO
        KindFilter.AUDIO -> item.kind == LocalMediaRepository.Kind.AUDIO
    }

    private fun matchesSearch(item: LocalMediaItem, search: String): Boolean {
        val q = search.trim()
        if (q.isEmpty()) return true
        return item.displayName.contains(q, ignoreCase = true)
    }
}
