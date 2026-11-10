package com.cobalt.android.ui.music

import com.cobalt.android.media.LocalMediaItem
import com.cobalt.android.media.MusicMetadata

/**
 * Groups local audio by album or artist for the "albums & artists" view.
 * Pure (no Android types beyond [LocalMediaItem], which is plain data) so the
 * grouping is unit-testable. Tracks with no tag fall into an "Unknown" group
 * rather than disappearing.
 */
object MusicGrouping {

    data class Group(val title: String, val subtitle: String, val tracks: List<LocalMediaItem>)

    fun byAlbum(
        tracks: List<LocalMediaItem>,
        tags: Map<Long, MusicMetadata.Tags>,
        idOf: (LocalMediaItem) -> Long = { MusicMetadata.idOf(it.uri) }
    ): List<Group> = group(tracks, tags, idOf) { it.album to it.artist }

    fun byArtist(
        tracks: List<LocalMediaItem>,
        tags: Map<Long, MusicMetadata.Tags>,
        idOf: (LocalMediaItem) -> Long = { MusicMetadata.idOf(it.uri) }
    ): List<Group> = group(tracks, tags, idOf) { it.artist to "" }

    private fun group(
        tracks: List<LocalMediaItem>,
        tags: Map<Long, MusicMetadata.Tags>,
        idOf: (LocalMediaItem) -> Long,
        key: (MusicMetadata.Tags) -> Pair<String, String>
    ): List<Group> {
        val buckets = LinkedHashMap<String, MutableList<LocalMediaItem>>()
        val subtitles = HashMap<String, String>()
        for (track in tracks) {
            val t = tags[idOf(track)] ?: MusicMetadata.Tags("", "")
            val (primary, secondary) = key(t)
            val name = primary.ifBlank { "Unknown" }
            buckets.getOrPut(name) { mutableListOf() } += track
            if (secondary.isNotBlank()) subtitles[name] = secondary
        }
        return buckets.entries
            .sortedBy { it.key.lowercase() }
            .map { (name, list) -> Group(name, subtitles[name].orEmpty(), list.sortedBy { it.displayName }) }
    }
}
