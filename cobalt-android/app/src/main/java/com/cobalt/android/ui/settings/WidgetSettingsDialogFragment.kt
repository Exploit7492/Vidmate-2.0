package com.cobalt.android.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.cobalt.android.util.WidgetSettings
import com.cobalt.android.widget.DownloadsWidgetProvider

/**
 * The widget's own options (VidMate's "app-widget settings" row): count,
 * progress detail, compact mode and the animated spinner. Each change is
 * written straight to [WidgetSettings] and immediately pushed to any placed
 * widget so the effect is visible without waiting for the next tick.
 */
class WidgetSettingsDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        val settings = WidgetSettings(requireContext())
        setContent {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp)
            ) {
                Text(
                    "widget settings",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Toggle("show download count", settings.showCount) {
                    settings.showCount = it; DownloadsWidgetProvider.refresh(requireContext())
                }
                Toggle("show progress detail", settings.showProgress) {
                    settings.showProgress = it; DownloadsWidgetProvider.refresh(requireContext())
                }
                Toggle("compact (hide action row)", settings.compact) {
                    settings.compact = it; DownloadsWidgetProvider.refresh(requireContext())
                }
                Toggle("animated indicator", settings.animated) {
                    settings.animated = it; DownloadsWidgetProvider.refresh(requireContext())
                }
            }
        }
    }

    @Composable
    private fun Toggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit) {
        var checked by remember { mutableStateOf(initial) }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = checked, onCheckedChange = { checked = it; onChange(it) })
        }
    }

    companion object {
        const val TAG = "WidgetSettingsDialogFragment"
        fun newInstance() = WidgetSettingsDialogFragment()
    }
}
