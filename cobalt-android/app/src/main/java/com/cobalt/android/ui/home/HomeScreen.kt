package com.cobalt.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cobalt.android.R
import com.cobalt.android.ui.theme.CobaltTheme
import com.cobalt.android.ui.common.DownloadBellIcon
import com.cobalt.android.ui.common.MiniPlayerBar
import com.cobalt.android.ui.common.OfflineScreen
import com.cobalt.android.ui.common.RefreshableFeed
import com.cobalt.android.util.NetworkMonitor
import androidx.compose.ui.platform.LocalContext

/**
 * Home surface, rendered from `HomeFragment`'s `ComposeView`. VidMate-style
 * layout recovered from the decompiled app: a rounded pill search bar at the
 * top, a "Quick download" paste-link card, then a "Recommended" two-column
 * tile grid. Combines the paste-a-link resolver (Phases 3/4/6), the real feed
 * grid ([HomeFeed]) and the inline player overlay ([HomePlayer]).
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlaylist: () -> Unit = {},
    onOpenDownloads: () -> Unit = {}
) {
    CobaltTheme {
        val feed by viewModel.feed.observeAsState(emptyList())
        val isLoadingFeed by viewModel.isLoadingFeed.observeAsState(false)
        val isFromCache by viewModel.isFeedFromCache.observeAsState(false)
        val isResolvingStream by viewModel.isResolvingStream.observeAsState(false)
        val nowPlaying by viewModel.nowPlaying.observeAsState()

        val status by viewModel.statusMessage.observeAsState()
        val isResolving by viewModel.isResolving.observeAsState(false)

        val searchQuery by viewModel.searchQuery.observeAsState()
        val searchResults by viewModel.searchResults
            .observeAsState(com.cobalt.android.feed.model.SearchResults.EMPTY)
        val isSearching by viewModel.isSearching.observeAsState(false)

        // One ExoPlayer for the current item, shown either as a bar pinned to
        // the top (the user keeps browsing) or full screen (the user expanded
        // it). Only one of the two surfaces is composed at a time, which is
        // what makes sharing the single player instance safe.
        val player = rememberFeedPlayer(nowPlaying)
        var expanded by remember(nowPlaying?.videoId) { mutableStateOf(false) }
        val isPlaying = rememberIsPlaying(player)

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // The mini player sits at the very top of the content, above the
            // search results / feed, so playback stays visible while the user
            // scrolls — the same slot Flow puts it in.
            if (nowPlaying != null && player != null && !expanded) {
                val item = nowPlaying!!
                MiniPlayerBar(
                    player = player,
                    title = item.title,
                    subtitle = item.channelName,
                    isPlaying = isPlaying,
                    onTogglePlay = { if (isPlaying) player.pause() else player.play() },
                    onExpand = { expanded = true },
                    onClose = viewModel::stopPlayback,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery != null) {
                // The global header (MainActivity) is the only search field, so
                // this surface shows the header's results when there are any and
                // falls back to the recommendation feed otherwise.
                SearchResultsSection(
                    query = searchQuery.orEmpty(),
                    results = searchResults,
                    isSearching = isSearching,
                    isResolvingStream = isResolvingStream,
                    onBack = viewModel::clearSearch,
                    onItemClick = viewModel::onFeedItemClick,
                    onDownloadClick = viewModel::downloadFromFeed,
                    onChannelClick = { viewModel.openChannel(it.browseUrl) },
                    onPlaylistClick = { viewModel.openPlaylist(it.playlistUrl) }
                )
            } else {
                DownloadsEntryCard(
                    status = status,
                    isResolving = isResolving,
                    onOpenDownloads = onOpenDownloads,
                    onPlaylist = onPlaylist
                )
                SectionHeader("Recommended")
                val context = LocalContext.current
                val online = remember(feed, isLoadingFeed) { NetworkMonitor.isOnline(context) }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        !online && feed.isEmpty() && !isLoadingFeed ->
                            OfflineScreen(onRetry = viewModel::refreshFeed)
                        else ->
                            RefreshableFeed(
                                refreshing = isLoadingFeed,
                                onRefresh = viewModel::refreshFeed
                            ) {
                                HomeFeed(
                                    items = feed,
                                    isLoading = isLoadingFeed,
                                    isFromCache = isFromCache,
                                    isResolvingStream = isResolvingStream,
                                    onItemClick = viewModel::onFeedItemClick,
                                    onDownloadClick = viewModel::downloadFromFeed
                                )
                            }
                    }
                }
            }
        }

        val comments by viewModel.comments.observeAsState(emptyList())
        val isLoadingComments by viewModel.isLoadingComments.observeAsState(false)
        val commentsError by viewModel.commentsError.observeAsState()

        if (nowPlaying != null && player != null && expanded) {
            val item = nowPlaying!!
            var showComments by remember { mutableStateOf(false) }
            LaunchedEffect(showComments, item.videoId) {
                if (showComments) viewModel.loadComments(item.videoId)
            }
            Dialog(
                onDismissRequest = { expanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        PlayerSurface(player = player, modifier = Modifier.fillMaxSize())
                        TextButton(
                            onClick = { expanded = false },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Text("minimise", color = Color.White, fontSize = 14.sp)
                        }
                        TextButton(
                            onClick = { showComments = !showComments },
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                        ) {
                            Text(
                                if (showComments) "hide comments" else "comments",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        if (showComments) {
                            CommentsPanel(
                                comments = comments,
                                isLoading = isLoadingComments,
                                error = commentsError,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.55f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The public-comments panel shown over the inline player (E11). Read-only: the
 * app has no YouTube sign-in, so there is nothing to post with.
 */
@Composable
private fun CommentsPanel(
    comments: List<com.cobalt.android.feed.comments.CommentsParser.Comment>,
    isLoading: Boolean,
    error: String?,
    modifier: Modifier = Modifier
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            item {
                Text(
                    "Comments",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            when {
                isLoading -> item {
                    Text("loading comments…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                error != null -> item {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                comments.isEmpty() -> item {
                    Text("no comments", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                else -> items(comments) { c ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                c.author.ifBlank { "anonymous" },
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                            if (c.publishedTime.isNotBlank()) {
                                Text(
                                    "  ·  ${c.publishedTime}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Text(
                            c.text,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        if (c.likeCount.isNotBlank()) {
                            Text(
                                "♥ ${c.likeCount}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SearchResultsSection(
    query: String,
    results: com.cobalt.android.feed.model.SearchResults,
    isSearching: Boolean,
    isResolvingStream: Boolean,
    onBack: () -> Unit,
    onItemClick: (com.cobalt.android.feed.model.FeedItem) -> Unit,
    onDownloadClick: (com.cobalt.android.feed.model.FeedItem) -> Unit,
    onChannelClick: (com.cobalt.android.feed.model.SearchChannel) -> Unit,
    onPlaylistClick: (com.cobalt.android.feed.model.SearchPlaylist) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "\u2039",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 24.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp)
        )
        Text(
            "Results for \u201C$query\u201D",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
        when {
            isSearching && results.isEmpty -> {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.align(Alignment.Center).size(30.dp)
                )
            }
            results.isEmpty -> {
                Text(
                    "No results for \u201C$query\u201D",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                if (results.channels.isNotEmpty()) {
                    item { SectionHeader("Channels") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(results.channels, key = { it.channelId }) { ch ->
                                SearchChannelChip(ch) { onChannelClick(ch) }
                            }
                        }
                    }
                }
                if (results.playlists.isNotEmpty()) {
                    item { SectionHeader("Playlists") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(results.playlists, key = { it.playlistId }) { pl ->
                                SearchPlaylistCard(pl) { onPlaylistClick(pl) }
                            }
                        }
                    }
                }
                if (results.videos.isNotEmpty()) {
                    item { SectionHeader("Videos") }
                    items(results.videos, key = { it.videoId }) { item ->
                        SearchVideoRow(
                            item = item,
                            isResolving = isResolvingStream,
                            onClick = { onItemClick(item) },
                            onDownload = { onDownloadClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchChannelChip(
    channel: com.cobalt.android.feed.model.SearchChannel,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = channel.avatarUrl,
            contentDescription = channel.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            channel.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (channel.subscriberText.isNotBlank()) {
            Text(
                channel.subscriberText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchPlaylistCard(
    playlist: com.cobalt.android.feed.model.SearchPlaylist,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = playlist.thumbnailUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            playlist.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (playlist.videoCountText.isNotBlank()) {
            Text(
                playlist.videoCountText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SearchVideoRow(
    item: com.cobalt.android.feed.model.FeedItem,
    isResolving: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(140.dp)
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
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(item.channelName, item.viewCountText)
                    .filter { it.isNotBlank() }
                    .joinToString(" \u2022 "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DownloadBellIcon(isResolving = isResolving, onClick = onDownload)
    }
}

@Composable
private fun DownloadsEntryCard(
    status: String?,
    isResolving: Boolean,
    onOpenDownloads: () -> Unit,
    onPlaylist: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Downloads & library",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Queue, alerts and saved files",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            if (isResolving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                TextButton(onClick = onOpenDownloads) { Text("Open", fontSize = 12.sp) }
                TextButton(onClick = onPlaylist) { Text("Playlist", fontSize = 12.sp) }
            }
        }
        if (!status.isNullOrBlank()) {
            Text(
                text = status,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
