package com.cobalt.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cobalt.android.download.DownloadRepository
import com.cobalt.android.shorts.source.InvidiousShortsSource
import com.cobalt.android.shorts.source.ShortsQueryFeeder
import com.cobalt.android.util.LocaleHelper
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
        val shortsQueries: String,
        val appLanguage: String,
        val contentRegion: String,
        val notificationsEnabled: Boolean,
        val notifyOnComplete: Boolean,
        val recommendationsEnabled: Boolean,
        val parallelSegments: Int,
        val floatingPlayer: Boolean,
        val onlineLyrics: Boolean,
        val keepAliveNotice: Boolean,
        val browserAntiDetection: Boolean,
        val browserHistory: Boolean,
        val hotfixEnabled: Boolean,
        val contentLanguageRule: String
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
            shortsQueries = settings.customShortsQueries.joinToString("\n"),
            appLanguage = settings.appLanguage,
            contentRegion = settings.contentRegion,
            notificationsEnabled = settings.notificationsEnabled,
            notifyOnComplete = settings.notifyOnComplete,
            recommendationsEnabled = settings.recommendationsEnabled,
            parallelSegments = settings.parallelSegments,
            floatingPlayer = settings.floatingPlayerEnabled,
            onlineLyrics = settings.onlineLyricsEnabled,
            keepAliveNotice = settings.keepAliveNotice,
            browserAntiDetection = settings.browserAntiDetection,
            browserHistory = settings.browserHistoryEnabled,
            hotfixEnabled = settings.hotfixEnabled,
            contentLanguageRule = settings.contentLanguageRule
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

    /** Persists the UI language and applies it process-wide. The caller
     *  recreates the Activity so the change is visible immediately (see
     *  `SettingsScreen`'s `onRecreate`). */
    fun setAppLanguage(tag: String) {
        settings.appLanguage = tag
        LocaleHelper.apply(tag)
        _state.value = _state.value.copy(appLanguage = tag)
    }

    /** Content region (ISO-3166) used to bias feed/search queries. */
    fun setContentRegion(code: String) {
        settings.contentRegion = code
        _state.value = _state.value.copy(contentRegion = code)
    }

    fun setNotificationsEnabled(value: Boolean) {
        settings.notificationsEnabled = value
        _state.value = _state.value.copy(notificationsEnabled = value)
    }

    fun setNotifyOnComplete(value: Boolean) {
        settings.notifyOnComplete = value
        _state.value = _state.value.copy(notifyOnComplete = value)
    }

    /** Toggles the on-device recommender and (un)schedules its daily job. */
    fun setRecommendationsEnabled(value: Boolean) {
        settings.recommendationsEnabled = value
        _state.value = _state.value.copy(recommendationsEnabled = value)
        if (value) {
            com.cobalt.android.recommend.RecommendationWorker.schedule(getApplication<Application>())
        } else {
            com.cobalt.android.recommend.RecommendationWorker.cancel(getApplication<Application>())
        }
    }

    fun setParallelSegments(value: Int) {
        settings.parallelSegments = value
        _state.value = _state.value.copy(parallelSegments = value)
    }

    fun setFloatingPlayer(value: Boolean) {
        settings.floatingPlayerEnabled = value
        _state.value = _state.value.copy(floatingPlayer = value)
    }

    fun setOnlineLyrics(value: Boolean) {
        settings.onlineLyricsEnabled = value
        _state.value = _state.value.copy(onlineLyrics = value)
    }

    fun setKeepAliveNotice(value: Boolean) {
        settings.keepAliveNotice = value
        _state.value = _state.value.copy(keepAliveNotice = value)
    }

    fun setBrowserAntiDetection(value: Boolean) {
        settings.browserAntiDetection = value
        _state.value = _state.value.copy(browserAntiDetection = value)
    }

    fun setBrowserHistory(value: Boolean) {
        settings.browserHistoryEnabled = value
        _state.value = _state.value.copy(browserHistory = value)
    }

    /** Enabling hot-fix loading is a security-relevant opt-in; off by default. */
    fun setHotfixEnabled(value: Boolean) {
        settings.hotfixEnabled = value
        _state.value = _state.value.copy(hotfixEnabled = value)
    }

    fun setContentLanguageRule(value: String) {
        settings.contentLanguageRule = value
        _state.value = _state.value.copy(contentLanguageRule = value)
    }

    /** Folds in the old `SettingsSheet`'s "clear history" action (it cleared
     * the download queue); the sheet itself is retired in Phase 26. */
    fun clearDownloadHistory() {
        viewModelScope.launch { downloadRepository.clearHistory() }
    }
}
