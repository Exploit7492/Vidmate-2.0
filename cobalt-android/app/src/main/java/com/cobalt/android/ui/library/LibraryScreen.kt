package com.cobalt.android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.R
import com.cobalt.android.download.DownloadRecord
import com.cobalt.android.download.DownloadStatus
import com.cobalt.android.ui.history.HistoryRow

/**
 * Library tab — VidMate's "My Files" surface: everything the user has
 * downloaded, saved, watched or liked, grouped into four tabs. Mirrors the
 * decompiled VidMate activity set (`MyFilesActivity`, `LocalVideoActivity`,
 * `favhis.BookmarkActivity`) collapsed into one modern Compose screen.
 */
private enum class LibraryTab(val label: String) {
    DOWNLOADS("Downloads"),
    FILES("Files"),
    HISTORY("History"),
    LIKED("Liked")
}

@Composable
fun LibraryScreen(
    activeDownloads: List<DownloadRecord>,
    allDownloads: List<DownloadRecord>,
    historyRows: List<HistoryRow>,
    likedRows: List<HistoryRow>,
    onRetry: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit,
    onPlay: (DownloadRecord) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = LibraryTab.entries

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            text = "Library",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
        )
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { i, t ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(t.label, fontSize = 13.sp) }
                )
            }
        }

        when (tabs[tab]) {
            LibraryTab.DOWNLOADS -> DownloadsTab(activeDownloads, allDownloads, onRetry, onCancel, onPlay)
            LibraryTab.FILES -> FilesTab(
                allDownloads.filter { it.status == DownloadStatus.COMPLETE }, onPlay
            )
            LibraryTab.HISTORY -> UrlRows(historyRows, "nothing watched or downloaded yet", onOpenUrl)
            LibraryTab.LIKED -> UrlRows(likedRows, "no liked Shorts yet", onOpenUrl)
        }
    }
}

@Composable
private fun DownloadsTab(
    active: List<DownloadRecord>,
    all: List<DownloadRecord>,
    onRetry: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit,
    onPlay: (DownloadRecord) -> Unit
) {
    val completed = all.filter {
        it.status == DownloadStatus.COMPLETE ||
            it.status == DownloadStatus.FAILED ||
            it.status == DownloadStatus.FAILED_NETWORK
    }
    if (active.isEmpty() && completed.isEmpty()) {
        EmptyState("no downloads yet", "Paste a link on Home to start one")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (active.isNotEmpty()) {
            item { SectionHeader("Active") }
            items(active, key = { "a${it.id}" }) { r -> DownloadCard(r, onRetry, onCancel, onPlay) }
        }
        if (completed.isNotEmpty()) {
            item { SectionHeader("Completed") }
            items(completed, key = { "c${it.id}" }) { r -> DownloadCard(r, onRetry, onCancel, onPlay) }
        }
    }
}

@Composable
private fun FilesTab(files: List<DownloadRecord>, onPlay: (DownloadRecord) -> Unit) {
    if (files.isEmpty()) {
        EmptyState("no saved files yet", "Completed downloads appear here")
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files, key = { "f${it.id}" }) { r ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPlay(r) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        r.filename.ifBlank { "file" },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        sizeLabel(r) + if (r.mimeType.isNotBlank()) " · ${r.mimeType.substringAfterLast('/')}" else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = "play",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun UrlRows(rows: List<HistoryRow>, empty: String, onOpen: (String) -> Unit) {
    if (rows.isEmpty()) {
        EmptyState(empty, null)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rows, key = { it.id }) { row ->
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpen(row.sourceUrl) }
                    .padding(12.dp)
            ) {
                Text(
                    row.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(row.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DownloadCard(
    record: DownloadRecord,
    onRetry: (DownloadRecord) -> Unit,
    onCancel: (DownloadRecord) -> Unit,
    onPlay: (DownloadRecord) -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            record.filename.ifBlank { "downloading…" } +
                if (record.transcodeProfileLabel.isNotBlank()) " · ${record.transcodeProfileLabel}" else "",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        when (record.status) {
            DownloadStatus.QUEUED -> {
                StatusLine("queued", MaterialTheme.colorScheme.onSurfaceVariant)
                IndeterminateBar()
                Action("cancel") { onCancel(record) }
            }
            DownloadStatus.DOWNLOADING -> {
                val pct = pct(record)
                StatusLine(progressLabel(record) + speedLabel(record), MaterialTheme.colorScheme.primary)
                Bar(pct)
                Action("cancel") { onCancel(record) }
            }
            DownloadStatus.CONVERTING -> {
                StatusLine("converting… ${pct(record)}%", MaterialTheme.colorScheme.primary)
                Bar(pct(record))
            }
            DownloadStatus.COMPLETE -> {
                StatusLine("${sizeLabel(record)} · saved", SuccessGreen)
                Action("open") { onPlay(record) }
            }
            DownloadStatus.FAILED -> {
                StatusLine("failed", MaterialTheme.colorScheme.error)
                Action("retry") { onRetry(record) }
            }
            DownloadStatus.FAILED_NETWORK -> {
                StatusLine("network error", MaterialTheme.colorScheme.error)
                Action("retry") { onRetry(record) }
            }
            DownloadStatus.CANCELLED -> {
                StatusLine("cancelled", MaterialTheme.colorScheme.onSurfaceVariant)
                Action("retry") { onRetry(record) }
            }
        }
    }
}

private val SuccessGreen = androidx.compose.ui.graphics.Color(0xFF37AA42)

private fun pct(r: DownloadRecord): Int =
    if (r.totalBytes > 0) (r.bytesDownloaded * 100 / r.totalBytes).toInt().coerceIn(0, 100) else 0

private fun progressLabel(r: DownloadRecord): String =
    if (r.totalBytes > 0)
        "%.1f / %.1f MB".format(r.bytesDownloaded / 1_048_576.0, r.totalBytes / 1_048_576.0)
    else "%.1f MB".format(r.bytesDownloaded / 1_048_576.0)

private fun sizeLabel(r: DownloadRecord): String =
    if (r.totalBytes > 0) "%.1f MB".format(r.totalBytes / 1_048_576.0) else "saved"

@Composable
private fun StatusLine(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, color = color, fontSize = 12.sp)
}

@Composable
private fun Bar(pct: Int) {
    LinearProgressIndicator(
        progress = { pct / 100f },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
private fun IndeterminateBar() {
    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp).clickable(onClick = onClick)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyState(title: String, hint: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

/** Human-readable transfer speed, or "" when the engine has not measured one. */
private fun speedLabel(r: DownloadRecord): String =
    if (r.speedBps <= 0) "" else " · %.1f MB/s".format(r.speedBps / 1_048_576.0)
