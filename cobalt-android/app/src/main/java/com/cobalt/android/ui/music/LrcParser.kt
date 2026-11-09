package com.cobalt.android.ui.music

/**
 * Minimal LRC (lyrics) parser (Phase G2).
 *
 * Handles the format every player writes: one or more `[mm:ss.xx]` timestamps
 * followed by the line's text, plus the metadata tags (`[ar:]`, `[ti:]`, …)
 * which are ignored. Multiple timestamps on one line (`[00:10.00][01:20.00]x`)
 * are expanded to one entry each, which is standard and cheap to support.
 *
 * Pure and Android-free so it is unit tested; the caller decides where the
 * `.lrc` text comes from.
 */
object LrcParser {

    data class Line(val timeMs: Long, val text: String)

    private val TAG = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val META = Regex("""^\[[a-zA-Z]+:.*]$""")

    /** Parses [lrc] into timestamp-ordered, non-empty lines. */
    fun parse(lrc: String): List<Line> {
        val out = mutableListOf<Line>()
        for (raw in lrc.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || META.matches(line)) continue

            val matches = TAG.findAll(line).toList()
            if (matches.isEmpty()) continue
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isEmpty()) continue

            for (m in matches) {
                val minutes = m.groupValues[1].toLongOrNull() ?: continue
                val seconds = m.groupValues[2].toLongOrNull() ?: continue
                val frac = m.groupValues[3]
                val millis = when (frac.length) {
                    1 -> (frac.toLongOrNull() ?: 0) * 100
                    2 -> (frac.toLongOrNull() ?: 0) * 10
                    3 -> frac.toLongOrNull() ?: 0
                    else -> 0
                }
                out += Line(minutes * 60_000 + seconds * 1_000 + millis, text)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /**
     * The text to show at [positionMs], or null before the first line. Binary
     * search keeps this O(log n) since it is called on every position tick.
     */
    fun lineAt(lines: List<Line>, positionMs: Long): String? {
        if (lines.isEmpty() || positionMs < lines.first().timeMs) return null
        var lo = 0
        var hi = lines.lastIndex
        var found = 0
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (lines[mid].timeMs <= positionMs) {
                found = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return lines[found].text
    }
}
