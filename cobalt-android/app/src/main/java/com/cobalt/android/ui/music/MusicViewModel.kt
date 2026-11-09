package com.cobalt.android.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cobalt.android.media.LocalMediaItem
import com.cobalt.android.media.LocalMediaQuery
import com.cobalt.android.media.LocalMediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the music player (Phase G1 / E10).
 *
 * Lists the device's audio (reusing [LocalMediaRepository] and the audio filter
 * of [LocalMediaQuery]) and tracks the currently-playing item + transport state.
 * Playback itself is owned by [MusicFragment]'s ExoPlayer; this only holds the
 * queue and the index so the mini-player and the list stay in sync.
 */
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMediaRepository(application)

    data class UiState(
        val tracks: List<LocalMediaItem> = emptyList(),
        val currentIndex: Int = -1,
        val playing: Boolean = false,
        val loading: Boolean = true,
        val search: String = ""
    ) {
        val current: LocalMediaItem? get() = tracks.getOrNull(currentIndex)
        val visible: List<LocalMediaItem>
            get() = LocalMediaQuery.apply(tracks, sort = LocalMediaQuery.Sort.NAME_ASC, search = search)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val audio = repository.load().filter { it.kind == LocalMediaRepository.Kind.AUDIO }
            _state.value = _state.value.copy(tracks = audio, loading = false)
        }
    }

    fun setSearch(text: String) { _state.value = _state.value.copy(search = text) }

    /** Selects a track by its position within the *visible* (filtered) list. */
    fun playVisible(index: Int) {
        val item = _state.value.visible.getOrNull(index) ?: return
        val realIndex = _state.value.tracks.indexOfFirst { it.uri == item.uri }
        if (realIndex >= 0) _state.value = _state.value.copy(currentIndex = realIndex, playing = true)
    }

    fun next() {
        val s = _state.value
        if (s.tracks.isEmpty()) return
        _state.value = s.copy(currentIndex = (s.currentIndex + 1) % s.tracks.size, playing = true)
    }

    fun previous() {
        val s = _state.value
        if (s.tracks.isEmpty()) return
        _state.value = s.copy(currentIndex = (s.currentIndex - 1 + s.tracks.size) % s.tracks.size, playing = true)
    }

    fun togglePlay() {
        val s = _state.value
        if (s.currentIndex < 0) { next(); return }
        _state.value = s.copy(playing = !s.playing)
    }

    fun onPlaybackEnded() { next() }
}
