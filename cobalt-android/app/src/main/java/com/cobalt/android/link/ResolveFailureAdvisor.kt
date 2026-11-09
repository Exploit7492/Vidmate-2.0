package com.cobalt.android.link

/**
 * Turns a raw resolver error into user guidance (Phase E5).
 *
 * The most common non-obvious failure for a downloader is a geo-restriction:
 * the cobalt instance — or the site itself — refuses because of the request's
 * region, surfacing as a 403/"forbidden"/"not available in your country". A
 * plain error string leaves the user stuck; this recognises the pattern and
 * suggests the actual remedy (retry through a VPN / different instance).
 *
 * Pure and Android-free so the mapping is unit tested.
 */
object ResolveFailureAdvisor {

    private val GEO_MARKERS = listOf(
        "geo", "region", "country", "not available in", "forbidden",
        "403", "blocked in your", "unavailable in your"
    )

    private val RATE_MARKERS = listOf("rate-limit", "rate limit", "too many requests", "429")

    /**
     * Returns [message] with an actionable hint appended when it matches a
     * known failure class, otherwise [message] unchanged.
     */
    fun advise(message: String): String {
        val lower = message.lowercase()
        return when {
            RATE_MARKERS.any { lower.contains(it) } ->
                "$message\nThe shared instance is busy — wait a moment and retry, " +
                    "or set your own cobalt instance in Settings."
            GEO_MARKERS.any { lower.contains(it) } ->
                "$message\nThis looks region-locked. Try again on a VPN, or point the app " +
                    "at a cobalt instance in another region (Settings)."
            else -> message
        }
    }
}
