package com.cobalt.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cobalt.android.databinding.FragmentHomeBinding
import com.cobalt.android.link.LinkResolverRepository
import com.cobalt.android.ui.DownloadQueueSheet
import com.cobalt.android.ui.downloads.PlaylistDownloadDialog
import com.cobalt.android.ui.downloads.ResolutionPickerDialog

/**
 * Phase 23: Home's feed is now a real Compose surface (see [HomeScreen]),
 * hosted in a `ComposeView` inside this still-Fragment screen — the
 * incremental-adoption path ARCHITECTURE.md's Phase 22 describes (one screen
 * at a time via `ComposeView` interop, not a big-bang rewrite).
 *
 * The paste-link → resolve → `ResolutionPickerDialog` flow is unchanged from
 * Phases 3/4/6; only its UI moved from XML into Compose ([HomeScreen]'s
 * `PasteLinkBar`). The picker itself is still the existing XML
 * `ResolutionPickerDialog` (Phase 25's scope), shown here from the fragment
 * because it needs a `FragmentManager`.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Activity-scoped (not viewModels()) so ResolutionPickerDialog, shown via
    // childFragmentManager, reads the same HomeViewModel instance/resolveResult
    // rather than getting its own separate one.
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set once per arrival at this destination — MainActivity.submitUrl()
        // navigates here fresh with a new "pending_url" each time.
        arguments?.getString("pending_url")?.let { url ->
            viewModel.setPendingUrl(url)
        }
        // Free-text from the header's search field lands here as "pending_search".
        arguments?.getString("pending_search")?.let { query ->
            viewModel.search(query)
        }

        binding.homeCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.homeCompose.setContent {
            HomeScreen(
                viewModel = viewModel,
                onPlaylist = {
                    if (childFragmentManager.findFragmentByTag(PlaylistDownloadDialog.TAG) == null) {
                        PlaylistDownloadDialog.newInstance()
                            .show(childFragmentManager, PlaylistDownloadDialog.TAG)
                    }
                },
                onOpenDownloads = { openDownloadsAndAlerts() }
            )
        }

        // Phase 6: show the resolution picker once a link resolves.
        // childFragmentManager (not parentFragmentManager) so the sheet's
        // lifecycle is tied to this fragment, not the hosting activity.
        // H5: a submitted channel URL opens the channel screen.
        viewModel.channelUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrBlank()) {
                viewModel.onChannelOpened()
                findNavController().navigate(
                    com.cobalt.android.R.id.nav_channel,
                    Bundle().apply { putString("channel_url", url) }
                )
            }
        }

        viewModel.resolveResult.observe(viewLifecycleOwner) { result ->
            if (result is LinkResolverRepository.ResolveResult.Success &&
                childFragmentManager.findFragmentByTag(ResolutionPickerDialog.TAG) == null
            ) {
                ResolutionPickerDialog.newInstance().show(childFragmentManager, ResolutionPickerDialog.TAG)
            }
        }

        // A playlist search result opens the batch dialog with the playlist URL
        // already filled in.
        viewModel.playlistUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrBlank()) {
                viewModel.onPlaylistOpened()
                if (childFragmentManager.findFragmentByTag(PlaylistDownloadDialog.TAG) == null) {
                    PlaylistDownloadDialog.newInstance(url)
                        .show(childFragmentManager, PlaylistDownloadDialog.TAG)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Opens the same downloads + alerts sheet the header's bell opens, so the
     * Home entry card and the bell are one surface rather than two.
     */
    private fun openDownloadsAndAlerts() {
        if (childFragmentManager.findFragmentByTag(DownloadQueueSheet.TAG) != null) return
        DownloadQueueSheet.newInstance().also { sheet ->
            sheet.onRetry = { record -> viewModel.setPendingUrl(record.originalUrl) }
            sheet.show(childFragmentManager, DownloadQueueSheet.TAG)
        }
    }
}
