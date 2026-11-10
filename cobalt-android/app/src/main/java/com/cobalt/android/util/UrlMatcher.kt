package com.cobalt.android.util

object UrlMatcher {
    /**
     * Hosts the app will hand to cobalt. cobalt does the actual extraction
     * server-side, so this list is really "what we will offer to download"
     * rather than "what we can parse locally" — adding a host is what turns a
     * paste into a working download. Matching is suffix-based
     * (see [isSupportedUrl]), so a bare apex like `"instagram.com"` also
     * covers `www.instagram.com`, `m.instagram.com`, etc., which is why most
     * entries are written without a `www.`.
     */
    private val SUPPORTED_HOSTS = setOf(
        // Big social / video platforms
        "youtube.com", "youtu.be", "youtube-nocookie.com",
        "tiktok.com", "vm.tiktok.com", "vt.tiktok.com",
        "twitter.com", "x.com",
        "instagram.com", "facebook.com", "fb.watch", "fb.com",
        "reddit.com", "redd.it",
        "soundcloud.com", "snd.sc",
        "vimeo.com", "twitch.tv", "clips.twitch.tv",
        "dailymotion.com", "dai.ly",
        "bilibili.com", "b23.tv",
        "pinterest.com", "pin.it",
        "tumblr.com",
        "snapchat.com",
        "linkedin.com",
        "vk.com", "vkvideo.ru",
        "ok.ru",
        "weibo.com", "weibo.cn",
        "douyin.com",
        "kuaishou.com",
        "xiaohongshu.com",
        "rumble.com",
        "odysee.com",
        "bitchute.com",
        "streamable.com",
        "imgur.com",
        "giphy.com",
        "9gag.com",
        "loom.com",
        "zoom.us",
        "ted.com",
        "bbc.co.uk", "bbc.com",
        "cnn.com",
        "espn.com",
        "nbc.com", "abc.net.au",
        "bandcamp.com",
        "mixcloud.com",
        "audiomack.com",
        "deezer.com",
        "tidal.com",
        "napster.com",
        "newgrounds.com",
        "archive.org",
        "coub.com",
        "nicovideo.jp",
        "acfun.cn",
        "youku.com",
        "iqiyi.com",
        "qq.com",
        "vimeo.com",
        "veoh.com",
        "metacafe.com",
        "break.com",
        "liveleak.com",
        "vidlii.com",
        "rutube.ru",
        "mail.ru",
        "kick.com",
        "odysee.tv"
    ).toSet()

    // Matches http(s):// followed by any non-whitespace characters
    private val URL_REGEX = Regex("""https?://\S+""")

    /**
     * True when [text] is a link the app will offer to download.
     *
     * By default this is deliberately **permissive**: any `http(s)` URL is
     * accepted, not just the [SUPPORTED_HOSTS] list. The list is a hint for
     * fast paths and for the few sites whose share text is worth pattern-
     * matching, but the extractor itself is a catch-all — cobalt resolves far
     * more than this list names, and rejecting an unknown host before the user
     * can even try is the one way to guarantee a "can't download this" that the
     * backend could actually have handled. Pass `strict = true` to require a
     * known host (used where a false positive would be costly, e.g. sniffing).
     */
    fun isSupportedUrl(text: String?, strict: Boolean = false): Boolean {
        if (text.isNullOrBlank()) return false
        val trimmed = text.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        return try {
            val url = java.net.URL(trimmed)
            val host = url.host?.lowercase() ?: return false
            if (!strict) return true
            // Built-in hosts, plus any fetched from remote config (Phase E7) so
            // the extractor list can grow without an app release.
            val extra = com.cobalt.android.remoteconfig.RemoteConfigStore.extraHosts()
            (SUPPORTED_HOSTS + extra).any { host == it || host.endsWith(".$it") }
        } catch (e: Exception) {
            false
        }
    }

    /** True when [host] is one of the curated hosts (or a remote-config extra). */
    fun isKnownHost(host: String): Boolean {
        val h = host.lowercase()
        val extra = com.cobalt.android.remoteconfig.RemoteConfigStore.extraHosts()
        return (SUPPORTED_HOSTS + extra).any { h == it || h.endsWith(".$it") }
    }

    /**
     * Extracts the first supported URL from [text], which may contain surrounding
     * prose (e.g. share text from social apps: "Check this out: https://youtu.be/abc").
     * Trailing punctuation characters are stripped before validation.
     */
    fun extractUrl(text: String?, strict: Boolean = false): String? {
        if (text.isNullOrBlank()) return null

        // Fast path: entire text is a bare URL
        val trimmed = text.trim()
        if (isSupportedUrl(trimmed, strict)) return trimmed

        // Slow path: scan for the first embedded URL
        for (match in URL_REGEX.findAll(text)) {
            // Strip trailing punctuation that sentence punctuation may have attached
            val candidate = match.value.trimEnd('.', ',', '!', '?', ')', ']', '}', '"', '\'', ';', ':')
            if (isSupportedUrl(candidate, strict)) return candidate
        }
        return null
    }
}
