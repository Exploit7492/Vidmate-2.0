package com.cobalt.android.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.media.LocalMediaItem

/**
 * Music player UI (Phase G1 / E10): the device's audio as a searchable list
 * with a mini transport bar pinned at the top. Tapping a row plays it; the
 * transport bar shows the current track and previous/play/next.
 */
@Composable
fun MusicScreen(
    state: MusicViewModel.UiState,
    onSearch: (String) -> Unit,
    onPlay: (Int) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp)
    ) {
        MiniTransport(state.current, state.playing, onToggle, onNext, onPrevious)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.search,
            onValueChange = onSearch,
            singleLine = true,
            label = { Text("search music") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        val visible = state.visible
        if (state.loading) {
            Text("scanning…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            return
        }
        if (visible.isEmpty()) {
            Text("no music on this device", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(visible, key = { _, it -> it.uri }) { index, track ->
                TrackRow(track, selected = track.uri == state.current?.uri) { onPlay(index) }
            }
        }
    }
}

@Composable
private fun MiniTransport(
    current: LocalMediaItem?,
    playing: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                current?.displayName ?: "Nothing playing",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (current != null) {
                Text(
                    "audio · %.1f MB".format(current.sizeBytes / 1_048_576.0),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
        TextButton(onClick = onPrevious) { Text("⏮") }
        TextButton(onClick = onToggle) { Text(if (playing) "⏸" else "▶") }
        TextButton(onClick = onNext) { Text("⏭") }
    }
}

@Composable
private fun TrackRow(track: LocalMediaItem, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp)
    ) {
        Text(track.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}
