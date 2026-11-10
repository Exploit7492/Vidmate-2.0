package com.cobalt.android.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cobalt.android.R
import com.cobalt.android.databinding.FragmentExploreBinding

/**
 * Hosts [ExploreScreen]. Play and download both route through the Home screen
 * (which owns the single resolve → picker → download pipeline), so there is one
 * download path in the app, not a second one here.
 */
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!
    private val exploreViewModel: ExploreViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.exploreCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.exploreCompose.setContent {
            ExploreScreen(
                viewModel = exploreViewModel,
                onOpen = { item -> handOff(item.watchUrl) },
                onDownload = { item -> handOff(item.watchUrl) }
            )
        }
    }

    private fun handOff(url: String) {
        findNavController().navigate(R.id.nav_home, Bundle().apply { putString("pending_url", url) })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
