package com.cobalt.android.util

import android.media.audiofx.Equalizer
import android.util.Log

/**
 * Wraps the platform [Equalizer] audio effect (Phase G3).
 *
 * The effect is bound to an ExoPlayer audio session (`exo.audioSessionId`), so
 * it processes the player's output rather than the whole device mix — only
 * this app's playback is equalised. Presets come from the platform effect's
 * own preset table (`numberOfPresets`/`getPresetName`), so the names and
 * curves match what the device's audio HAL ships rather than a hardcoded list.
 *
 * Every platform call is wrapped: some devices/emulators report 0 bands or
 * throw `UnsupportedOperationException`, and equalisation is an enhancement —
 * playback must never fail because the effect is unavailable.
 */
class EqualizerController {

    private var equalizer: Equalizer? = null
    private var sessionId = -1

    val isAvailable: Boolean get() = equalizer != null

    /** Attaches to a player's audio session. Safe to call repeatedly. */
    fun attach(newSessionId: Int) {
        if (newSessionId == sessionId && equalizer != null) return
        detach()
        sessionId = newSessionId
        if (newSessionId == 0) return
        runCatching {
            equalizer = Equalizer(0, newSessionId).apply { enabled = true }
        }.onFailure { Log.w(TAG, "equalizer unavailable: ${it.message}") }
    }

    fun detach() {
        runCatching { equalizer?.release() }
        equalizer = null
    }

    fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
    }

    /** Platform preset names, or an empty list when the effect is unavailable. */
    fun presetNames(): List<String> = runCatching {
        val eq = equalizer ?: return emptyList()
        (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
    }.getOrDefault(emptyList())

    fun currentPreset(): Int = runCatching { equalizer?.currentPreset?.toInt() ?: -1 }.getOrDefault(-1)

    fun applyPreset(index: Int) {
        runCatching { equalizer?.usePreset(index.toShort()) }
    }

    private companion object {
        const val TAG = "EqualizerController"
    }
}
