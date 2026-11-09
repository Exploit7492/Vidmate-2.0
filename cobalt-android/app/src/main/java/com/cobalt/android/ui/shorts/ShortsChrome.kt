package com.cobalt.android.ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cobalt.android.R
import com.cobalt.android.shorts.model.ShortItem

/**
 * Phase 24: the per-item Shorts chrome, in Compose. Replaces the previous
 * XML chrome whose like/save/share were literal Android-API-1 stock system
 * drawables (`android.R.drawable.btn_star_big_off`, `ic_menu_save`,
 * `ic_menu_share`) with no avatar, no username styling, no counts — the
 * exact regression the user reported. This is the "per-item chrome moving
 * to Compose" scope ARCHITECTURE.md's Phase 24 defines; the ExoPlayer /
 * preload lifecycle in `ShortsFragment` (Phase 2/18) is deliberately
 * untouched.
 *
 * Rendered into each ViewHolder's `chromeCompose` (`item_short_video.xml`),
 * layered over the `PlayerView` that the fragment attaches the shared
 * player to.
 */
@Composable
fun ShortsChrome(
    item: ShortItem,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Right-hand vertical action rail, bottom-aligned like a real
        // short-form player.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Avatar(item.thumbnailUrl, item.authorName)
            Spacer(Modifier.height(20.dp))
            ActionIcon(
                iconRes = if (item.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline,
                label = if (item.isLiked) "liked" else "like",
                tint = if (item.isLiked) Color(0xFFED2236) else Color.White,
                onClick = onLikeClick
            )
            Spacer(Modifier.height(20.dp))
            ActionIcon(R.drawable.ic_download, "save", Color.White, onSaveClick)
            Spacer(Modifier.height(20.dp))
            ActionIcon(R.drawable.ic_share, "share", Color.White, onShareClick)
        }

        // Bottom-left caption block: channel, title, duration.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(start = 16.dp, bottom = 32.dp, end = 8.dp)
        ) {
            Text(
                text = "@${item.authorName.ifBlank { "unknown" }}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.durationSeconds > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatDuration(item.durationSeconds),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun Avatar(url: String, name: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = url,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape)
        )
    }
}

@Composable
private fun ActionIcon(iconRes: Int, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
