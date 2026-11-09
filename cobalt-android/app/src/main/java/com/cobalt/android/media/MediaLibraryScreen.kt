package com.cobalt.android.media

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Local media library (Phase D4/D5) with sort (D3) and search (D6).
 *
 * Two tabs' worth of content in one screen — video and audio from the device —
 * filtered by a kind chip row, sorted by a sort chip row, and searched by a
 * text field. Rows tap through to [onOpen], which plays the item.
 */
@Composable
fun MediaLibraryScreen(
    state: MediaLibraryViewModel.UiState,
    onSearch: (String) -> Unit,
    onFilter: (LocalMediaQuery.KindFilter) -> Unit,
    onSort: (LocalMediaQuery.Sort) -> Unit,
    onOpen: (LocalMediaItem) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp)
    ) {
        OutlinedTextField(
            value = state.search,
            onValueChange = onSearch,
            singleLine = true,
            label = { Text("search files") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = LocalMediaQuery.KindFilter.values().map { it.name.lowercase() to it },
            selectedIndex = LocalMediaQuery.KindFilter.values().indexOf(state.filter),
            onSelect = onFilter
        )
        Spacer(Modifier.height(8.dp))
        ChipRow(
            options = LocalMediaQuery.Sort.values().map { it.name.lowercase().replace('_', ' ') to it },
            selectedIndex = LocalMediaQuery.Sort.values().indexOf(state.sort),
            onSelect = onSort
        )
        Spacer(Modifier.height(10.dp))

        val visible = state.visible
        if (state.loading) {
            Text("scanning…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            return
        }
        if (visible.isEmpty()) {
            Text("no media found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.uri }) { item -> MediaRow(item, onOpen) }
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<Pair<String, T>>,
    selectedIndex: Int,
    onSelect: (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(options) { i, (label, value) ->
            FilterChip(
                selected = i == selectedIndex,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun MediaRow(item: LocalMediaItem, onOpen: (LocalMediaItem) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(item) }
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            item.displayName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            "${item.kind.name.lowercase()} · %.1f MB".format(item.sizeBytes / 1_048_576.0),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
