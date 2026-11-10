package com.cobalt.android.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cobalt.android.feed.explore.ExploreRepository
import com.cobalt.android.feed.model.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the Explore screen (Phase "use Innertube fully"): in-app YouTube
 * search, the trending grid, and YouTube-Music discovery — all public InnerTube
 * reads, no sign-in.
 */
class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    enum class Tab { SEARCH, TRENDING, MUSIC }

    private val repository = ExploreRepository()

    data class UiState(
        val tab: Tab = Tab.SEARCH,
        val query: String = "",
        val searchResults: List<FeedItem> = emptyList(),
        val estimatedResults: String? = null,
        val searchLoading: Boolean = false,
        val searchError: String? = null,
        val trending: List<FeedItem> = emptyList(),
        val trendingLoading: Boolean = false,
        val music: List<FeedItem> = emptyList(),
        val musicLoading: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var searchToken: String? = null

    fun selectTab(tab: Tab) {
        _state.value = _state.value.copy(tab = tab)
        when (tab) {
            Tab.TRENDING -> if (_state.value.trending.isEmpty()) loadTrending()
            Tab.MUSIC -> if (_state.value.music.isEmpty()) loadMusic()
            Tab.SEARCH -> Unit
        }
    }

    fun onQueryChanged(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        _state.value = _state.value.copy(searchLoading = true, searchError = null)
        viewModelScope.launch {
            val page = repository.search(q)
            searchToken = page?.nextToken
            _state.value = _state.value.copy(
                searchResults = page?.items.orEmpty(),
                estimatedResults = page?.estimatedResults,
                searchLoading = false,
                searchError = if (page == null) "search failed" else null
            )
        }
    }

    fun searchMore() {
        val token = searchToken ?: return
        searchToken = null
        viewModelScope.launch {
            val page = repository.continueFrom(token) ?: return@launch
            searchToken = page.nextToken
            val seen = _state.value.searchResults.map { it.videoId }.toHashSet()
            _state.value = _state.value.copy(
                searchResults = _state.value.searchResults + page.items.filter { it.videoId !in seen }
            )
        }
    }

    fun loadTrending() {
        _state.value = _state.value.copy(trendingLoading = true)
        viewModelScope.launch {
            val page = repository.trending()
            _state.value = _state.value.copy(trending = page?.items.orEmpty(), trendingLoading = false)
        }
    }

    fun loadMusic() {
        _state.value = _state.value.copy(musicLoading = true)
        viewModelScope.launch {
            val page = repository.musicHome()
            _state.value = _state.value.copy(music = page?.items.orEmpty(), musicLoading = false)
        }
    }
}
