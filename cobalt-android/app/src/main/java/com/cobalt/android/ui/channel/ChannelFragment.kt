package com.cobalt.android.ui.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.cobalt.android.databinding.FragmentChannelBinding

/** Hosts [ChannelScreen] (H5). The channel URL is passed as an argument. */
class ChannelFragment : Fragment() {

    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChannelViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val url = arguments?.getString(ARG_URL).orEmpty()
        binding.channelCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.channelCompose.setContent {
            val state by viewModel.state.collectAsState()
            ChannelScreen(
                state = state,
                onPlay = { item -> openInHome(item.watchUrl) },
                onDownload = { item -> openInHome(item.watchUrl) },
                onLoadMore = viewModel::loadMore
            )
        }
        if (url.isNotBlank()) viewModel.load(url)
    }

    /**
     * Channel taps hand the URL back to Home, which owns the resolve → picker
     * pipeline; duplicating that flow here would be a second implementation of
     * the download path (ARCHITECTURE.md's "Unified enhancement" rule).
     */
    private fun openInHome(url: String) {
        findNavController().navigate(
            com.cobalt.android.R.id.nav_home,
            Bundle().apply { putString("pending_url", url) }
        )
    }

    companion object {
        private const val ARG_URL = "channel_url"
        fun newInstance(url: String): ChannelFragment =
            ChannelFragment().apply {
                arguments = Bundle().apply { putString(ARG_URL, url) }
            }
    }
}
