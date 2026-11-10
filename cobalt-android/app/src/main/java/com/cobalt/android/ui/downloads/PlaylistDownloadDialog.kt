package com.cobalt.android.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.cobalt.android.databinding.DialogPlaylistDownloadBinding
import com.cobalt.android.link.PlaylistDownloader
import kotlinx.coroutines.launch

/**
 * Playlist / batch download entry (Phase B7 / E3): paste one URL per line, tap
 * download, and each is resolved and enqueued. See [PlaylistDownloader] for why
 * a "playlist" is a line-per-item batch rather than a backend feature.
 */
class PlaylistDownloadDialog : DialogFragment() {

    private var _binding: DialogPlaylistDownloadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogPlaylistDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // A playlist search result opens this dialog with its URL pre-filled,
        // so "download this playlist" is one tap rather than a copy-paste.
        arguments?.getString(ARG_PREFILL)?.takeIf { it.isNotBlank() }?.let {
            binding.etUrls.setText(it)
        }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnStart.setOnClickListener {
            val text = binding.etUrls.text?.toString().orEmpty()
            if (text.isBlank()) return@setOnClickListener
            binding.btnStart.isEnabled = false
            binding.tvStatus.text = "resolving…"
            viewLifecycleOwner.lifecycleScope.launch {
                val result = PlaylistDownloader(requireContext())
                    .downloadAll(text, cookies = "", userAgent = "")
                binding.tvStatus.text = "queued ${result.enqueued}, skipped ${result.failed}"
                binding.btnStart.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PlaylistDownloadDialog"
        private const val ARG_PREFILL = "prefill_urls"

        fun newInstance() = PlaylistDownloadDialog()

        /** Opens the batch dialog with one or more URLs already filled in. */
        fun newInstance(prefill: String) = PlaylistDownloadDialog().apply {
            arguments = Bundle().apply { putString(ARG_PREFILL, prefill) }
        }
    }
}
