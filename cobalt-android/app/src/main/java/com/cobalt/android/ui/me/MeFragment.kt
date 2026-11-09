package com.cobalt.android.ui.me

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.cobalt.android.BuildConfig
import com.cobalt.android.R
import com.cobalt.android.databinding.FragmentMeBinding
import com.cobalt.android.ui.history.HistoryFragment
import java.io.File
import kotlinx.coroutines.launch
import com.cobalt.android.update.UpdateChecker
import com.cobalt.android.update.ApkInstaller
import androidx.lifecycle.lifecycleScope

/**
 * Me tab host. Renders [MeScreen] and routes its rows: My Downloads opens
 * the download-queue sheet, History & Likes opens the existing
 * [HistoryFragment], Settings navigates to the hidden settings destination,
 * and About shows a simple dialog.
 */
class MeFragment : Fragment() {

    private var _binding: FragmentMeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.meCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.meCompose.setContent {
            val downloads by viewModel.downloadCount.observeAsState(0)
            val history by viewModel.historyCount.observeAsState(0)
            val liked by viewModel.likedCount.observeAsState(0)

            MeScreen(
                downloadCount = downloads,
                historyCount = history,
                likedCount = liked,
                versionName = BuildConfig.VERSION_NAME,
                onDownloads = {
                    findNavController().navigate(R.id.nav_library)
                },
                onHistory = {
                    HistoryFragment.newInstance().show(parentFragmentManager, HistoryFragment.TAG)
                },
                onSettings = {
                    findNavController().navigate(R.id.nav_settings)
                },
                onVault = {
                    findNavController().navigate(R.id.nav_vault)
                },
                onMediaLibrary = {
                    findNavController().navigate(R.id.nav_media_library)
                },
                onMusic = {
                    findNavController().navigate(R.id.nav_music)
                },
                onStatus = {
                    findNavController().navigate(R.id.nav_status)
                },
                onGames = {
                    findNavController().navigate(R.id.nav_games)
                },
                onCheckUpdates = { checkForUpdates() },
                onBrowser = {
                    findNavController().navigate(R.id.nav_browser)
                },
                onAbout = { showAbout() }
            )
        }
    }

    private fun showAbout() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.me_about))
            .setMessage(getString(R.string.me_about_body))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Self-update (I2). Checks the published config; if a newer version with a
     * download URL exists, offers it, then downloads the APK into the app's
     * private files dir and hands it to the system installer. Installing
     * outside a store always needs the user's confirmation in the system
     * dialog, and on Android 8+ the "install unknown apps" permission — routed
     * to Settings when missing.
     */
    private fun checkForUpdates() {
        viewLifecycleOwner.lifecycleScope.launch {
            val checker = UpdateChecker(BuildConfig.VERSION_CODE)
            val update = checker.check()
            if (update == null) {
                AlertDialog.Builder(requireContext())
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
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.update_title)
                .setMessage(message)
                .setPositiveButton(R.string.update_download) { _, _ -> downloadAndInstall(checker, update) }
                .setNegativeButton(R.string.not_now, null)
                .show()
        }
    }

    private fun downloadAndInstall(checker: UpdateChecker, update: UpdateChecker.Update) {
        if (!ApkInstaller.canRequestInstalls(requireContext())) {
            ApkInstaller.requestInstallPermission(requireContext())
            return
        }
        val dest = File(requireContext().filesDir, "updates/update.apk")
        viewLifecycleOwner.lifecycleScope.launch {
            val apk = checker.download(update.apkUrl, dest)
            if (apk == null) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_title)
                    .setMessage(R.string.update_download_failed)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@launch
            }
            ApkInstaller.install(requireContext(), apk)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
