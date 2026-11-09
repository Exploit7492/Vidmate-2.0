package com.cobalt.android.ui.music

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cobalt.android.databinding.FragmentMusicBinding
import com.google.common.util.concurrent.ListenableFuture

/**
 * Music player host (Phase G1 / E10), now backed by [MusicPlaybackService] so
 * playback survives backgrounding (Phase C4).
 *
 * The fragment holds a [MediaController] bound to the service's
 * [androidx.media3.session.MediaSession] rather than its own ExoPlayer. The
 * ViewModel still owns the queue/index; this reacts to `playing`/current-index
 * changes by driving the controller, and advances the ViewModel when the
 * *service's* player reaches the end of a track. Because the service owns the
 * player, the same audio continues (and stays controllable from the
 * notification) after the fragment's view is gone.
 */
class MusicFragment : Fragment() {

    private var _binding: FragmentMusicBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MusicViewModel by viewModels()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var loadedUri: String? = null

    /** Ticked while the view is alive so the lyrics line can follow the song. */
    private var positionMs by mutableLongStateOf(0L)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val token = SessionToken(
            requireContext(),
            ComponentName(requireContext(), MusicPlaybackService::class.java)
        )
        val future = MediaController.Builder(requireContext(), token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                controller = future.get().also { c ->
                    c.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) viewModel.onPlaybackEnded()
                        }
                    })
                    // A controller that connected after the user already pressed
                    // play (fast tap) would otherwise sit idle — reconcile now.
                    val s = viewModel.state.value
                    syncPlayer(s.current?.uri, s.playing)
                }
            },
            ContextCompat.getMainExecutor(requireContext())
        )

        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                positionMs = controller?.currentPosition ?: 0L
                kotlinx.coroutines.delay(500)
            }
        }

        binding.musicCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.musicCompose.setContent {
            val state by viewModel.state.collectAsState()
            syncPlayer(state.current?.uri, state.playing)
            MusicScreen(
                state = state,
                positionMs = positionMs,
                onSearch = viewModel::setSearch,
                onPlay = viewModel::playVisible,
                onToggle = viewModel::togglePlay,
                onNext = viewModel::next,
                onPrevious = viewModel::previous
            )
        }
    }

    private fun syncPlayer(uri: String?, playing: Boolean) {
        val c = controller ?: return
        if (uri == null) return
        if (uri != loadedUri) {
            loadedUri = uri
            c.setMediaItem(MediaItem.fromUri(uri))
            c.prepare()
        }
        if (playing && !c.isPlaying) c.play()
        if (!playing && c.isPlaying) c.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller?.release()
        controller = null
        controllerFuture = null
        _binding = null
    }
}
