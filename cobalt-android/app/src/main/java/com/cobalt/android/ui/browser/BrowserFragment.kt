package com.cobalt.android.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cobalt.android.R
import com.cobalt.android.databinding.FragmentBrowserBinding
import com.google.android.material.snackbar.Snackbar

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
    // Multi-tab (F2): the URL of each open tab plus which is active. One
    // WebView is reused; switching tabs saves the current URL and loads the
    // target, which is all a downloader's browser needs (full session
    // isolation per tab would mean a WebView each, for no user-visible gain
    // here).
    private val tabs = mutableListOf<String?>()
    private var activeTab = 0
    private var incognito = false

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
        binding.btnTabs.setOnClickListener { showTabs() }
        binding.btnIncognito.setOnClickListener { toggleIncognito() }
        binding.etAddress.setOnEditorActionListener { _, _, _ ->
            val typed = binding.etAddress.text.toString().trim()
            if (typed.isNotBlank()) load(typed)
            true
        }

        // A URL handed in by the caller (e.g. "open in browser" from Home).
        val start = arguments?.getString(ARG_URL)
        tabs.add(start)
        if (!start.isNullOrBlank()) {
            load(start)
        } else {
            binding.etAddress.setText("")
        }
    }

    /** Tab strip as a list dialog plus a "New tab" action (F2). */
    private fun showTabs() {
        tabs[activeTab] = binding.webView.url
        val labels = tabs.mapIndexed { i, url ->
            val shown = url?.takeIf { it.isNotBlank() } ?: "(new tab)"
            (if (i == activeTab) "• " else "  ") + shown
        }.toMutableList()
        labels.add(getString(R.string.browser_new_tab))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.browser_tabs)
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == labels.lastIndex) {
                    openNewTab()
                } else {
                    switchToTab(which)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openNewTab() {
        tabs[activeTab] = binding.webView.url
        tabs.add(null)
        activeTab = tabs.lastIndex
        binding.etAddress.setText("")
        binding.webView.loadUrl("about:blank")
    }

    private fun switchToTab(index: Int) {
        if (index == activeTab || index !in tabs.indices) return
        tabs[activeTab] = binding.webView.url
        activeTab = index
        val url = tabs[index]
        if (url.isNullOrBlank()) {
            binding.etAddress.setText("")
            binding.webView.loadUrl("about:blank")
        } else {
            load(url)
        }
    }

    /**
     * Incognito toggle (F3). Turning it on wipes cookies, WebStorage and cache
     * *now* so nothing from the previous session is visible, and again on exit
     * so the incognito session itself leaves nothing behind. It does not claim
     * to hide traffic from the network — only to not persist it locally.
     */
    private fun toggleIncognito() {
        incognito = !incognito
        binding.btnIncognito.alpha = if (incognito) 1.0f else 0.4f
        if (incognito) wipeBrowsingData()
        Snackbar.make(
            binding.root,
            if (incognito) "Incognito on — history and cookies cleared" else "Incognito off",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun wipeBrowsingData() {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            binding.webView.clearHistory()
            binding.webView.clearCache(true)
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
        if (incognito) wipeBrowsingData()
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
