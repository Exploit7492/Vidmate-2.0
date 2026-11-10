package com.cobalt.android.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.download.DownloadRecord
import com.cobalt.android.util.StorageStats

/**
 * "File info" sheet for a completed download — name, size, type, source URL,
 * save location and when it finished, the same set VidMate shows when you tap
 * a finished item's info button.
 */
@Composable
fun FileDetailDialog(record: DownloadRecord, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(record.filename.ifBlank { "file" }) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("Size", if (record.totalBytes > 0) StorageStats.formatBytes(record.totalBytes) else "unknown")
                DetailRow("Type", record.mimeType.ifBlank { "unknown" })
                DetailRow("Status", record.status.name.lowercase())
                if (record.transcodeProfileLabel.isNotBlank()) DetailRow("Converted", record.transcodeProfileLabel)
                DetailRow("Source", record.originalUrl.ifBlank { record.cobaltUrl })
                if (record.mediaStoreUriString.isNotBlank()) DetailRow("Saved as", record.mediaStoreUriString)
                DetailRow("Parts", if (record.rangeSupported) "segmented" else "single stream")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("close") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$label: ", fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
        Text(value, fontSize = 12.sp)
    }
}
