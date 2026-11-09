package com.cobalt.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cobalt.android.download.DownloadRepository
import com.cobalt.android.shorts.source.InvidiousShortsSource
import com.cobalt.android.shorts.source.ShortsQueryFeeder
import com.cobalt.android.util.SettingsRepository
import com.cobalt.android.util.SettingsRepository.DownloadFormatPreference
import com.cobalt.android.util.SettingsRepository.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 25: backs the Compose Settings screen. Unlike the previous XML
 * `SettingsFragment` (which committed multi-line text fields in `onPause`),
 * this persists every change immediately — Compose recomposes on each
 * keystroke/selection, so there's no single "leaving the screen" moment to
 * hook, and immediate persistence is the less surprising behaviour anyway.
 *
 * A single immutable [UiState] is exposed as a `StateFlow`; the screen
 * collects it and calls the `set*` methods. All reads/writes still go
 * through the same [SettingsRepository] every other screen uses.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)
    private val downloadRepository = DownloadRepository(application)

    data class UiState(
        val defaultFormat: DownloadFormatPreference,
        val downloadLocation: String,
        val themeMode: ThemeMode,
        val cobaltApiKey: String,
        val audioOnly: Boolean,
        val clipboardTrigger: Boolean,
        val invidiousInstances: String,
        val shortsQueries: String
    )

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private fun readState(): UiState {
        val storedInstances = settings.invidiousInstances
        val isCustomInstances = storedInstances != InvidiousShortsSource.DEFAULT_INSTANCES
        return UiState(
            defaultFormat = settings.defaultDownloadFormat,
            downloadLocation = settings.downloadLocation,
            themeMode = settings.themeMode,
            cobaltApiKey = settings.cobaltApiKey,
            audioOnly = settings.audioOnlyMode,
            clipboardTrigger = settings.clipboardTriggerEnabled,
            // Blank when the shipped default is in use (shown as a hint),
            // matching the old fragment's behaviour.
            invidiousInstances = if (isCustomInstances) storedInstances.joinToString("\n") else "",
            shortsQueries = settings.customShortsQueries.joinToString("\n")
        )
    }

    fun setDefaultFormat(value: DownloadFormatPreference) {
        settings.defaultDownloadFormat = value
        _state.value = _state.value.copy(defaultFormat = value)
    }

    fun setDownloadLocation(value: String) {
        if (value.trim().isNotBlank()) settings.downloadLocation = value
        _state.value = _state.value.copy(downloadLocation = value)
    }

    fun setThemeMode(value: ThemeMode) {
        settings.themeMode = value
        _state.value = _state.value.copy(themeMode = value)
    }

    fun setCobaltApiKey(value: String) {
        settings.cobaltApiKey = value
        _state.value = _state.value.copy(cobaltApiKey = value)
    }

    fun setAudioOnly(value: Boolean) {
        settings.audioOnlyMode = value
        _state.value = _state.value.copy(audioOnly = value)
    }

    fun setClipboardTrigger(value: Boolean) {
        settings.clipboardTriggerEnabled = value
        _state.value = _state.value.copy(clipboardTrigger = value)
    }

    fun setInvidiousInstances(value: String) {
        val cleaned = value.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        settings.invidiousInstances = cleaned.ifEmpty { InvidiousShortsSource.DEFAULT_INSTANCES }
        _state.value = _state.value.copy(invidiousInstances = value)
    }

    fun setShortsQueries(value: String) {
        settings.customShortsQueries = value.split("\n")
        // Take effect on the very next feed refresh, not just next launch.
        ShortsQueryFeeder.applyCustomQueries(settings.customShortsQueries)
        _state.value = _state.value.copy(shortsQueries = value)
    }

    /** Folds in the old `SettingsSheet`'s "clear history" action (it cleared
     * the download queue); the sheet itself is retired in Phase 26. */
    fun clearDownloadHistory() {
        viewModelScope.launch { downloadRepository.clearHistory() }
    }
}
