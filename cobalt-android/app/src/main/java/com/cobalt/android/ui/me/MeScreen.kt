package com.cobalt.android.ui.me

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.R

/**
 * Me tab — VidMate's profile/settings hub. A gradient profile header with
 * download/watched/liked counters over a list of navigation rows
 * (My Downloads, History & Likes, Settings, About).
 */
@Composable
fun MeScreen(
    downloadCount: Int,
    historyCount: Int,
    likedCount: Int,
    versionName: String,
    onDownloads: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onVault: () -> Unit,
    onMediaLibrary: () -> Unit,
    onMusic: () -> Unit,
    onStatus: () -> Unit,
    onGames: () -> Unit,
    onCheckUpdates: () -> Unit,
    onBrowser: () -> Unit,
    onAbout: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(downloadCount, historyCount, likedCount, versionName)

        Spacer(Modifier.height(12.dp))
        MeRow(R.drawable.ic_folder, "My Downloads", "saved videos & audio", onDownloads)
        MeRow(R.drawable.ic_nav_library, "Vault", "private, passcode-locked", onVault)
        MeRow(R.drawable.ic_folder, "Media library", "video & audio on this device", onMediaLibrary)
        MeRow(R.drawable.ic_nav_shorts, "Music", "play your audio library", onMusic)
        MeRow(R.drawable.ic_folder, "Status saver", "save WhatsApp statuses", onStatus)
        MeRow(R.drawable.ic_search, "Games", "browser games hub", onGames)
        MeRow(R.drawable.ic_history, "History & Likes", "what you've watched", onHistory)
        MeRow(R.drawable.ic_nav_home, "Browser", "browse & sniff media", onBrowser)
        MeRow(R.drawable.ic_settings, "Settings", "theme, format, instances", onSettings)
        MeRow(R.drawable.ic_nav_home, "Check for updates", "in-app update", onCheckUpdates)
        MeRow(R.drawable.ic_nav_me, "About", "version $versionName", onAbout)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileHeader(
    downloadCount: Int,
    historyCount: Int,
    likedCount: Int,
    versionName: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A2A4A), MaterialTheme.colorScheme.background)
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("C", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Cobalt",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "v$versionName · cobalt.tools client",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Stat(downloadCount, "downloads")
            Stat(historyCount, "watched")
            Stat(likedCount, "liked")
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
private fun MeRow(iconRes: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
}
