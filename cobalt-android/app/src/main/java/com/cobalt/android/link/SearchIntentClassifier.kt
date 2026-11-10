package com.cobalt.android.link

/**
 * Decides what the header's single search field should do with whatever the
 * user put in it. Pure and Android-free so the routing rules are unit
 * testable; the caller performs the action.
 *
 * The rules, in order:
 *  - an empty/blank string is [Action.Idle] (show hints, don't act);
 *  - a link the downloader knows how to resolve (or any http(s) link, which
 *    the extractor is a catch-all for) is [Action.Download];
 *  - any other http(s) link is [Action.Browse] — the in-app browser sniffs
 *    the media off the page instead;
 *  - anything else is free text → [Action.Search].
 */
object SearchIntentClassifier {

    sealed class Action {
        /** Nothing actionable yet. */
        object Idle : Action()

        /** Resolve and offer quality options, then download. */
        data class Download(val url: String) : Action()

        /** Open in the in-app browser (media sniffing there can still download). */
        data class Browse(val url: String) : Action()

        /** Free-text search against the feed/search source. */
        data class Search(val query: String) : Action()
    }

    fun classify(raw: String?, strict: Boolean = false): Action {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return Action.Idle
        val url = com.cobalt.android.util.UrlMatcher.extractUrl(text, strict = strict)
        if (url != null) {
            return if (com.cobalt.android.util.UrlMatcher.isSupportedUrl(url, strict = strict)) {
                Action.Download(url)
            } else {
                Action.Browse(url)
            }
        }
        if (looksLikeBareUrl(text)) return Action.Browse(text)
        return Action.Search(text)
    }

    /**
     * True for something the user clearly meant as a link (starts with a
     * scheme, or a `host.tld` with no spaces) even when [UrlMatcher] wouldn't
     * accept it verbatim — a bare `example.com/path` pasted without a scheme.
     */
    private fun looksLikeBareUrl(text: String): Boolean {
        if (text.any { it.isWhitespace() }) return false
        if (text.startsWith("http://") || text.startsWith("https://")) return true
        val host = text.substringBefore('/').substringBefore('?')
        val dot = host.lastIndexOf('.')
        if (dot <= 0 || dot == host.length - 1) return false
        val tld = host.substring(dot + 1)
        return tld.length in 2..24 && tld.all { it.isLetter() }
    }

    /** Normalises a bare host into a real URL so Browse/Search never gets a scheme-less string. */
    fun asUrl(text: String): String =
        if (text.startsWith("http://") || text.startsWith("https://")) text else "https://$text"
}
