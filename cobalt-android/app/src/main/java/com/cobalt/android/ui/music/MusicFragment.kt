package com.cobalt.android.ui.music

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cobalt.android.databinding.FragmentMusicBinding

/**
 * Music player host (Phase G1 / E10).
 *
 * Owns an audio-only ExoPlayer. The ViewModel holds the queue/index and drives
 * "playing"; this fragment reacts to that state — when `playing` flips or the
 * current index changes, it loads the matching track and starts/stops the
 * player. When the player reaches the end of a track it calls back into the
 * ViewModel to advance, so a whole album plays through.
 */
class MusicFragment : Fragment() {

    private var _binding: FragmentMusicBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MusicViewModel by viewModels()
    private var player: ExoPlayer? = null
    private var loadedUri: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        player = ExoPlayer.Builder(requireContext()).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) viewModel.onPlaybackEnded()
                }
            })
        }
        binding.musicCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.musicCompose.setContent {
            val state by viewModel.state.collectAsState()
            syncPlayer(state.current?.uri, state.playing)
            MusicScreen(
                state = state,
                onSearch = viewModel::setSearch,
                onPlay = viewModel::playVisible,
                onToggle = viewModel::togglePlay,
                onNext = viewModel::next,
                onPrevious = viewModel::previous
            )
        }
    }

    private fun syncPlayer(uri: String?, playing: Boolean) {
        val exo = player ?: return
        if (uri == null) return
        if (uri != loadedUri) {
            loadedUri = uri
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
        }
        if (playing && !exo.isPlaying) exo.play()
        if (!playing && exo.isPlaying) exo.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }
}
