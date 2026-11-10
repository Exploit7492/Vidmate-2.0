package com.cobalt.android.link

import android.content.Context
import com.cobalt.android.download.DownloadService
import com.cobalt.android.feed.explore.ExploreRepository
import com.cobalt.android.feed.explore.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves and downloads a playlist (Phase B7 / E3).
 *
 * This app's cobalt backend resolves one URL to one item, not a list, so a
 * "playlist" is handled here as a small, explicit batch: each line of the
 * pasted text is treated as one item URL, resolved and enqueued independently.
 * That covers the realistic user flow (copy a set of links from a playlist page
 * or a chat, paste them, download all) without pretending the backend supports
 * native playlist pagination it does not.
 *
 * Runs sequentially rather than in parallel: the resolves hit one cobalt
 * instance, and firing N at once is the fastest way to get rate-limited
 * (`rate-limit` is a real response code — see [LinkResolverRepository]).
 */
class PlaylistDownloader(private val context: Context) {

    private val resolver = LinkResolverRepository(context)
    private val playlists = PlaylistRepository()

    data class Result(val enqueued: Int, val failed: Int)

    /**
     * Resolves each non-blank line of [text] and enqueues the first format of
     * each successful resolve. Reports how many were enqueued vs. skipped.
     */
    suspend fun downloadAll(text: String, cookies: String, userAgent: String): Result =
        withContext(Dispatchers.IO) {
            val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()

            // Expand any YouTube playlist URL to its real track list first, so
            // "paste a playlist link, download it all" works. A playlist we
            // can't read (private, or an account-only "Watch later") expands to
            // nothing and falls back to being treated as a plain URL below.
            val urls = lines.flatMap { line ->
                val id = ExploreRepository.playlistId(line)
                if (id == null) listOf(line)
                else playlists.load(line)?.tracks?.map { it.watchUrl }?.ifEmpty { listOf(line) } ?: listOf(line)
            }

            var ok = 0
            var failed = 0
            for (url in urls) {
                when (val r = resolver.resolve(url)) {
                    is LinkResolverRepository.ResolveResult.Success -> {
                        val first = r.formats.firstOrNull()
                        if (first == null) {
                            failed++
                        } else {
                            DownloadService.startHttps(
                                ctx = context,
                                cobaltUrl = first.url,
                                filename = first.filename,
                                mimeType = first.mimeType,
                                cookies = cookies,
                                userAgent = userAgent,
                                originalUrl = url
                            )
                            ok++
                        }
                    }
                    is LinkResolverRepository.ResolveResult.Error -> failed++
                }
            }
            Result(ok, failed)
        }
}
