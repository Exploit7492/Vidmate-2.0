package com.cobalt.android.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cobalt.android.R

/**
 * The floating "pop-up" video window (VidMate's signature overlay player). A
 * small always-on-top [PlayerView] added straight to the window manager with
 * `TYPE_APPLICATION_OVERLAY`, so playback continues over other apps — distinct
 * from Android's own picture-in-picture, which is a whole-task mode and hides
 * the app's controls.
 *
 * Requires `SYSTEM_ALERT_WINDOW`; the caller checks
 * `Settings.canDrawOverlays()` (see [PermissionGuides]) before calling [show],
 * and falls back to the in-app dialog player otherwise. The window owns its own
 * [ExoPlayer] so it is independent of the dialog player's lifecycle.
 */
class FloatingPlayerWindow(private val context: Context) {

    private var player: ExoPlayer? = null
    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val isShowing: Boolean get() = root != null

    fun show(uri: String, startPositionMs: Long = 0L, mimeType: String? = null) {
        if (isShowing) return
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.window_floating_player, null)
        val exo = ExoPlayer.Builder(context).build()
        player = exo
        view.findViewById<PlayerView>(R.id.floatingPlayerView).player = exo
        view.findViewById<ImageButton>(R.id.floatingClose).setOnClickListener { dismiss() }

        val item = MediaItem.Builder().setUri(uri)
        if (mimeType != null) item.setMimeType(mimeType)
        exo.setMediaItem(item.build())
        if (startPositionMs > 0) exo.seekTo(startPositionMs)
        exo.prepare()
        exo.playWhenReady = true

        val type = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 160
        }
        params = lp
        root = view
        runCatching { windowManager.addView(view, lp) }
        makeDraggable(view, lp)
    }

    /** Drag the window by its whole surface (the PlayerView passes through taps). */
    private fun makeDraggable(view: View, lp: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        view.findViewById<View>(R.id.floatingDragHandle).setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (event.rawX - touchX).toInt()
                    lp.y = startY + (event.rawY - touchY).toInt()
                    runCatching { windowManager.updateViewLayout(view, lp) }
                    true
                }
                else -> false
            }
        }
    }

    fun dismiss() {
        runCatching { player?.release() }
        player = null
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        params = null
    }
}
