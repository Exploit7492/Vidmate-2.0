package com.cobalt.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.cobalt.android.databinding.ActivityMainBinding
import com.cobalt.android.link.SearchIntentClassifier
import com.cobalt.android.remoteconfig.RemoteConfigStore
import kotlinx.coroutines.launch
import com.cobalt.android.ui.DownloadQueueSheet
import com.cobalt.android.ui.DownloadQueueViewModel
import com.cobalt.android.ui.common.HeaderBar
import com.cobalt.android.ui.theme.CobaltTheme
import com.cobalt.android.util.ClipboardHelper
import com.cobalt.android.util.SettingsRepository
import com.cobalt.android.util.UrlMatcher
import com.cobalt.android.widget.DownloadsWidgetProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

/**
 * The app shell. Three-tab bottom bar (Home / Shorts / Settings) under a
 * global header: one dynamic search field that classifies whatever the user
 * types or pastes (download / browse / search) and a notification bell that
 * opens the downloads + alerts surface. There is no static brand title — the
 * search field is the title.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsRepository
    private val queueViewModel: DownloadQueueViewModel by viewModels()

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not — app works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsRepository(this)

        val navController = findNavController(R.id.navHost)
        val nav = findViewById<BottomNavigationView>(R.id.navBottom)
        nav.setupWithNavController(navController)
        setupFloatingShortsTab(navController, nav)
        applyRemoteTabs()
        setupHeader()
        observeDownloadQueue()
        handleFirstLaunch()
        handleIntent(intent)
    }

    /**
     * The floating Shorts tab. It is a separate view lifted above the bar, so
     * its taps have to drive the nav controller explicitly, and it has to stay
     * in step with the selected destination (tapping the bar's own blank middle
     * slot, a deep link, or the back stack all have to light it up or dim it).
     * Its flare is dimmed with it so the glow tracks the active state.
     */
    private fun setupFloatingShortsTab(
        navController: NavController,
        nav: BottomNavigationView
    ) {
        val tab = findViewById<View>(R.id.shortsTab)
        val flare = findViewById<View>(R.id.shortsFlare)

        fun openShorts() {
            val current = navController.currentDestination?.id
            if (current != R.id.nav_shorts) {
                navController.navigate(R.id.nav_shorts)
            }
            nav.selectedItemId = R.id.nav_shorts
        }

        tab.setOnClickListener { openShorts() }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val active = destination.id == R.id.nav_shorts
            tab.alpha = if (active) 1f else 0.92f
            flare.alpha = if (active) 1f else 0.45f
            flare.scaleX = if (active) 1f else 0.9f
            flare.scaleY = if (active) 1f else 0.9f
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (settings.clipboardTriggerEnabled) checkClipboard()
    }

    // ── Global header (dynamic search + alerts bell) ─────────────────────────

    private fun setupHeader() {
        binding.headerBar.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.headerBar.setContent {
            CobaltTheme {
                var text by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf("")
                }
                val active by queueViewModel.activeDownloads.observeAsState(emptyList())
                HeaderBar(
                    value = text,
                    onValueChange = { text = it },
                    onSubmit = {
                        routeInput(text)
                        text = ""
                    },
                    onBellClick = { openDownloadsAndAlerts() },
                    bellBadge = active.size
                )
            }
        }
    }

    /** The single routing decision for anything typed or pasted into the header. */
    private fun routeInput(raw: String) {
        when (val action = SearchIntentClassifier.classify(raw)) {
            is SearchIntentClassifier.Action.Download -> submitUrl(action.url)
            is SearchIntentClassifier.Action.Browse ->
                openInBrowser(SearchIntentClassifier.asUrl(action.url))
            is SearchIntentClassifier.Action.Search -> openSearch(action.query)
            SearchIntentClassifier.Action.Idle -> Unit
        }
    }

    private fun openSearch(query: String) {
        findNavController(R.id.navHost).navigate(
            R.id.nav_home,
            bundleOf("pending_search" to query)
        )
        findViewById<BottomNavigationView>(R.id.navBottom).selectedItemId = R.id.nav_home
    }

    private fun openDownloadsAndAlerts() {
        DownloadQueueSheet.newInstance().also { sheet ->
            sheet.onRetry = { record -> submitUrl(record.originalUrl) }
            sheet.show(supportFragmentManager, DownloadQueueSheet.TAG)
        }
    }

    /** Batch dialog for several shared links at once. */
    private fun openPlaylistDialog(prefill: String) {
        com.cobalt.android.ui.downloads.PlaylistDownloadDialog.newInstance(prefill)
            .show(supportFragmentManager, com.cobalt.android.ui.downloads.PlaylistDownloadDialog.TAG)
    }

    /**
     * Copies a shared video/image into Downloads, then posts the same
     * completion notification a resolved download would, so a file shared into
     * the app behaves like anything else the app saved.
     */
    private fun importSharedMedia(intent: Intent) {
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM) ?: return
        val mime = intent.type ?: "application/octet-stream"
        lifecycleScope.launch {
            val saved = com.cobalt.android.util.SharedMediaImporter.import(this@MainActivity, uri, mime)
            if (saved == null) {
                Snackbar.make(binding.root, "could not import shared file", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            val name = intent.clipData?.description?.label?.toString()
                ?: saved.lastPathSegment ?: "shared file"
            com.cobalt.android.util.NotificationHelper(this@MainActivity)
                .showComplete(recordId = 0L, filename = name, uri = saved, mimeType = mime)
            Snackbar.make(binding.root, "saved to Downloads", Snackbar.LENGTH_SHORT).show()
        }
    }

    // ── Remote tab config ────────────────────────────────────────────────────

    /**
     * Hides bottom-bar items the remote config excludes, so the tab set can be
     * changed centrally without an app update. A null/absent config leaves the
     * three shipped tabs as-is.
     */
    private fun applyRemoteTabs() {
        val tabs = RemoteConfigStore.tabs() ?: return
        val nav = findViewById<BottomNavigationView>(R.id.navBottom)
        val byName = mapOf(
            "home" to R.id.nav_home,
            "shorts" to R.id.nav_shorts,
            "settings" to R.id.nav_settings
        )
        byName.forEach { (name, id) ->
            nav.menu.findItem(id)?.isVisible = tabs.contains(name)
        }
    }

    // ── Download queue badge (widget sync) ───────────────────────────────────

    private fun observeDownloadQueue() {
        queueViewModel.activeDownloads.observe(this) {
            // Keep the home-screen widget (H7) in step with the queue.
            DownloadsWidgetProvider.refresh(this)
        }
    }

    // ── Intent handling ──────────────────────────────────────────────────────

    private fun handleIntent(intent: Intent?) {
        when {
            // Open-with / deep link: cobalt://download?url=... or a VIEW of an
            // http(s) link the user chose to open here.
            intent?.action == Intent.ACTION_VIEW -> {
                val url = intent.dataString
                    ?.let { UrlMatcher.extractUrl(it) }
                    ?: intent.data?.getQueryParameter("url")
                if (!url.isNullOrBlank()) submitUrl(url) else openInBrowser(intent.dataString.orEmpty())
            }
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val url = UrlMatcher.extractUrl(text)
                if (url != null) {
                    submitUrl(url)
                } else if (!text.isNullOrBlank()) {
                    // Not a host we resolve directly — offer the in-app browser,
                    // which can sniff the media off the page instead.
                    Snackbar.make(
                        binding.root,
                        getString(R.string.not_a_supported_link),
                        Snackbar.LENGTH_LONG
                    ).setAction("browse") { openInBrowser(text) }.show()
                }
            }
            // Sharing several links at once (e.g. a multi-select share): hand
            // them to the batch dialog so one tap queues them all.
            intent?.action == Intent.ACTION_SEND_MULTIPLE &&
                intent.type?.startsWith("text/") == true -> {
                val urls = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
                    .orEmpty()
                    .mapNotNull { UrlMatcher.extractUrl(it) }
                when {
                    urls.size == 1 -> submitUrl(urls.first())
                    urls.size > 1 -> openPlaylistDialog(urls.joinToString("\n"))
                    else -> Snackbar.make(
                        binding.root,
                        getString(R.string.not_a_supported_link),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            // Sharing a media file: copy it into Downloads and notify. There is
            // no URL to resolve — the bytes are already here.
            intent?.action == Intent.ACTION_SEND &&
                (intent.type?.startsWith("video/") == true ||
                    intent.type?.startsWith("image/") == true) -> {
                importSharedMedia(intent)
            }
            intent?.getStringExtra("shortcut_queue") == "true" ||
            intent?.getBooleanExtra("shortcut_queue", false) == true -> {
                openDownloadsAndAlerts()
            }
            intent?.getStringExtra("shortcut_paste") == "true" ||
            intent?.getBooleanExtra("shortcut_paste", false) == true -> {
                val url = ClipboardHelper.getSupportedUrl(this)
                if (url != null) submitUrl(url)
            }
        }
    }

    // ── Clipboard trigger ────────────────────────────────────────────────────

    private fun checkClipboard() {
        val url = ClipboardHelper.getSupportedUrl(this) ?: return
        Snackbar.make(binding.root, getString(R.string.download_from_clipboard), Snackbar.LENGTH_LONG)
            .setAction("download") { submitUrl(url) }
            .setBackgroundTint(getColor(R.color.cobalt_surface_elevated))
            .setTextColor(getColor(R.color.cobalt_text_primary))
            .setActionTextColor(getColor(R.color.cobalt_accent_blue))
            .show()
    }

    // ── URL routing ──────────────────────────────────────────────────────────

    private fun openInBrowser(url: String) {
        findNavController(R.id.navHost).navigate(
            R.id.nav_browser,
            bundleOf("url" to url)
        )
    }

    private fun submitUrl(url: String) {
        findNavController(R.id.navHost).navigate(
            R.id.nav_home,
            bundleOf("pending_url" to url)
        )
        findViewById<BottomNavigationView>(R.id.navBottom).selectedItemId = R.id.nav_home
    }

    // ── First launch ─────────────────────────────────────────────────────────

    private fun handleFirstLaunch() {
        if (settings.firstLaunchDone) return
        settings.firstLaunchDone = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.battery_dialog_title))
                .setMessage(getString(R.string.battery_dialog_message))
                .setPositiveButton(getString(R.string.allow)) { _, _ ->
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
                .setNegativeButton(getString(R.string.not_now), null)
                .show()
        }
    }
}
