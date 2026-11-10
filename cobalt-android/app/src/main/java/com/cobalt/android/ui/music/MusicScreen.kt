package com.cobalt.android.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.media.LocalMediaItem
import com.cobalt.android.music.MusicPlaylist

/**
 * Music player UI (Phase G1 / E10): the device's audio as a searchable list
 * with a mini transport bar pinned at the top. Tapping a row plays it; the
 * transport bar shows the current track and previous/play/next.
 */
@Composable
fun MusicScreen(
    state: MusicViewModel.UiState,
    positionMs: Long = 0L,
    onSearch: (String) -> Unit,
    onPlay: (Int) -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPlaylist: (MusicPlaylist) -> Unit = {},
    onDeletePlaylist: (Long) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onSetSleepTimer: (Int) -> Unit = {},
    onShare: (LocalMediaItem) -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp)
    ) {
        MiniTransport(state.current, state.playing, onToggle, onNext, onPrevious)
        val lyric = LrcParser.lineAt(state.lyrics, positionMs)
        if (lyric != null) {
            Text(
                lyric,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = state.search,
            onValueChange = onSearch,
            singleLine = true,
            label = { Text("search music") },
            modifier = Modifier.fillMaxWidth()
        )
        PlaylistsRow(
            playlists = state.playlists,
            onPlay = onPlayPlaylist,
            onDelete = onDeletePlaylist,
            onCreate = onCreatePlaylist
        )
        SleepTimerRow(state.sleepTimerEndsAt, onSetSleepTimer)
        Spacer(Modifier.height(10.dp))
        var groupMode by remember { mutableStateOf(0) } // 0 = tracks, 1 = albums, 2 = artists
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { groupMode = (groupMode + 1) % 3 }) {
                Text(
                    when (groupMode) { 1 -> "by album"; 2 -> "by artist"; else -> "all tracks" },
                    fontSize = 12.sp
                )
            }
        }
        val visible = state.visible
        if (state.loading) {
            Text("scanning…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            return
        }
        if (visible.isEmpty()) {
            Text("no music on this device", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            return
        }
        if (groupMode == 0) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(visible, key = { _, it -> it.uri }) { index, track ->
                    TrackRow(track, selected = track.uri == state.current?.uri,
                        onClick = { onPlay(index) }, onLongClick = { onShare(track) })
                }
            }
        } else {
            val groups = if (groupMode == 1) MusicGrouping.byAlbum(visible, state.tags)
            else MusicGrouping.byArtist(visible, state.tags)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                groups.forEach { group ->
                    item(key = "h_" + group.title) {
                        Text(
                            group.title + if (group.subtitle.isNotBlank()) " · ${group.subtitle}" else "",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    itemsIndexed(group.tracks, key = { _, it -> it.uri }) { _, track ->
                        TrackRow(track, selected = track.uri == state.current?.uri,
                            onClick = { onPlay(visible.indexOfFirst { it.uri == track.uri }) },
                            onLongClick = { onShare(track) })
                    }
                }
            }
        }
    }
}

/** Sleep-timer control (G7): tap to pick a duration; shows the remaining time. */
@Composable
private fun SleepTimerRow(endsAt: Long, onSet: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val options = listOf(0, 5, 15, 30, 60, 90)
    if (picking) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    options.forEach { minutes ->
                        Text(
                            if (minutes == 0) "Off" else "$minutes minutes",
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSet(minutes); picking = false }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("close") } }
        )
    }
    val remaining = if (endsAt > System.currentTimeMillis())
        " · stops in ${(endsAt - System.currentTimeMillis()) / 60000}m" else ""
    Text(
        "sleep timer$remaining",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        modifier = Modifier
            .padding(top = 4.dp)
            .clickable { picking = true }
    )
}

/**
 * The playlists strip (G4): one chip per saved playlist (tap plays, long-press
 * deletes) plus a "+ save" chip that prompts for a name and stores the tracks
 * currently visible in the list.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlaylistsRow(
    playlists: List<MusicPlaylist>,
    onPlay: (MusicPlaylist) -> Unit,
    onDelete: (Long) -> Unit,
    onCreate: (String) -> Unit
) {
    var naming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (naming) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text("Save playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) onCreate(newName)
                    newName = ""
                    naming = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { naming = false; newName = "" }) { Text("Cancel") }
            }
        )
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(playlists, key = { _, it -> it.id }) { _, pl ->
                Text(
                    pl.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .combinedClickable(
                            onClick = { onPlay(pl) },
                            onLongClick = { onDelete(pl.id) }
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        TextButton(onClick = { naming = true }) { Text("+ save", fontSize = 12.sp) }
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    track: LocalMediaItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp)
    ) {
        Text(track.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}
