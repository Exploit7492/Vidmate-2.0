package com.cobalt.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cobalt.android.feed.model.FeedItem

/**
 * The Home screen's feed body (Phase 23). Rendered inside a `ComposeView`
 * hosted by `HomeFragment`, which keeps the paste-link resolver + picker
 * flow (Phases 3/4/6) around it — this composable is only the feed surface
 * that replaced the empty `feedContainer` reservation.
 *
 * Layout follows the reference pattern ARCHITECTURE.md's pivot section
 * records (a real feed: labeled sections of tappable thumbnail tiles), not
 * the reference app's code (which is GPLv3 and not copied — see Theme.kt's
 * note).
 */
@Composable
fun HomeFeed(
    items: List<FeedItem>,
    isLoading: Boolean,
    isFromCache: Boolean,
    isResolvingStream: Boolean,
    onItemClick: (FeedItem) -> Unit,
    onDownloadClick: (FeedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        when {
            isLoading && items.isEmpty() -> FeedSkeleton()
            items.isEmpty() -> EmptyFeed()
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.videoId }) { item ->
                    FeedTile(
                        item = item,
                        isResolving = isResolvingStream,
                        onClick = { onItemClick(item) },
                        onDownload = { onDownloadClick(item) }
                    )
                }
            }
        }

        if (isFromCache && items.isNotEmpty()) {
            Text(
                text = "Showing cached feed — no live results right now",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun FeedTile(
    item: FeedItem,
    isResolving: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isResolving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                )
            }
            if (item.durationSeconds > 0) {
                Text(
                    text = formatDuration(item.durationSeconds),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOf(item.channelName, item.viewCountText)
                .filter { it.isNotBlank() }
                .joinToString(" • "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "download",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable(onClick = onDownload)
        )
    }
}

@Composable
private fun FeedSkeleton() {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        repeat(3) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Box(
                    Modifier
                        .width(120.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier.fillMaxWidth().height(14.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth(0.6f).height(12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFeed() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No feed right now — paste a link below to download.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
