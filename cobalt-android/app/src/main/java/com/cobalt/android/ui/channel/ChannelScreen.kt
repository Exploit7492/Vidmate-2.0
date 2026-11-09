package com.cobalt.android.ui.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cobalt.android.feed.model.FeedItem

/**
 * A YouTube channel's video list (H5). Reached from the Home feed when a
 * channel URL is resolved. Rows are the same visual language as Home's tiles;
 * tapping plays inline on Home (the caller routes it back there) and the
 * download action goes through the existing cobalt pipeline.
 */
@Composable
fun ChannelScreen(
    state: ChannelViewModel.UiState,
    onPlay: (FeedItem) -> Unit,
    onDownload: (FeedItem) -> Unit,
    onLoadMore: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            state.channelName.ifBlank { "Channel" },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
        )
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Text(
                state.error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
            state.videos.isEmpty() -> Text(
                "no videos found",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.videos, key = { it.videoId }) { item ->
                    ChannelRow(item, onPlay, onDownload)
                }
                item {
                    Text(
                        "load more",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoadMore() }
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    item: FeedItem,
    onPlay: (FeedItem) -> Unit,
    onDownload: (FeedItem) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onPlay(item) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(item.viewCountText, item.publishedText)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                "download",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onDownload(item) }
            )
        }
    }
}
