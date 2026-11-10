package com.cobalt.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.cobalt.android.feed.model.FeedItem
import com.cobalt.android.feed.model.StreamKind

/**
 * Playback split into a player holder and a render surface so one ExoPlayer
 * instance can be shown at two sizes — a small bar while the user keeps
 * browsing and a full-screen surface once they expand it — without a second
 * decoder running. Only one [PlayerSurface] is composed at a time (the bar
 * and the full surface are mutually exclusive), which is what makes reusing
 * the single player safe: an ExoPlayer can only be attached to one PlayerView.
 */
@Composable
fun rememberFeedPlayer(item: FeedItem?): ExoPlayer? {
    val context = LocalContext.current
    val streamUrl = item?.streamUrl ?: return null
    val player = remember(item.videoId, streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(buildMediaItem(item, streamUrl))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    DisposableEffect(item.videoId, streamUrl) {
        onDispose { player.release() }
    }
    return player
}

/** Renders [player] into a PlayerView. Caller decides the size/placement. */
@Composable
fun PlayerSurface(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    useController: Boolean = true
) {
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.useController = useController
                this.player = player
            }
        },
        update = {
            it.player = player
            it.useController = useController
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Full-size playback for a single [item], kept for callers that want a
 * self-contained player (it builds and releases its own ExoPlayer).
 */
@Composable
fun HomePlayer(item: FeedItem, modifier: Modifier = Modifier) {
    val player = rememberFeedPlayer(item)
    if (player == null) {
        Box(
            modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("preparing stream…", color = Color.White, fontSize = 13.sp)
        }
        return
    }
    PlayerSurface(player, modifier)
}


/**
 * Mirrors [player]'s playing/paused state into Compose so the mini bar's
 * play/pause icon reflects reality (including pauses triggered from the
 * full-screen PlayerView controls).
 */
@Composable
fun rememberIsPlaying(player: ExoPlayer?): Boolean {
    var playing by remember(player) { mutableStateOf(player?.isPlaying ?: false) }
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        playing = player.isPlaying
        onDispose { player.removeListener(listener) }
    }
    return playing
}

private fun buildMediaItem(item: FeedItem, streamUrl: String): MediaItem = when (item.streamKind) {
    StreamKind.HLS -> MediaItem.Builder()
        .setUri(streamUrl)
        .setMimeType(MimeTypes.APPLICATION_M3U8)
        .build()
    StreamKind.DASH -> MediaItem.Builder()
        .setUri(streamUrl)
        .setMimeType(MimeTypes.APPLICATION_MPD)
        .build()
    else -> MediaItem.fromUri(streamUrl)
}
