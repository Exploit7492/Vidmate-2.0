package com.cobalt.android.ui.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cobalt.android.R
import com.cobalt.android.util.SettingsRepository.DownloadFormatPreference
import com.cobalt.android.util.SettingsRepository.ThemeMode

/**
 * Settings, reached from the Me tab (VidMate's "Settings" row). Every control
 * writes through [SettingsViewModel] to the same `SettingsRepository` as
 * before; writes are immediate.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onBatteryClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopBar(onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionLabel("default format")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatChip("ask", state.defaultFormat == DownloadFormatPreference.ASK) {
                    viewModel.setDefaultFormat(DownloadFormatPreference.ASK)
                }
                FormatChip("video", state.defaultFormat == DownloadFormatPreference.VIDEO) {
                    viewModel.setDefaultFormat(DownloadFormatPreference.VIDEO)
                }
                FormatChip("audio", state.defaultFormat == DownloadFormatPreference.AUDIO) {
                    viewModel.setDefaultFormat(DownloadFormatPreference.AUDIO)
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("download location")
            OutlinedTextField(
                value = state.downloadLocation,
                onValueChange = viewModel::setDownloadLocation,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip("light", state.themeMode == ThemeMode.LIGHT) { viewModel.setThemeMode(ThemeMode.LIGHT) }
                ThemeChip("dark", state.themeMode == ThemeMode.DARK) { viewModel.setThemeMode(ThemeMode.DARK) }
                ThemeChip("dynamic", state.themeMode == ThemeMode.DYNAMIC) { viewModel.setThemeMode(ThemeMode.DYNAMIC) }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("cobalt API key (optional)")
            OutlinedTextField(
                value = state.cobaltApiKey,
                onValueChange = viewModel::setCobaltApiKey,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            ToggleRow("audio-only mode", state.audioOnly, viewModel::setAudioOnly)
            ToggleRow("clipboard trigger", state.clipboardTrigger, viewModel::setClipboardTrigger)

            Spacer(Modifier.height(24.dp))
            SectionLabel("invidious instances (one per line)")
            OutlinedTextField(
                value = state.invidiousInstances,
                onValueChange = viewModel::setInvidiousInstances,
                placeholder = { Text("https://yewtu.be") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("shorts search terms (one per line, blank = default)")
            OutlinedTextField(
                value = state.shortsQueries,
                onValueChange = viewModel::setShortsQueries,
                placeholder = { Text("gaming clips") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("actions")
            ActionRow("history & likes", onHistoryClick)
            ActionRow("clear download history", viewModel::clearDownloadHistory)
            ActionRow("battery optimization settings", onBatteryClick)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = "back",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack)
                .padding(0.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Settings",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
