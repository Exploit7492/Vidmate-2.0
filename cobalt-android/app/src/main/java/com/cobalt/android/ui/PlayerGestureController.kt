package com.cobalt.android.ui

import android.content.Context
import android.media.AudioManager
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * VidMate-style swipe gestures on the video surface: horizontal drag seeks,
 * vertical drag on the left half changes brightness, vertical drag on the right
 * half changes volume. A plain tap (no movement) is passed through untouched so
 * the normal player controls still open.
 *
 * The controller is deliberately separate from the player so the same gestures
 * can be attached to the in-app dialog player and the floating window.
 */
class PlayerGestureController(
    private val context: Context,
    private val window: android.view.Window?,
    private val onSeek: (deltaMillis: Long) -> Unit,
    private val currentPosition: () -> Long,
    private val duration: () -> Long,
    private val onFeedback: (String) -> Unit
) : View.OnTouchListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var downX = 0f
    private var downY = 0f
    private var moved = false
    private var mode = NONE

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; moved = false; mode = NONE
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!moved && (kotlin.math.abs(dx) > TOUCH_SLOP || kotlin.math.abs(dy) > TOUCH_SLOP)) {
                    moved = true
                    mode = if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) SEEK
                    else if (event.x < v.width / 2f) BRIGHTNESS else VOLUME
                }
                if (!moved) return false
                when (mode) {
                    SEEK -> {
                        val total = duration()
                        if (total > 0) {
                            val delta = (dx / v.width * total).toLong()
                            onSeek(delta)
                            onFeedback("${fmt(currentPosition() + delta)} / ${fmt(total)}")
                        }
                    }
                    BRIGHTNESS -> adjustBrightness(dy / v.height)
                    VOLUME -> adjustVolume(dy / v.height)
                }
                downX = event.x; downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val handled = moved
                moved = false; mode = NONE
                return handled
            }
        }
        return false
    }

    private fun adjustBrightness(frac: Float) {
        val attrs = window?.attributes ?: return
        val next = (attrs.screenBrightness.takeIf { it >= 0 } ?: 0.5f) - frac
        attrs.screenBrightness = next.coerceIn(0.05f, 1f)
        window.attributes = attrs
        onFeedback("Brightness ${(attrs.screenBrightness * 100).toInt()}%")
    }

    private fun adjustVolume(frac: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val next = (current - frac * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        onFeedback("Volume $next/$max")
    }

    private fun fmt(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    companion object {
        private const val TOUCH_SLOP = 24f
        private const val NONE = 0
        private const val SEEK = 1
        private const val BRIGHTNESS = 2
        private const val VOLUME = 3
    }
}
