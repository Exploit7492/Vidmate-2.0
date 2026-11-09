package com.cobalt.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cobalt.android.databinding.FragmentHomeBinding
import com.cobalt.android.link.LinkResolverRepository
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

        binding.homeCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.homeCompose.setContent {
            HomeScreen(viewModel)
        }

        // Phase 6: show the resolution picker once a link resolves.
        // childFragmentManager (not parentFragmentManager) so the sheet's
        // lifecycle is tied to this fragment, not the hosting activity.
        viewModel.resolveResult.observe(viewLifecycleOwner) { result ->
            if (result is LinkResolverRepository.ResolveResult.Success &&
                childFragmentManager.findFragmentByTag(ResolutionPickerDialog.TAG) == null
            ) {
                ResolutionPickerDialog.newInstance().show(childFragmentManager, ResolutionPickerDialog.TAG)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
