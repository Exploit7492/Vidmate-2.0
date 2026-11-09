package com.cobalt.android.status

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs the WhatsApp status saver screen (E8). */
class StatusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatusRepository(application)

    data class UiState(
        val items: List<StatusRepository.StatusFile> = emptyList(),
        val loading: Boolean = true,
        val status: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.value = _state.value.copy(loading = true, status = null)
        viewModelScope.launch {
            val items = repository.scan()
            _state.value = _state.value.copy(items = items, loading = false)
        }
    }

    fun save(file: StatusRepository.StatusFile) {
        viewModelScope.launch {
            val uri = repository.save(file)
            _state.value = _state.value.copy(
                status = if (uri != null) "saved ${file.name}" else "could not save ${file.name}"
            )
        }
    }
}
