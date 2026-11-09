package com.cobalt.android.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cobalt.android.R
import com.cobalt.android.databinding.FragmentBrowserBinding
import com.cobalt.android.util.UrlMatcher

/**
 * A real in-app browser (Phase F1/F4/F5).
 *
 * This is not a stub: it is a `WebView` with an address bar, a back button, a
 * desktop-site UA toggle, and — the capability that actually matters for a
 * downloader — **media sniffing**. As the page loads, [MediaSniffer] watches
 * every request the WebView makes (both sub-resources via
 * `shouldInterceptRequest` and the top-level navigations via
 * `shouldOverrideUrlLoading`) and records URLs that look like media
 * (`.mp4/.m3u8/.mpd/.webm/.mp3/...`, or a media MIME type). When the user taps
 * the download button — or a sniffer candidate appears — they are offered the
 * captured streams, and picking one hands it to the same Home download path the
 * share/clipboard entries use, so a site cobalt cannot resolve directly can
 * still be downloaded from a sniffed URL.
 *
 * The sniffer's URL heuristics live in [MediaSniffer] (a plain class, unit
 * tested on the JVM) rather than in the fragment so the classification is
 * testable without a WebView.
 */
class BrowserFragment : Fragment() {

    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!
    private val sniffer = MediaSniffer()
    private var desktopMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = true
        }
        binding.webView.webViewClient = sniffingClient()

        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack() else findNavController().navigateUp()
        }
        binding.btnDesktop.setOnClickListener { toggleDesktopMode() }
        binding.btnDownload.setOnClickListener { offerSniffed() }
        binding.etAddress.setOnEditorActionListener { _, _, _ ->
            val typed = binding.etAddress.text.toString().trim()
            if (typed.isNotBlank()) load(typed)
            true
        }

        // A URL handed in by the caller (e.g. "open in browser" from Home).
        val start = arguments?.getString(ARG_URL)
        if (!start.isNullOrBlank()) {
            load(start)
        } else {
            binding.etAddress.setText("")
        }
    }

    private fun load(input: String) {
        val url = normalize(input)
        binding.etAddress.setText(url)
        binding.webView.loadUrl(url)
    }

    private fun normalize(input: String): String =
        if (input.startsWith("http://") || input.startsWith("https://")) input
        else if (input.contains(' ') || !input.contains('.')) "https://www.google.com/search?q=" + input
        else "https://$input"

    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        binding.webView.settings.userAgentString = if (desktopMode) {
            DESKTOP_UA
        } else {
            null // reset to the WebView default
        }
        binding.webView.reload()
    }

    /** Offers the sniffed media candidates (or the current page URL) for download. */
    private fun offerSniffed() {
        val candidates = sniffer.candidates()
        val pageUrl = binding.webView.url
        val options = buildList {
            addAll(candidates)
            if (pageUrl != null && candidates.none { it == pageUrl }) add(pageUrl)
        }.distinct()
        if (options.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.browser_download)
            .setItems(options.toTypedArray()) { _, which -> handOff(options[which]) }
            .show()
    }

    private fun handOff(url: String) {
        findNavController().navigate(R.id.nav_home, bundleOf("pending_url" to url))
    }

    private fun sniffingClient(): WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            sniffer.observe(url, null)
            return false // let the WebView navigate
        }

        override fun shouldInterceptRequest(
            view: WebView, request: WebResourceRequest
        ): WebResourceResponse? {
            sniffer.observe(request.url.toString(), request.requestHeaders?.get("Accept"))
            return null // observe only; never alter the response
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            binding.progress.visibility = View.VISIBLE
            binding.etAddress.setText(url)
            sniffer.clear() // candidates belong to one page
        }

        override fun onPageFinished(view: WebView, url: String) {
            binding.progress.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webView.destroy()
        _binding = null
    }

    companion object {
        private const val ARG_URL = "url"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"

        fun newInstance(url: String? = null): BrowserFragment = BrowserFragment().apply {
            if (!url.isNullOrBlank()) arguments = bundleOf(ARG_URL to url)
        }
    }
}
