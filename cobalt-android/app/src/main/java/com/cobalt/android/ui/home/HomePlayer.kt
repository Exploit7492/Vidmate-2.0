package com.cobalt.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
 * Inline Home playback (Phase 23). A real ExoPlayer/Media3 player — the same
 * stack the Shorts feed uses — rather than the download-then-open-externally
 * path, so tapping a feed tile streams it immediately.
 *
 * One player per [item]; it is built in a `remember` and released in a
 * `DisposableEffect` keyed on the item, so switching items (or leaving Home)
 * tears the old player down instead of leaking it. This mirrors the Shorts
 * feed's single-live-player discipline, applied to the one item Home is
 * currently playing.
 *
 * A null [item.streamUrl] renders a "resolving" placeholder rather than an
 * empty surface — the ViewModel resolves on demand and re-emits the item
 * with a stream URL once ready.
 */
@Composable
fun HomePlayer(item: FeedItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val streamUrl = item.streamUrl

    if (streamUrl == null) {
        Box(
            modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("preparing stream…", color = Color.White, fontSize = 13.sp)
        }
        return
    }

    val player = remember(item.videoId, streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(buildMediaItem(item, streamUrl))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(item.videoId, streamUrl) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                this.player = player
            }
        },
        modifier = modifier.fillMaxSize()
    )
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
