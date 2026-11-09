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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.cobalt.android.R
import com.cobalt.android.databinding.DialogVideoPlayerBinding
import android.util.Log
import com.cobalt.android.util.EqualizerController

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
    private val equalizer = EqualizerController()
    private var eqIndex = 0
    // MediaSession advertises the player to the system, which is what routes
    // headset/Bluetooth transport buttons and the lock-screen controls to it
    // (Phase C6).
    private var mediaSession: MediaSession? = null
    private var subtitlesOn = true

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
        setupEqualizer(exo)
        setupTracks(exo)
        setupMediaSession(exo)
    }

    /**
     * One "tracks" button covering both the quality switch (C2) and subtitle
     * selection (C3): it lists the video qualities and the text/subtitle tracks
     * ExoPlayer actually found in the media, and applies the user's pick with a
     * [TrackSelectionOverride]. Lists are read from `player.currentTracks` when
     * tapped, so they reflect what is really present rather than a guess.
     */
    private fun setupTracks(exo: ExoPlayer) {
        binding.btnTracks.setOnClickListener {
            val tracks = exo.currentTracks
            val qualities = mutableListOf<String>()
            val videoGroups = mutableListOf<Tracks.Group>()
            for (group in tracks.groups) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.height > 0) {
                        qualities.add("${format.height}p${if (format.bitrate > 0) " · ${format.bitrate / 1000}kbps" else ""}")
                        videoGroups.add(group)
                    }
                }
            }
            val labels = buildList {
                add("Subtitles: ${if (subtitlesOn) "on" else "off"}")
                addAll(qualities)
            }
            if (labels.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(requireContext())
                .setTitle("Tracks")
                .setItems(labels.toTypedArray()) { _, which ->
                    if (which == 0) {
                        subtitlesOn = !subtitlesOn
                        exo.trackSelectionParameters = exo.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !subtitlesOn)
                            .build()
                    } else {
                        val group = videoGroups[which - 1]
                        val format = group.getTrackFormat(0)
                        exo.trackSelectionParameters = exo.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                            .build()
                        Log.i("VideoPlayer", "quality override -> ${format.height}p")
                    }
                }
                .show()
        }
    }

    /**
     * Wires a [MediaSession] to the player so headset/Bluetooth play-pause-skip
     * buttons and the system media notification control it (C6). Media3's
     * `MediaSession` handles the transport-command routing; we only supply the
     * player and an id.
     */
    private fun setupMediaSession(exo: ExoPlayer) {
        mediaSession = MediaSession.Builder(requireContext(), exo)
            .setId("cobalt_player")
            .build()
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

    /**
     * Binds the platform equalizer to the player's audio session and wires the
     * EQ button to cycle the device's own preset list. Hidden when the device
     * exposes no equalizer (the controller reports it unavailable).
     */
    private fun setupEqualizer(exo: ExoPlayer) {
        equalizer.attach(exo.audioSessionId)
        val presets = equalizer.presetNames()
        if (!equalizer.isAvailable || presets.isEmpty()) {
            binding.btnEq.visibility = View.GONE
            return
        }
        binding.btnEq.setOnClickListener {
            eqIndex = (eqIndex + 1) % presets.size
            equalizer.applyPreset(eqIndex)
            binding.btnEq.text = presets[eqIndex].take(10)
        }
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
        mediaSession?.release()
        mediaSession = null
        equalizer.detach()
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
