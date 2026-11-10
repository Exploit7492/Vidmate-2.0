package com.cobalt.android.shorts.source

import android.util.Log
import com.cobalt.android.shorts.model.ShortItem
import com.cobalt.android.shorts.model.ShortsSourceType
import com.cobalt.android.shorts.model.StreamKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Aggregated short-drama rail: every public catalogue this app can reach
 * without an API key, merged into one feed.
 *
 * Providers, and why they are (or are not) expected to contribute at runtime:
 *
 *  1. **Dailymotion** — the public, key-free `api.dailymotion.com` search plus
 *     the player-metadata endpoint, which returns a real HLS master playlist.
 *     This is the rail's primary source: a title, a cover and a playable
 *     `.m3u8` per item, no token required.
 *  2. **iptv-org** — the public category playlists (`movies`, `series`,
 *     `entertainment`). Key-free and stable; every entry already carries a
 *     title plus a `.m3u8`. Acts as the floor so the rail is never empty.
 *  3. **DramaBox** — the `reelapi/DramaBox` community frontend. Its upstream
 *     API needs a bearer token that is not published, so this is best-effort:
 *     it contributes when a deployment happens to be open and no-ops otherwise.
 *  4. **DramaShorts / Shortical / FlickReels / shorts-drama.com** — their
 *     catalogue data is served behind a bot wall or an authenticated API, so
 *     these are registered best-effort too. Each is a single independent
 *     attempt that fails closed, and the moment one opens up it contributes
 *     with no code change.
 *
 * A provider only yields an item when it has a usable media URL — a catalogue
 * card with no stream can be neither played nor downloaded, so it is dropped
 * rather than shown as a dead tile.
 */
class DramaShortsSource : ShortsSource {

    override val type: ShortsSourceType = ShortsSourceType.DRAMA

    override suspend fun fetchShorts(count: Int): List<ShortItem> = withContext(Dispatchers.IO) {
        val perProvider = (count / PROVIDERS.size).coerceAtLeast(4)
        val batches = coroutineScope {
            PROVIDERS.map { provider ->
                async {
                    // A provider that stalls (a bot wall holding the socket open,
                    // a slow CDN) must not hold up the whole refresh: each gets a
                    // hard ceiling and contributes nothing if it exceeds it.
                    withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                        runCatching { provider.fetch(perProvider) }
                            .onFailure { Log.w(TAG, "drama provider ${provider.name} failed", it) }
                            .getOrDefault(emptyList())
                    } ?: emptyList()
                }
            }.awaitAll()
        }
        // Interleave round-robin so the top of the rail mixes providers instead
        // of showing one catalogue's head-of-list repeatedly.
        interleave(batches).distinctBy { it.videoId }.take(count)
    }

    private fun interleave(batches: List<List<ShortItem>>): List<ShortItem> {
        val out = mutableListOf<ShortItem>()
        var i = 0
        while (batches.any { i < it.size }) {
            batches.forEach { if (i < it.size) out += it[i] }
            i++
        }
        return out
    }

    // ── Providers ────────────────────────────────────────────────────────────

    private interface DramaProvider {
        val name: String
        suspend fun fetch(count: Int): List<ShortItem>
    }

    /**
     * Dailymotion: search the public video API for drama compilations, then ask
     * the player-metadata endpoint for each result's HLS manifest. Search alone
     * yields only ids and titles; the manifest comes from the second call, and
     * that second call is what makes an item playable and downloadable. The
     * resolves run in parallel and are individually time-boxed so one dead or
     * still-encoding video can't stall the batch.
     */
    private object DailymotionProvider : DramaProvider {
        override val name = "dailymotion"
        private const val API = "https://api.dailymotion.com/videos"
        private val queries = listOf(
            "short drama full episodes english",
            "short drama complete english sub",
            "mini drama full series english",
            "vertical drama full episodes",
            "reelshort full episodes",
            "dramabox full episodes"
        )

        override suspend fun fetch(count: Int): List<ShortItem> {
            val perQuery = (count / queries.size).coerceAtLeast(3)
            val candidates = coroutineScope {
                queries.map { query ->
                    async { runCatching { search(query, perQuery) }.getOrDefault(emptyList()) }
                }.awaitAll().flatten()
            }.distinctBy { it.id }

            return coroutineScope {
                candidates.map { hit ->
                    async {
                        withTimeoutOrNull(RESOLVE_TIMEOUT_MS) {
                            runCatching { resolveHls(hit.id) }.getOrNull()
                        }?.let { hls ->
                            buildItem(
                                title = hit.title,
                                author = "Dailymotion",
                                streamUrl = hls,
                                thumbnail = hit.thumbnail,
                                source = ShortsSourceType.DRAMA,
                                durationSeconds = hit.duration
                            )
                        }
                    }
                }.awaitAll().filterNotNull()
            }.take(count)
        }

        private data class Hit(val id: String, val title: String, val thumbnail: String, val duration: Long)

        private fun search(query: String, limit: Int): List<Hit> {
            val url = "$API?search=${query.replace(" ", "+")}" +
                "&fields=id,title,thumbnail_720_url,duration&sort=recent&limit=$limit"
            val list = JSONObject(get(url)).optJSONArray("list") ?: return emptyList()
            return (0 until list.length()).mapNotNull { i ->
                val v = list.optJSONObject(i) ?: return@mapNotNull null
                val id = v.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Hit(
                    id = id,
                    title = v.optString("title").ifBlank { "Short drama" },
                    thumbnail = v.optString("thumbnail_720_url"),
                    duration = v.optLong("duration", 0L)
                )
            }
        }

        /** The HLS master URL from the player metadata, or null if not ready. */
        private fun resolveHls(id: String): String? {
            val json = get(
                "https://www.dailymotion.com/player/metadata/video/$id",
                referer = "https://www.dailymotion.com/embed/video/$id"
            )
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
            // Removed, private or still-encoding videos answer with an `error`
            // and no `qualities` — nothing to play, so skip them.
            if (root.optJSONObject("error") != null) return null
            val auto = root.optJSONObject("qualities")?.optJSONArray("auto") ?: return null
            val url = auto.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }
            return url?.takeIf { it.contains(".m3u8") }
        }
    }

    /**
     * DramaBox through the `reelapi/DramaBox` community frontend. Its proxy
     * needs an `AUTH_TOKEN` that is not published, so in practice this returns
     * nothing — kept as a real attempt because an open deployment (or a
     * self-hosted one on the same paths) would contribute with no code change.
     */
    private object DramaBoxProvider : DramaProvider {
        override val name = "dramabox"
        private val urls = listOf(
            "https://drama-box-alpha.vercel.app/api/foryou/1?lang=en",
            "https://drama-box-alpha.vercel.app/api/new/1?lang=en&pageSize=20"
        )

        override suspend fun fetch(count: Int): List<ShortItem> {
            for (url in urls) {
                val body = runCatching { getJson(url) }.getOrNull() ?: continue
                val items = parseGenericCatalogue(body, "DramaBox")
                if (items.isNotEmpty()) return items.take(count)
            }
            return emptyList()
        }
    }

    /** DramaShorts' public Next.js catalogue data. */
    private object DramaShortsProvider : DramaProvider {
        override val name = "dramashorts"
        private val urls = listOf(
            "https://dramashorts.io/api/series",
            "https://dramashorts.io/_next/data/series.json",
            "https://dramashorts.io/api/dramas?page=1"
        )

        override suspend fun fetch(count: Int): List<ShortItem> {
            for (url in urls) {
                val body = runCatching { getJson(url) }.getOrNull() ?: continue
                val items = parseGenericCatalogue(body, "DramaShorts")
                if (items.isNotEmpty()) return items.take(count)
            }
            return emptyList()
        }
    }

    /** Shortical's browse catalogue. */
    private object ShorticalProvider : DramaProvider {
        override val name = "shortical"
        private val urls = listOf(
            "https://shortical.com/api/series",
            "https://shortical.com/api/browse",
            "https://shortical.com/api/dramas"
        )

        override suspend fun fetch(count: Int): List<ShortItem> {
            for (url in urls) {
                val body = runCatching { getJson(url) }.getOrNull() ?: continue
                val items = parseGenericCatalogue(body, "Shortical")
                if (items.isNotEmpty()) return items.take(count)
            }
            return emptyList()
        }
    }

    /** FlickReels' public classification directory. */
    private object FlickReelsProvider : DramaProvider {
        override val name = "flickreels"
        private val urls = listOf(
            "https://www.flickreels.net/api/classify",
            "https://www.flickreels.net/api/series?page=1",
            "https://api.flickreels.net/v1/series"
        )

        override suspend fun fetch(count: Int): List<ShortItem> {
            for (url in urls) {
                val body = runCatching { getJson(url) }.getOrNull() ?: continue
                val items = parseGenericCatalogue(body, "FlickReels")
                if (items.isNotEmpty()) return items.take(count)
            }
            return emptyList()
        }
    }

    /** Luni's shorts-drama.com public series index. */
    private object ShortsDramaProvider : DramaProvider {
        override val name = "shorts-drama"
        private val urls = listOf(
            "https://www.shorts-drama.com/api/series",
            "https://www.shorts-drama.com/api/series/all"
        )

        override suspend fun fetch(count: Int): List<ShortItem> {
            for (url in urls) {
                val body = runCatching { getJson(url) }.getOrNull() ?: continue
                val items = parseGenericCatalogue(body, "Shorts-Drama")
                if (items.isNotEmpty()) return items.take(count)
            }
            return emptyList()
        }
    }

    /**
     * iptv-org's public category playlists. Key-free and stable, so it is the
     * rail's floor: it always yields title + `.m3u8` pairs even when every
     * scraped provider above is unreachable.
     */
    private object IptvOrgProvider : DramaProvider {
        override val name = "iptv-org"
        private const val BASE = "https://iptv-org.github.io/iptv/categories"
        private val categories = listOf("movies", "series", "entertainment")

        override suspend fun fetch(count: Int): List<ShortItem> {
            val out = mutableListOf<ShortItem>()
            for (category in categories) {
                if (out.size >= count) break
                val playlist = runCatching { get("$BASE/$category.m3u") }.getOrNull() ?: continue
                out += parseM3u(playlist, category)
            }
            return out.shuffled().take(count)
        }
    }

    private val PROVIDERS = listOf(
        DailymotionProvider, IptvOrgProvider, DramaBoxProvider,
        DramaShortsProvider, ShorticalProvider, FlickReelsProvider, ShortsDramaProvider
    )

    /** Internal so unit tests can exercise the parsers without a network. */
    internal companion object {
        private const val TAG = "DramaShortsSource"
        private const val PROVIDER_TIMEOUT_MS = 15_000L
        private const val RESOLVE_TIMEOUT_MS = 8_000L

        internal fun parseM3u(text: String, category: String): List<ShortItem> {
            val items = mutableListOf<ShortItem>()
            var pendingTitle: String? = null
            var pendingLogo: String = ""
            text.lineSequence().forEach { raw ->
                val line = raw.trim()
                when {
                    line.startsWith("#EXTINF") -> {
                        pendingTitle = line.substringAfterLast(',', "").trim().ifBlank { null }
                        pendingLogo = Regex("""tvg-logo="([^"]*)"""").find(line)?.groupValues?.get(1).orEmpty()
                    }
                    line.isNotEmpty() && !line.startsWith("#") -> {
                        val title = pendingTitle
                        if (title != null && line.contains(".m3u8", ignoreCase = true)) {
                            items += buildItem(
                                title = title,
                                author = category.replaceFirstChar { it.uppercase() },
                                streamUrl = line,
                                thumbnail = pendingLogo,
                                source = ShortsSourceType.DRAMA
                            )
                        }
                        pendingTitle = null
                        pendingLogo = ""
                    }
                }
            }
            return items
        }

        /**
         * Best-effort reader for the scraped providers. Their payload shapes differ
         * (a bare array, an object with a `data`/`items`/`results`/`series` array,
         * or nested `list`), so this walks the common ones and pulls the fields
         * that identify a playable item. An entry only becomes an item when it
         * carries a usable stream URL — a catalogue card with no media URL can't
         * be played or downloaded, so it is dropped rather than shown as dead.
         */
        internal fun parseGenericCatalogue(json: String, author: String): List<ShortItem> {
            val array = extractArray(json) ?: return emptyList()
            val out = mutableListOf<ShortItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val title = firstString(obj, "title", "name", "dramaName", "seriesName", "bookName")
                    ?: continue
                val streamUrl = firstString(
                    obj, "videoUrl", "playUrl", "m3u8", "hlsUrl", "hls", "url", "episodeUrl", "play_url"
                ) ?: continue
                if (!looksLikeMedia(streamUrl)) continue
                out += buildItem(
                    title = title,
                    author = author,
                    streamUrl = streamUrl,
                    thumbnail = firstString(obj, "cover", "coverUrl", "poster", "thumbnail", "image", "coverImg")
                        .orEmpty(),
                    source = ShortsSourceType.DRAMA
                )
            }
            return out
        }

        private fun extractArray(json: String): JSONArray? {
            val trimmed = json.trim()
            if (trimmed.startsWith("[")) return runCatching { JSONArray(trimmed) }.getOrNull()
            val root = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
            for (key in listOf("data", "items", "results", "series", "list", "dramas", "records", "content")) {
                when (val value = root.opt(key)) {
                    is JSONArray -> return value
                    is JSONObject -> {
                        for (inner in listOf("list", "items", "records", "series", "data")) {
                            (value.opt(inner) as? JSONArray)?.let { return it }
                        }
                    }
                }
            }
            return null
        }

        private fun firstString(obj: JSONObject, vararg keys: String): String? =
            keys.firstNotNullOfOrNull { key ->
                obj.optString(key).takeIf { it.isNotBlank() && it != "null" }
            }

        private fun looksLikeMedia(url: String): Boolean =
            url.startsWith("http") &&
                (url.contains(".m3u8") || url.contains(".mp4") || url.contains("/hls/") || url.contains("m3u8"))

        private fun buildItem(
            title: String,
            author: String,
            streamUrl: String,
            thumbnail: String,
            source: ShortsSourceType,
            durationSeconds: Long = 0L
        ): ShortItem = ShortItem(
            videoId = stableId(streamUrl + title),
            title = title,
            authorName = author,
            streamUrl = streamUrl,
            streamKind = if (streamUrl.contains(".m3u8") || streamUrl.contains("/hls/")) {
                StreamKind.HLS
            } else {
                StreamKind.PROGRESSIVE
            },
            thumbnailUrl = thumbnail,
            durationSeconds = durationSeconds,
            watchUrl = streamUrl,
            source = source
        )

        /** Stable, URL-derived id so the same stream de-dupes across refreshes. */
        private fun stableId(seed: String): String {
            val hash = seed.hashCode().toLong() and 0xFFFFFFFFL
            return "dr" + hash.toString(36).padStart(9, '0').takeLast(9)
        }

        // ── HTTP ─────────────────────────────────────────────────────────────────

        private fun getJson(url: String): String {
            val body = get(url)
            // Some deployments answer HTML (a login page or a 404 shell) rather
            // than JSON; that must not be treated as a catalogue.
            val head = body.trimStart()
            if (!head.startsWith("[") && !head.startsWith("{")) return "null"
            return body
        }

        private fun get(url: String, referer: String? = null): String {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) VidMate2/1.0")
                setRequestProperty("Accept", "application/json, text/plain, */*")
                referer?.let { setRequestProperty("Referer", it) }
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        }
    }
}
