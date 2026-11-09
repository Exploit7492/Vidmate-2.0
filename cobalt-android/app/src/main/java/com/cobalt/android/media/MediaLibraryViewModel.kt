package com.cobalt.android.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the local media library screen (Phase D4/D5/D3/D6): scans the device,
 * then exposes the list with the user's current sort / kind-filter / search
 * applied via [LocalMediaQuery].
 */
class MediaLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMediaRepository(application)

    data class UiState(
        val items: List<LocalMediaItem> = emptyList(),
        val loading: Boolean = true,
        val sort: LocalMediaQuery.Sort = LocalMediaQuery.Sort.DATE_DESC,
        val filter: LocalMediaQuery.KindFilter = LocalMediaQuery.KindFilter.ALL,
        val search: String = ""
    ) {
        /** The visible list, derived from the raw scan + current controls. */
        val visible: List<LocalMediaItem>
            get() = LocalMediaQuery.apply(items, sort, filter, search)
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            val scanned = repository.load()
            _state.value = _state.value.copy(items = scanned, loading = false)
        }
    }

    fun setSort(sort: LocalMediaQuery.Sort) { _state.value = _state.value.copy(sort = sort) }
    fun setFilter(filter: LocalMediaQuery.KindFilter) { _state.value = _state.value.copy(filter = filter) }
    fun setSearch(text: String) { _state.value = _state.value.copy(search = text) }
}
