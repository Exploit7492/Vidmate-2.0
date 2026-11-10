package com.cobalt.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.cobalt.android.plugin.PluginDescriptor

/**
 * The Plugins screen (VidMate's "modules" list): every known add-on with a
 * toggle. A required module's switch is disabled so it can't be turned off;
 * everything else can be, and the choice is persisted by the caller.
 */
@Composable
fun PluginsScreen(
    plugins: List<PluginDescriptor>,
    isEnabled: (String) -> Boolean,
    onToggle: (String, Boolean) -> Boolean,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "back",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 12.dp)
                    .clickable(onClick = onBack)
            )
            Text("Modules", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (plugins.isEmpty()) {
            Text(
                "no modules installed",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                plugins.forEach { plugin ->
                    var on by remember(plugin.id) { mutableStateOf(isEnabled(plugin.id)) }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(plugin.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            Text(
                                buildString {
                                    append(plugin.id)
                                    if (plugin.versionName.isNotBlank()) append(" · ${plugin.versionName}")
                                    if (plugin.required) append(" · required")
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = on,
                            enabled = !plugin.required,
                            onCheckedChange = { next ->
                                if (onToggle(plugin.id, next)) on = next
                            }
                        )
                    }
                }
            }
        }
    }
}
