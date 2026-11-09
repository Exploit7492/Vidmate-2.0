package com.cobalt.android.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cobalt.android.feed.FeedRepository
import com.cobalt.android.feed.model.FeedItem
import com.cobalt.android.feed.source.InnertubeStreamResolver
import com.cobalt.android.link.LinkResolverRepository
import kotlinx.coroutines.launch

/**
 * Home is two things at once, and this ViewModel owns both:
 *
 * 1. **The feed** (Phase 23) — a real, YouTube-backed list of regular-length
 *    videos ([feed]) loaded from [FeedRepository], replacing the
 *    intentionally-empty `feedContainer` reservation that stood in for a
 *    feed since Phase 3. Tapping a tile plays it inline ([nowPlaying]) after
 *    resolving a playable stream on demand, or falls back to the download
 *    path ([downloadFromFeed]).
 * 2. **The paste-link resolver** (Phase 3/4/6) — the paste-a-link box and
 *    the real [LinkResolverRepository] network resolve, unchanged in
 *    behaviour from the previous phase; its result still drives
 *    `ResolutionPickerDialog`.
 *
 * Both live here (rather than two ViewModels) because they share one screen
 * and one scroll container, and the paste box's success feeds the same
 * download pipeline the feed's "download" action uses.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val feedRepository = FeedRepository(application)
    private val streamResolver = InnertubeStreamResolver()
    private val commentsRepository = com.cobalt.android.feed.comments.CommentsRepository()
    private val resolver = LinkResolverRepository(application)

    // ── Feed state ──────────────────────────────────────────────────────────

    private val _feed = MutableLiveData<List<FeedItem>>(emptyList())
    val feed: LiveData<List<FeedItem>> = _feed

    private val _isLoadingFeed = MutableLiveData(false)
    val isLoadingFeed: LiveData<Boolean> = _isLoadingFeed

    private val _isFeedFromCache = MutableLiveData(false)
    val isFeedFromCache: LiveData<Boolean> = _isFeedFromCache

    /** The item currently playing inline (null = nothing playing). Only one
     * Home video plays at a time, matching the Shorts feed's one-live-player
     * rule. */
    private val _nowPlaying = MutableLiveData<FeedItem?>(null)
    val nowPlaying: LiveData<FeedItem?> = _nowPlaying

    /** Set while a tapped item's stream is being resolved, so the tile can
     * show a spinner instead of looking unresponsive. */
    private val _isResolvingStream = MutableLiveData(false)
    val isResolvingStream: LiveData<Boolean> = _isResolvingStream

    init {
        loadFeed()
    }

    fun loadFeed() {
        if (_isLoadingFeed.value == true) return
        viewModelScope.launch {
            _isLoadingFeed.value = true
            val page = feedRepository.loadFeed()
            _feed.value = page.items
            _isFeedFromCache.value = page.isFromCache
            _isLoadingFeed.value = false
        }
    }

    fun refreshFeed() {
        if (_isLoadingFeed.value == true) return
        viewModelScope.launch {
            _isLoadingFeed.value = true
            val page = feedRepository.loadFeed()
            // De-dupe against what's already shown so refresh appends fresh
            // items rather than replacing the list under the user's thumb.
            val existing = _feed.value.orEmpty()
            val existingIds = existing.map { it.videoId }.toHashSet()
            val merged = existing + page.items.filter { it.videoId !in existingIds }
            _feed.value = merged
            _isFeedFromCache.value = page.isFromCache
            _isLoadingFeed.value = false
        }
    }

    /**
     * Tapping a feed tile: if the item already carries a resolved stream
     * (cache), play immediately; otherwise resolve on demand first. A failed
     * resolve clears the spinner and leaves [nowPlaying] null — the tile
     * stays tappable and the download path remains available.
     */
    fun onFeedItemClick(item: FeedItem) {
        if (item.streamUrl != null) {
            _nowPlaying.value = item
            return
        }
        if (_isResolvingStream.value == true) return
        viewModelScope.launch {
            _isResolvingStream.value = true
            val resolved = streamResolver.resolve(item.videoId)
            if (resolved != null) {
                val playable = item.copy(streamUrl = resolved.url, streamKind = resolved.kind)
                _feed.value = _feed.value.orEmpty().map {
                    if (it.videoId == item.videoId) playable else it
                }
                _nowPlaying.value = playable
            }
            _isResolvingStream.value = false
        }
    }

    fun stopPlayback() {
        _nowPlaying.value = null
        clearComments()
    }

    // ── Comments (E11) ──────────────────────────────────────────────────────

    private val _comments = MutableLiveData<List<com.cobalt.android.feed.comments.CommentsParser.Comment>>(emptyList())
    val comments: LiveData<List<com.cobalt.android.feed.comments.CommentsParser.Comment>> = _comments

    private val _isLoadingComments = MutableLiveData(false)
    val isLoadingComments: LiveData<Boolean> = _isLoadingComments

    private val _commentsError = MutableLiveData<String?>(null)
    val commentsError: LiveData<String?> = _commentsError

    /** Fetches the public comment page for the inline-playing video (E11). */
    fun loadComments(videoId: String) {
        if (videoId.isBlank()) return
        _isLoadingComments.value = true
        _commentsError.value = null
        viewModelScope.launch {
            val page = commentsRepository.fetch(videoId)
            _comments.value = page?.comments.orEmpty()
            _commentsError.value = if (page == null) "could not load comments" else null
            _isLoadingComments.value = false
        }
    }

    fun clearComments() {
        _comments.value = emptyList()
        _commentsError.value = null
        _isLoadingComments.value = false
    }

    /**
     * "Download" on a feed tile reuses the existing real download pipeline
     * via the same resolve → picker flow the paste box drives, rather than a
     * second save mechanism (ARCHITECTURE.md "Unified enhancement, NOT a
     * parallel rebuild").
     */
    fun downloadFromFeed(item: FeedItem) {
        _linkInput.value = item.watchUrl
        onSubmit()
    }

    // ── Paste-link resolver state (unchanged behaviour from Phase 3/4/6) ────

    private val _linkInput = MutableLiveData("")
    val linkInput: LiveData<String> = _linkInput

    private val _statusMessage = MutableLiveData<String?>(null)
    val statusMessage: LiveData<String?> = _statusMessage

    private val _isResolving = MutableLiveData(false)
    val isResolving: LiveData<Boolean> = _isResolving

    private val _resolveResult = MutableLiveData<LinkResolverRepository.ResolveResult?>(null)
    val resolveResult: LiveData<LinkResolverRepository.ResolveResult?> = _resolveResult

    fun setPendingUrl(url: String) {
        _linkInput.value = url
    }

    fun onLinkInputChanged(text: String) {
        _linkInput.value = text
        _statusMessage.value = null
        _resolveResult.value = null
    }

    fun onSubmit() {
        val url = _linkInput.value.orEmpty().trim()
        if (url.isEmpty()) {
            _statusMessage.value = "Paste a link first."
            return
        }
        if (!isPlausibleUrl(url)) {
            _statusMessage.value = "That doesn't look like a valid link."
            return
        }
        if (_isResolving.value == true) return

        viewModelScope.launch {
            _isResolving.value = true
            _statusMessage.value = "Resolving…"
            _resolveResult.value = null

            when (val result = resolver.resolve(url)) {
                is LinkResolverRepository.ResolveResult.Success -> {
                    _resolveResult.value = result
                    _statusMessage.value = "Found ${result.formats.size} format(s)."
                }
                is LinkResolverRepository.ResolveResult.Error -> {
                    // E5: add VPN/retry guidance for geo-blocked and rate-limited
                    // failures, which are otherwise opaque error strings.
                    _statusMessage.value = com.cobalt.android.link.ResolveFailureAdvisor
                        .advise(result.message)
                }
            }

            _isResolving.value = false
        }
    }

    fun clearResolveResult() {
        _resolveResult.value = null
    }

    private fun isPlausibleUrl(url: String): Boolean =
        url.startsWith("http://") || url.startsWith("https://")
}
