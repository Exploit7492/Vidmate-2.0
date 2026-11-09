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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
