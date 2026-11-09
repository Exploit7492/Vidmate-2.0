package com.cobalt.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import com.cobalt.android.databinding.SheetDownloadQueueBinding
import com.cobalt.android.download.DownloadCanceller
import com.cobalt.android.download.DownloadRecord
import com.cobalt.android.download.DownloadStatus
import com.cobalt.android.ui.downloads.DownloadsScreen
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Phase 25: the download queue bottom sheet now hosts the Compose
 * [DownloadsScreen] in a `ComposeView`; the RecyclerView/`DownloadAdapter`
 * it used to own are gone. The fragment still exists (rather than being
 * replaced by a Compose bottom sheet) because MainActivity shows it via
 * `supportFragmentManager` and needs the fragment to hand `onRetry` a URL to
 * re-submit — the same seam MainActivity already used.
 */
class DownloadQueueSheet : BottomSheetDialogFragment() {

    private var _binding: SheetDownloadQueueBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadQueueViewModel by activityViewModels()

    /** Set by MainActivity before `show()`, to route "retry" back through the
     * resolve flow rather than a second retry path. */
    var onRetry: ((DownloadRecord) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SheetDownloadQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.queueCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.queueCompose.setContent {
            val all by viewModel.allDownloads.observeAsState(emptyList())
            val active by viewModel.activeDownloads.observeAsState(emptyList())

            DownloadsScreen(
                activeRecords = active,
                historyRecords = all.filter {
                    it.status == DownloadStatus.COMPLETE ||
                        it.status == DownloadStatus.FAILED ||
                        it.status == DownloadStatus.FAILED_NETWORK
                },
                onRetry = { record -> onRetry?.invoke(record) },
                onCancel = { record -> DownloadCanceller.cancel(requireContext(), record.id) },
                onPlay = { record ->
                    VideoPlayerDialogFragment.newInstance(record.mediaStoreUriString)
                        .show(parentFragmentManager, VideoPlayerDialogFragment.TAG)
                },
                onClear = { viewModel.clearCompleted() }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DownloadQueueSheet"
        fun newInstance() = DownloadQueueSheet()
    }
}
