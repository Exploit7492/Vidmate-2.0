package com.cobalt.android.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cobalt.android.R
import com.cobalt.android.ui.theme.CobaltTheme

/**
 * Home surface, rendered from `HomeFragment`'s `ComposeView`. VidMate-style
 * layout recovered from the decompiled app: a rounded pill search bar at the
 * top, a "Quick download" paste-link card, then a "Recommended" two-column
 * tile grid. Combines the paste-a-link resolver (Phases 3/4/6), the real feed
 * grid ([HomeFeed]) and the inline player overlay ([HomePlayer]).
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    CobaltTheme {
        val feed by viewModel.feed.observeAsState(emptyList())
        val isLoadingFeed by viewModel.isLoadingFeed.observeAsState(false)
        val isFromCache by viewModel.isFeedFromCache.observeAsState(false)
        val isResolvingStream by viewModel.isResolvingStream.observeAsState(false)
        val nowPlaying by viewModel.nowPlaying.observeAsState()

        val linkInput by viewModel.linkInput.observeAsState("")
        val status by viewModel.statusMessage.observeAsState()
        val isResolving by viewModel.isResolving.observeAsState(false)

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            SearchBar(value = linkInput, onValueChange = viewModel::onLinkInputChanged, onSubmit = viewModel::onSubmit)
            QuickDownloadCard(
                value = linkInput,
                status = status,
                isResolving = isResolving,
                onValueChange = viewModel::onLinkInputChanged,
                onSubmit = viewModel::onSubmit
            )
            SectionHeader("Recommended")
            Box(Modifier.weight(1f).fillMaxWidth()) {
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

        nowPlaying?.let { item ->
            Dialog(
                onDismissRequest = viewModel::stopPlayback,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        HomePlayer(item = item, modifier = Modifier.fillMaxSize())
                        TextButton(
                            onClick = viewModel::stopPlayback,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Text("close", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(14.dp))
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "Search or paste a link",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            }
        )
        Text(
            "Paste",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(onClick = onSubmit)
                .padding(horizontal = 14.dp)
        )
    }
}

@Composable
private fun QuickDownloadCard(
    value: String,
    status: String?,
    isResolving: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
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
            Column(Modifier.weight(1f)) {
                Text(
                    "Quick download",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Paste a link from YouTube, TikTok, Instagram…",
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
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = !isResolving,
            singleLine = true,
            placeholder = { Text("https://…", fontSize = 13.sp) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.Button(
            onClick = onSubmit,
            enabled = !isResolving && value.isNotBlank(),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isResolving) "Resolving…" else "Download")
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
