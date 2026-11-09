package com.cobalt.android.ui.downloads

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.download.DownloadRecord
import com.cobalt.android.download.DownloadStatus

/**
 * Phase 25: the download queue, migrated from the XML `DownloadQueueSheet`
 * (RecyclerView + `DownloadAdapter`) to Compose. Same two tabs (active /
 * history), same per-status rendering (queued / downloading / converting /
 * complete / failed / network error), same actions (retry / cancel / open).
 *
 * The skeleton-pulse placeholder the XML sheet showed before Room's first
 * emission is replaced by a plain empty state — Compose's `LazyColumn`
 * renders instantly and the LiveData arrives within the same frame in
 * practice, so the pulsing-skeleton machinery (Phase 17) isn't worth porting.
 */
@Composable
fun DownloadsScreen(
    activeRecords: List<DownloadRecord>,
    historyRecords: List<DownloadRecord>,
    onRetry: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit,
    onPlay: (DownloadRecord) -> Unit,
    onClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val records = if (selectedTab == 0) activeRecords else historyRecords

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("active") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("history") })
        }

        if (selectedTab == 1 && records.isNotEmpty()) {
            Text(
                text = "clear finished",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(onClick = onClear)
            )
        }

        if (selectedTab == 0 && records.isNotEmpty()) {
            DownloadWheel(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp).size(18.dp)
            )
        }

        if (records.isEmpty()) {
            Text(
                text = if (selectedTab == 0) "no active downloads" else "nothing here yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                items(records, key = { it.id }) { record ->
                    DownloadRow(record, onRetry, onCancel, onPlay)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    record: DownloadRecord,
    onRetry: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit,
    onPlay: (DownloadRecord) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = record.filename.ifBlank { "downloading…" } +
                if (record.transcodeProfileLabel.isNotBlank()) " · ${record.transcodeProfileLabel}" else "",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))

        when (record.status) {
            DownloadStatus.QUEUED -> {
                StatusText("queued")
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                RowAction("cancel") { onCancel(record) }
            }
            DownloadStatus.DOWNLOADING -> {
                val pct = if (record.totalBytes > 0)
                    (record.bytesDownloaded * 100 / record.totalBytes).toInt() else 0
                val label = if (record.totalBytes > 0) {
                    "%.1f / %.1f MB".format(record.bytesDownloaded / 1_048_576.0, record.totalBytes / 1_048_576.0)
                } else {
                    "%.1f MB".format(record.bytesDownloaded / 1_048_576.0)
                }
                StatusText(label + speedLabel(record))
                if (record.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
                RowAction("cancel") { onCancel(record) }
            }
            DownloadStatus.CONVERTING -> {
                val pct = if (record.totalBytes > 0)
                    (record.bytesDownloaded * 100 / record.totalBytes).toInt() else 0
                StatusText(if (record.totalBytes > 0) "converting… $pct%" else "converting…")
                if (record.totalBytes > 0) {
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
            DownloadStatus.COMPLETE -> {
                val mb = if (record.totalBytes > 0) "%.1f MB · ".format(record.totalBytes / 1_048_576.0) else ""
                StatusText("${mb}saved")
                RowAction("open") { onPlay(record) }
            }
            DownloadStatus.FAILED -> {
                StatusText("failed")
                RowAction("retry") { onRetry(record) }
            }
            DownloadStatus.FAILED_NETWORK -> {
                StatusText("network error")
                RowAction("retry") { onRetry(record) }
            }
            DownloadStatus.CANCELLED -> {
                StatusText("cancelled")
                RowAction("retry") { onRetry(record) }
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
}

@Composable
private fun RowAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp).clickable(onClick = onClick)
    )
}

/** Human-readable transfer speed, or "" when the engine has not measured one. */
private fun speedLabel(r: DownloadRecord): String =
    if (r.speedBps <= 0) "" else " · %.1f MB/s".format(r.speedBps / 1_048_576.0)
