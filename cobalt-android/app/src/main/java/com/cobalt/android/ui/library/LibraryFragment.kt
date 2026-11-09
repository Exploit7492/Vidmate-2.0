package com.cobalt.android.ui.library

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cobalt.android.databinding.FragmentLibraryBinding
import com.cobalt.android.download.DownloadCanceller
import com.cobalt.android.ui.VideoPlayerDialogFragment

/**
 * Library tab host. Renders [LibraryScreen] in a `ComposeView` (the
 * incremental Compose-adoption path this project already uses) and bridges
 * the two actions that need a Fragment/Activity: playing a saved file
 * (existing `VideoPlayerDialogFragment`) and opening a history/liked source
 * link in the browser.
 */
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.libraryCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.libraryCompose.setContent {
            val active by viewModel.activeDownloads.observeAsState(emptyList())
            val all by viewModel.allDownloads.observeAsState(emptyList())
            val history by viewModel.historyRows.observeAsState(emptyList())
            val liked by viewModel.likedRows.observeAsState(emptyList())

            LibraryScreen(
                activeDownloads = active,
                allDownloads = all,
                historyRows = history,
                likedRows = liked,
                onRetry = { record ->
                    if (record.originalUrl.isNotBlank()) openUrl(record.originalUrl)
                },
                onCancel = { record -> DownloadCanceller.cancel(requireContext(), record.id) },
                onDelete = { record -> viewModel.delete(record) },
                onPlay = { record ->
                    if (record.mediaStoreUriString.isNotBlank()) {
                        VideoPlayerDialogFragment.newInstance(record.mediaStoreUriString)
                            .show(parentFragmentManager, VideoPlayerDialogFragment.TAG)
                    }
                },
                onOpenUrl = { url -> openUrl(url) }
            )
        }
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
