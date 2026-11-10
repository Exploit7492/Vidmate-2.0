package com.cobalt.android.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import com.cobalt.android.plugin.PluginRegistry

/**
 * Hosts [PluginsScreen] in a dialog. The registry is owned by the fragment
 * (created once, loaded from the bundled manifest) so the enable/disable
 * choices survive while the dialog is open; persistence across launches is
 * handled by [com.cobalt.android.plugin.PluginRegistry]'s caller when needed.
 */
class PluginsDialogFragment : DialogFragment() {

    private lateinit var registry: PluginRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registry = PluginRegistry(requireContext()).apply { load() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            PluginsScreen(
                plugins = registry.all(),
                isEnabled = registry::isEnabled,
                onToggle = { id, on -> registry.setEnabled(id, on) },
                onBack = { dismiss() }
            )
        }
    }

    companion object {
        const val TAG = "PluginsDialogFragment"
        fun newInstance() = PluginsDialogFragment()
    }
}
