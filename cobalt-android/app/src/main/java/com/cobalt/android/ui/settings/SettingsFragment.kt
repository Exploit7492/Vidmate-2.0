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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cobalt.android.R
import com.cobalt.android.databinding.FragmentSettingsBinding
import com.cobalt.android.ui.history.HistoryFragment
import kotlinx.coroutines.launch

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
                onRecreate = { requireActivity().recreate() },
                // Rows folded in from the removed Me tab.
                onVaultClick = { findNavController().navigate(R.id.nav_vault) },
                onMediaLibraryClick = { findNavController().navigate(R.id.nav_media_library) },
                onMusicClick = { findNavController().navigate(R.id.nav_music) },
                onStatusClick = { findNavController().navigate(R.id.nav_status) },
                onGamesClick = { findNavController().navigate(R.id.nav_games) },
                onBrowserClick = { findNavController().navigate(R.id.nav_browser) },
                onCheckUpdatesClick = { checkForUpdates() },
                onAboutClick = { showAbout() }
            )
        }
    }

    private fun showAbout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.me_about)
            .setMessage(getString(R.string.me_about_body, com.cobalt.android.BuildConfig.VERSION_NAME))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Self-update, moved here from the removed Me tab. Checks the published
     * config and, if a newer build with a download URL exists, offers it, then
     * downloads the APK and hands it to the system installer.
     */
    private fun checkForUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            val checker = com.cobalt.android.update.UpdateChecker(com.cobalt.android.BuildConfig.VERSION_CODE)
            val update = checker.check()
            if (update == null) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_title)
                    .setMessage(R.string.update_up_to_date)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@launch
            }
            val message = buildString {
                append(getString(R.string.update_available, update.versionName))
                if (update.notes.isNotBlank()) append("\n\n").append(update.notes)
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.update_title)
                .setMessage(message)
                .setPositiveButton(R.string.update_download) { _, _ -> downloadAndInstall(checker, update) }
                .setNegativeButton(R.string.not_now, null)
                .show()
        }
    }

    private fun downloadAndInstall(
        checker: com.cobalt.android.update.UpdateChecker,
        update: com.cobalt.android.update.UpdateChecker.Update
    ) {
        if (!com.cobalt.android.update.ApkInstaller.canRequestInstalls(requireContext())) {
            com.cobalt.android.update.ApkInstaller.requestInstallPermission(requireContext())
            return
        }
        val dest = java.io.File(requireContext().filesDir, "updates/update.apk")
        viewLifecycleOwner.lifecycleScope.launch {
            val apk = checker.download(update.apkUrl, dest)
            if (apk == null) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_title)
                    .setMessage(R.string.update_download_failed)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@launch
            }
            com.cobalt.android.update.ApkInstaller.install(requireContext(), apk)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
