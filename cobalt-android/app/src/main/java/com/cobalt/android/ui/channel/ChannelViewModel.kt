package com.cobalt.android.ui.channel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cobalt.android.feed.channel.ChannelRepository
import com.cobalt.android.feed.model.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs the channel feed screen (H5). */
class ChannelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChannelRepository()

    data class UiState(
        val url: String = "",
        val channelName: String = "",
        val videos: List<FeedItem> = emptyList(),
        val loading: Boolean = true,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var nextToken: String? = null

    fun load(url: String) {
        _state.value = _state.value.copy(url = url, loading = true, error = null, videos = emptyList())
        viewModelScope.launch {
            val page = repository.load(url)
            nextToken = page?.nextToken
            _state.value = _state.value.copy(
                channelName = page?.channelName.orEmpty(),
                videos = page?.videos.orEmpty(),
                loading = false,
                error = if (page == null) "could not load this channel" else null
            )
        }
    }

    fun loadMore() {
        val token = nextToken ?: return
        nextToken = null
        viewModelScope.launch {
            val page = repository.loadMore(token) ?: return@launch
            nextToken = page.nextToken
            val existing = _state.value.videos.map { it.videoId }.toHashSet()
            _state.value = _state.value.copy(
                videos = _state.value.videos + page.videos.filter { it.videoId !in existing }
            )
        }
    }
}
