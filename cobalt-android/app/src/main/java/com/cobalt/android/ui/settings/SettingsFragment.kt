package com.cobalt.android.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.cobalt.android.databinding.FragmentSettingsBinding
import com.cobalt.android.ui.history.HistoryFragment

/**
 * Settings host. Reached from the Me tab's Settings row (VidMate-style), so
 * it carries its own back affordance that pops back to Me. Bridges the two
 * Compose actions that need a Fragment/Activity context — showing the history
 * sheet and opening the system battery-optimization screen.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.settingsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.settingsCompose.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { findNavController().popBackStack() },
                onHistoryClick = {
                    HistoryFragment.newInstance().show(parentFragmentManager, HistoryFragment.TAG)
                },
                onPluginsClick = {
                    PluginsDialogFragment.newInstance().show(parentFragmentManager, PluginsDialogFragment.TAG)
                },
                onWidgetSettingsClick = {
                    WidgetSettingsDialogFragment.newInstance()
                        .show(parentFragmentManager, WidgetSettingsDialogFragment.TAG)
                },
                onBatteryClick = {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                        }
                    )
                },
                // The language change is applied process-wide by the ViewModel;
                // recreating the Activity makes it visible immediately.
                onRecreate = { requireActivity().recreate() }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
