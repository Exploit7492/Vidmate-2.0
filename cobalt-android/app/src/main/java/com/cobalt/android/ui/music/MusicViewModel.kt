package com.cobalt.android.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cobalt.android.media.LocalMediaItem
import com.cobalt.android.media.LocalMediaQuery
import com.cobalt.android.media.LocalMediaRepository
import com.cobalt.android.download.DownloadDatabase
import com.cobalt.android.music.MusicPlaylist
import com.cobalt.android.music.MusicPlaylistDao
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
    private val playlistDao = DownloadDatabase.getInstance(application).musicPlaylistDao()

    data class UiState(
        val tracks: List<LocalMediaItem> = emptyList(),
        val currentIndex: Int = -1,
        val playing: Boolean = false,
        val loading: Boolean = true,
        val search: String = "",
        val lyrics: List<LrcParser.Line> = emptyList(),
        val playlists: List<MusicPlaylist> = emptyList()
    ) {
        val current: LocalMediaItem? get() = tracks.getOrNull(currentIndex)
        val visible: List<LocalMediaItem>
            get() = LocalMediaQuery.apply(tracks, sort = LocalMediaQuery.Sort.NAME_ASC, search = search)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
        refreshPlaylists()
    }

    private fun load() {
        viewModelScope.launch {
            val audio = repository.load().filter { it.kind == LocalMediaRepository.Kind.AUDIO }
            _state.value = _state.value.copy(tracks = audio, loading = false)
        }
    }

    fun setSearch(text: String) { _state.value = _state.value.copy(search = text) }

    private fun refreshPlaylists() {
        viewModelScope.launch {
            val lists = runCatching { playlistDao.getAll() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(playlists = lists)
        }
    }

    /**
     * Saves [uris] as a named playlist (G4). The name is required — a
     * nameless list is unreachable in the UI — so a blank name is rejected by
     * the caller before this is invoked.
     */
    fun createPlaylist(name: String, uris: List<String>) {
        if (name.isBlank() || uris.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                playlistDao.insert(MusicPlaylist(name = name.trim(), trackUris = MusicPlaylistDao.encode(uris)))
            }
            refreshPlaylists()
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            runCatching { playlistDao.deleteById(id) }
            refreshPlaylists()
        }
    }

    /**
     * Replaces the play queue with the playlist's tracks (in the device
     * library's order) and starts playing the first one. Tracks no longer on
     * the device are skipped, since the queue is built from the current scan.
     */
    fun playPlaylist(playlist: MusicPlaylist) {
        val wanted = playlist.uris.toSet()
        val filtered = _state.value.tracks.filter { it.uri in wanted }
        if (filtered.isEmpty()) return
        _state.value = _state.value.copy(tracks = filtered, currentIndex = 0, playing = true)
        loadLyrics(filtered.first().uri)
    }

    /** Selects a track by its position within the *visible* (filtered) list. */
    fun playVisible(index: Int) {
        val item = _state.value.visible.getOrNull(index) ?: return
        val realIndex = _state.value.tracks.indexOfFirst { it.uri == item.uri }
        if (realIndex >= 0) setCurrent(realIndex)
    }

    fun next() {
        val s = _state.value
        if (s.tracks.isEmpty()) return
        setCurrent((s.currentIndex + 1) % s.tracks.size)
    }

    fun previous() {
        val s = _state.value
        if (s.tracks.isEmpty()) return
        setCurrent((s.currentIndex - 1 + s.tracks.size) % s.tracks.size)
    }

    private fun setCurrent(index: Int) {
        _state.value = _state.value.copy(currentIndex = index, playing = true)
        loadLyrics(_state.value.tracks.getOrNull(index)?.uri)
    }

    /**
     * Loads a `.lrc` sidecar sitting next to a local audio file (G2). Only
     * `file://` URIs are handled — a MediaStore `content://` URI has no stable
     * sibling path to guess — so this silently yields no lyrics otherwise
     * rather than pretending to fetch them.
     */
    private fun loadLyrics(uri: String?) {
        val path = uri?.takeIf { it.startsWith("file://") }?.removePrefix("file://")
        val lines = if (path == null) emptyList() else {
            val lrc = java.io.File(path.substringBeforeLast('.') + ".lrc")
            if (lrc.exists()) runCatching { LrcParser.parse(lrc.readText()) }.getOrDefault(emptyList())
            else emptyList()
        }
        _state.value = _state.value.copy(lyrics = lines)
    }

    fun togglePlay() {
        val s = _state.value
        if (s.currentIndex < 0) { next(); return }
        _state.value = s.copy(playing = !s.playing)
    }

    fun onPlaybackEnded() { next() }
}
