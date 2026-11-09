package com.cobalt.android.ui

import android.app.Dialog
import android.app.PictureInPictureParams
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cobalt.android.R
import com.cobalt.android.databinding.DialogVideoPlayerBinding

/**
 * Phase 8 — tap-to-play for completed downloads.
 *
 * Reuses the same `androidx.media3`/`ExoPlayer` stack `ShortsFragment` already
 * depends on (Phase 2) rather than a second video-playback library, per
 * ARCHITECTURE.md's "Unified enhancement" rule. Unlike `ShortsFragment`'s
 * single player shared across a `ViewPager2` of many items, this dialog owns
 * exactly one player for exactly one local file, so the lifecycle is simpler:
 * built in `onViewCreated`, paused in `onPause` (backgrounding), released in
 * `onDestroyView` (screen exit) — matching Phase 8's Definition of Done and
 * the same discipline `ShortsFragment` uses.
 *
 * Plays from a `content://` MediaStore URI (`DownloadRecord.mediaStoreUriString`
 * from Phase 7's completed-download rows) — a local file, not a network
 * stream, so no HLS/DASH MIME-type branching is needed here the way
 * `ShortsFragment.playAt()` needs for remote streams.
 */
class VideoPlayerDialogFragment : DialogFragment() {

    private var _binding: DialogVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var player: ExoPlayer? = null
    private var speedIndex = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.Theme_Cobalt_FullScreenDialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        // A plain Dialog wraps its content by default even under a fullscreen
        // theme — force it to actually fill the screen.
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener { dismiss() }

        val uriString = requireArguments().getString(ARG_URI).orEmpty()
        if (uriString.isBlank()) {
            showError()
            return
        }

        val exo = ExoPlayer.Builder(requireContext()).build()
        player = exo
        binding.playerView.player = exo

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                showError()
            }
        })

        exo.setMediaItem(MediaItem.fromUri(Uri.parse(uriString)))
        exo.prepare()
        exo.playWhenReady = true

        setupPlaybackSpeed(exo)
        setupPip()
    }

    /**
     * Cycles 1.0x → 1.25x → 1.5x → 2.0x → 0.5x → 1.0x. ExoPlayer applies the
     * speed to audio and video together (no pitch correction beyond the
     * platform default), which is the standard behaviour for a media player.
     */
    private fun setupPlaybackSpeed(exo: ExoPlayer) {
        binding.btnSpeed.setOnClickListener {
            speedIndex = (speedIndex + 1) % SPEEDS.size
            val speed = SPEEDS[speedIndex]
            exo.setPlaybackSpeed(speed)
            binding.btnSpeed.text = "%.2fx".format(speed).replace(".00x", ".0x")
        }
    }

    /**
     * Picture-in-picture: available from API 26 (O); the button is hidden below
     * that. Entering PiP hands the player's surface to the system window, so the
     * video keeps playing when the user leaves the app.
     */
    private fun setupPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            binding.btnPip.visibility = View.GONE
            return
        }
        binding.btnPip.setOnClickListener { enterPip() }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        runCatching { requireActivity().enterPictureInPictureMode(params) }
    }

    /** When the Activity enters PiP, hide the chrome (close/speed/PiP buttons)
     *  so the small window shows only video. */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        val visibility = if (isInPictureInPictureMode) View.GONE else View.VISIBLE
        _binding?.btnClose?.visibility = visibility
        _binding?.btnSpeed?.visibility = visibility
        _binding?.btnPip?.visibility = visibility
    }

    private fun showError() {
        binding.tvPlayerError.text = getString(R.string.player_error_unplayable)
        binding.tvPlayerError.visibility = View.VISIBLE
        binding.playerView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        _binding = null
    }

    companion object {
        const val TAG = "VideoPlayerDialogFragment"
        private const val ARG_URI = "arg_uri"
        private val SPEEDS = floatArrayOf(1.0f, 1.25f, 1.5f, 2.0f, 0.5f)

        fun newInstance(mediaStoreUriString: String): VideoPlayerDialogFragment =
            VideoPlayerDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_URI, mediaStoreUriString) }
            }
    }
}
