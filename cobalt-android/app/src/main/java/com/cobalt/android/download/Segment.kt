package com.cobalt.android.download

import org.json.JSONArray
import org.json.JSONObject

/**
 * One byte-range part of a segmented download.
 *
 * This is the reimplementation of the model VidMate uses for its download
 * engine: a task is a list of parts, each carrying its own byte range, its own
 * URL (so a part can fail over to a different CDN host without restarting the
 * whole transfer), and its own already-written byte count (so an interrupted
 * part resumes from where it stopped rather than from its range start). See
 * `MTDownloadPart`'s `curPos[start-end+type+url+path]` serialisation in the
 * VidMate RE report for the reference design; the shape here is equivalent but
 * stored as JSON because this app already depends on `org.json` and a JSON
 * array is self-describing and trivially migratable, whereas VidMate's
 * bracket format needs a hand-written parser with no schema.
 *
 * Nothing is copied from VidMate (it is not open source) — this is the same
 * *design* reimplemented against OkHttp + MediaStore.
 */
data class Segment(
    /** Inclusive byte offset this part starts at. */
    val start: Long,
    /** Inclusive byte offset this part ends at (`totalBytes - 1` for the last part). */
    val end: Long,
    /** Bytes already written for this part; a resume re-requests `[start + written, end]`. */
    val written: Long = 0L,
    /**
     * The URL this part fetches from. Normally the original media URL, but the
     * engine swaps in an alternate host (see [SegmentList.alternateUrl]) after
     * a part exhausts its per-host retries — the "per-part URL fail-over" that
     * keeps one flaky CDN edge from failing the whole download.
     */
    val url: String,
    /**
     * Alternate URL tried if [url] fails. Populated from the origin's
     * `Location`/redirect or a caller-supplied mirror; blank means "no
     * alternate, retry the same URL".
     */
    val alternateUrl: String = ""
) {
    /** Bytes still to fetch for this part. */
    val remaining: Long get() = (end - start + 1) - written

    /** True once every byte of this part has been written. */
    val isComplete: Boolean get() = written >= (end - start + 1)

    fun toJson(): JSONObject = JSONObject().apply {
        put("start", start)
        put("end", end)
        put("written", written)
        put("url", url)
        put("alternateUrl", alternateUrl)
    }

    companion object {
        fun fromJson(o: JSONObject) = Segment(
            start = o.optLong("start"),
            end = o.optLong("end"),
            written = o.optLong("written"),
            url = o.optString("url"),
            alternateUrl = o.optString("alternateUrl")
        )
    }
}

/** Helpers for persisting and reasoning about a whole part list. */
object SegmentList {

    fun encode(segments: List<Segment>): String {
        if (segments.isEmpty()) return ""
        val arr = JSONArray()
        segments.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    fun decode(json: String?): List<Segment> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { Segment.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    /**
     * Splits a known-length file into at most [maxParts] contiguous ranges.
     * More parts means more parallel connections (faster on a high-latency
     * link) but more server load and more open sockets, so the engine caps it.
     */
    fun split(totalBytes: Long, maxParts: Int): List<Segment> {
        if (totalBytes <= 0 || maxParts <= 0) return emptyList()
        // Don't shred a small file into pieces smaller than a chunk worth
        // requesting — one part per ~4 MiB, capped at maxParts.
        val bySize = ((totalBytes + MIN_PART_BYTES - 1) / MIN_PART_BYTES).toInt()
        val parts = bySize.coerceIn(1, maxParts)
        if (parts == 1) return listOf(Segment(0, totalBytes - 1, 0, ""))

        val partSize = totalBytes / parts
        val out = ArrayList<Segment>(parts)
        var start = 0L
        for (i in 0 until parts) {
            val end = if (i == parts - 1) totalBytes - 1 else (start + partSize - 1)
            out += Segment(start = start, end = end, written = 0, url = "")
            start = end + 1
        }
        return out
    }

    fun totalWritten(segments: List<Segment>): Long = segments.sumOf { it.written }

    fun allComplete(segments: List<Segment>): Boolean =
        segments.isNotEmpty() && segments.all { it.isComplete }

    private const val MIN_PART_BYTES = 4L * 1024 * 1024
}
