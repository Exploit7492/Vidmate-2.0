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
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.cobalt.android.databinding.ActivityMainBinding
import com.cobalt.android.remoteconfig.RemoteConfigStore
import com.cobalt.android.ui.DownloadQueueSheet
import com.cobalt.android.ui.DownloadQueueViewModel
import com.cobalt.android.util.ClipboardHelper
import com.cobalt.android.util.SettingsRepository
import com.cobalt.android.util.UrlMatcher
import com.cobalt.android.widget.DownloadsWidgetProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

/**
 * The app shell. VidMate-style: a four-tab bottom bar (Home / Library /
 * Shorts / Me) over a full-bleed content host, plus a floating download-queue
 * button and the share/clipboard/shortcut entry points. Settings is reached
 * from the Me tab, not the bottom bar.
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

        findViewById<BottomNavigationView>(R.id.navBottom)
            .setupWithNavController(findNavController(R.id.navHost))
        applyRemoteTabs()
        setupQueueButton()
        observeDownloadQueue()
        handleFirstLaunch()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (settings.clipboardTriggerEnabled) checkClipboard()
    }

    // ── Remote tab config (Phase H3) ─────────────────────────────────────────

    /**
     * Hides bottom-bar items the remote config excludes, so the tab set can be
     * changed centrally (roll a tab out or pull it back) without an app update.
     * A null/absent config leaves all four tabs as shipped.
     */
    private fun applyRemoteTabs() {
        val tabs = RemoteConfigStore.tabs() ?: return
        val nav = findViewById<BottomNavigationView>(R.id.navBottom)
        val byName = mapOf(
            "home" to R.id.nav_home,
            "explore" to R.id.nav_explore,
            "library" to R.id.nav_library,
            "shorts" to R.id.nav_shorts,
            "me" to R.id.nav_me
        )
        byName.forEach { (name, id) ->
            nav.menu.findItem(id)?.isVisible = tabs.contains(name)
        }
    }

    // ── Download queue badge ─────────────────────────────────────────────────

    private fun observeDownloadQueue() {
        queueViewModel.activeDownloads.observe(this) { list ->
            val count = list.size
            if (count > 0) {
                binding.tvBadge.visibility = View.VISIBLE
                binding.tvBadge.text = count.toString()
            } else {
                binding.tvBadge.visibility = View.GONE
            }
            // Keep the home-screen widget (H7) in step with the queue.
            DownloadsWidgetProvider.refresh(this)
        }
    }

    private fun setupQueueButton() {
        binding.fabQueue.setOnClickListener {
            DownloadQueueSheet.newInstance().also { sheet ->
                sheet.onRetry = { record -> submitUrl(record.originalUrl) }
                sheet.show(supportFragmentManager, DownloadQueueSheet.TAG)
            }
        }
    }

    // ── Intent handling ────────────────────────────────────────────────────

    private fun handleIntent(intent: Intent?) {
        when {
            // Open-with / deep link: cobalt://download?url=... or a VIEW of an
            // http(s) link the user chose to open in Cobalt.
            intent?.action == Intent.ACTION_VIEW -> {
                val url = intent.dataString
                    ?.let { UrlMatcher.extractUrl(it) }
                    ?: intent.data?.getQueryParameter("url")
                if (!url.isNullOrBlank()) submitUrl(url) else openInBrowser(intent.dataString.orEmpty())
            }
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val url = UrlMatcher.extractUrl(text)
                if (url != null) {
                    submitUrl(url)
                } else if (!text.isNullOrBlank()) {
                    // Not a host cobalt resolves directly — offer the in-app
                    // browser, which can sniff the media off the page instead.
                    Snackbar.make(
                        binding.root,
                        getString(R.string.not_a_supported_link),
                        Snackbar.LENGTH_LONG
                    ).setAction("browse") { openInBrowser(text) }.show()
                }
            }
            intent?.getStringExtra("shortcut_queue") == "true" ||
            intent?.getBooleanExtra("shortcut_queue", false) == true -> {
                binding.fabQueue.performClick()
            }
            intent?.getStringExtra("shortcut_paste") == "true" ||
            intent?.getBooleanExtra("shortcut_paste", false) == true -> {
                val url = ClipboardHelper.getSupportedUrl(this)
                if (url != null) submitUrl(url)
            }
        }
    }

    // ── Clipboard trigger ──────────────────────────────────────────────────

    private fun checkClipboard() {
        val url = ClipboardHelper.getSupportedUrl(this) ?: return
        Snackbar.make(binding.root, getString(R.string.download_from_clipboard), Snackbar.LENGTH_LONG)
            .setAction("download") { submitUrl(url) }
            .setBackgroundTint(getColor(R.color.cobalt_surface_elevated))
            .setTextColor(getColor(R.color.cobalt_text_primary))
            .setActionTextColor(getColor(R.color.cobalt_accent_blue))
            .show()
    }

    // ── URL routing (no WebView) ─────────────────────────────────────────────

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

    // ── First launch ───────────────────────────────────────────────────────

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
