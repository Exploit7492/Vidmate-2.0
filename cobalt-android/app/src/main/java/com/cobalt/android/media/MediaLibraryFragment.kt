package com.cobalt.android.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cobalt.android.databinding.FragmentMediaLibraryBinding
import com.cobalt.android.ui.VideoPlayerDialogFragment

/** Host for [MediaLibraryScreen]; plays a tapped item in the existing player. */
class MediaLibraryFragment : Fragment() {

    private var _binding: FragmentMediaLibraryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MediaLibraryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.mediaCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.mediaCompose.setContent {
            val state by viewModel.state.collectAsState()
            MediaLibraryScreen(
                state = state,
                onSearch = viewModel::setSearch,
                onFilter = viewModel::setFilter,
                onSort = viewModel::setSort,
                onOpen = { item ->
                    VideoPlayerDialogFragment.newInstance(item.uri)
                        .show(parentFragmentManager, VideoPlayerDialogFragment.TAG)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
