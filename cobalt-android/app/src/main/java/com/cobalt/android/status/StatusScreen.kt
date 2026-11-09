package com.cobalt.android.status

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WhatsApp status saver screen (E8). Lists media found in the readable status
 * directories and offers "save" per item. When nothing is found it explains
 * why (Android 11+ scoping) rather than showing a bare empty list — see
 * [StatusRepository]'s KDoc.
 */
@Composable
fun StatusScreen(
    state: StatusViewModel.UiState,
    onRefresh: () -> Unit,
    onSave: (StatusRepository.StatusFile) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "WhatsApp statuses",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }
        state.status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
        Spacer(Modifier.padding(top = 8.dp))

        when {
            state.loading -> Text("scanning…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            state.items.isEmpty() -> Text(
                "No statuses found.\n\nOn Android 11 and newer WhatsApp keeps its " +
                    "status files in private storage that other apps cannot read, so " +
                    "this feature only works on Android 10 or below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.items, key = { it.file.absolutePath }) { item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            Text(
                                if (item.isVideo) "video" else "image",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "save",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { onSave(item) }
                        )
                    }
                }
            }
        }
    }
}
