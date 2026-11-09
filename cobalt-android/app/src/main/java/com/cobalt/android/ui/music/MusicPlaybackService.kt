package com.cobalt.android.ui.music

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background music playback (Phase C4).
 *
 * A [MediaSessionService] owns the audio ExoPlayer, so music keeps playing when
 * the app is backgrounded or the screen is off, and the transport is controllable
 * from the notification / lock screen / headset buttons. media3 provides the
 * media-notification and the media-button routing from the [MediaSession]; we
 * only have to build the player and hand it a session.
 *
 * `setAudioAttributes(..., handleAudioFocus = true)` is what makes the player
 * pause for calls and duck for navigation prompts, rather than talking over them.
 */
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
