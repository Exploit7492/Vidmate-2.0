package com.cobalt.android.ui.explore

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cobalt.android.feed.model.FeedItem

/**
 * Explore (Phase "use Innertube fully"): search results, trending, and
 * YouTube-Music discovery. Everything is a public InnerTube read, so it all
 * works with no YouTube account.
 */
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onOpen: (FeedItem) -> Unit,
    onDownload: (FeedItem) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.tab == ExploreViewModel.Tab.SEARCH,
                onClick = { viewModel.selectTab(ExploreViewModel.Tab.SEARCH) },
                label = { Text("Search") }
            )
            FilterChip(
                selected = state.tab == ExploreViewModel.Tab.TRENDING,
                onClick = { viewModel.selectTab(ExploreViewModel.Tab.TRENDING) },
                label = { Text("Trending") }
            )
            FilterChip(
                selected = state.tab == ExploreViewModel.Tab.MUSIC,
                onClick = { viewModel.selectTab(ExploreViewModel.Tab.MUSIC) },
                label = { Text("Music") }
            )
        }

        when (state.tab) {
            ExploreViewModel.Tab.SEARCH -> SearchTab(state, viewModel, onOpen, onDownload)
            ExploreViewModel.Tab.TRENDING -> VideoList(state.trending, state.trendingLoading, onOpen, onDownload)
            ExploreViewModel.Tab.MUSIC -> VideoList(state.music, state.musicLoading, onOpen, onDownload)
        }
    }
}

@Composable
private fun SearchTab(
    state: ExploreViewModel.UiState,
    viewModel: ExploreViewModel,
    onOpen: (FeedItem) -> Unit,
    onDownload: (FeedItem) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (state.query.isEmpty()) {
                        Text("Search YouTube", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                    inner()
                }
            }
        )
        Text(
            "go",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            modifier = Modifier.clickable { viewModel.search() }
        )
    }

    state.estimatedResults?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    when {
        state.searchLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.searchError != null -> Text(
            state.searchError,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
        )
        else -> VideoList(state.searchResults, false, onOpen, onDownload, onLoadMore = viewModel::searchMore)
    }
}

@Composable
private fun VideoList(
    items: List<FeedItem>,
    loading: Boolean,
    onOpen: (FeedItem) -> Unit,
    onDownload: (FeedItem) -> Unit,
    onLoadMore: (() -> Unit)? = null
) {
    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.videoId }) { item -> VideoRow(item, onOpen, onDownload) }
        if (onLoadMore != null && items.isNotEmpty()) {
            item {
                Text(
                    "load more",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().clickable { onLoadMore() }.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun VideoRow(item: FeedItem, onOpen: (FeedItem) -> Unit, onDownload: (FeedItem) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onOpen(item) }, verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
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
                listOf(item.channelName, item.viewCountText, item.publishedText)
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
