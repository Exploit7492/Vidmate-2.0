package com.cobalt.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * The several ways Cobalt hands content to another app — the same set VidMate
 * offers from a file's share sheet:
 *
 *  - [shareText] — plain text/link share.
 *  - [shareFile] — send the actual bytes (uses the app's `FileProvider`, so the
 *    receiver can read the file without any storage permission).
 *  - [bluetoothShare] — a share explicitly targeting Bluetooth, for the
 *    "offline / Bluetooth share" entry.
 *  - [shareShortened] — share a shortened link when the user has turned that on
 *    in Settings, falling back to the original link.
 */
object ShareHelper {

    fun shareText(context: Context, text: String, chooserTitle: String = "Share") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Share") {
        val uri = uriFor(context, file) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    /**
     * Shares a `content://` URI the app already holds a grant for (e.g. a
     * MediaStore row from the download DB). [bluetooth] restricts the target to
     * Bluetooth, falling back to the full chooser when no Bluetooth app answers.
     */
    fun shareUri(context: Context, uri: String, mimeType: String, bluetooth: Boolean = false) {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "application/octet-stream" }
            putExtra(Intent.EXTRA_STREAM, parsed)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (bluetooth) setPackage("com.android.bluetooth")
        }
        val title = if (bluetooth) "Share via Bluetooth" else "Share"
        runCatching { context.startActivity(Intent.createChooser(intent, title)) }
            .onFailure {
                // A forced Bluetooth package may not exist; retry unforced.
                if (bluetooth) shareUri(context, uri, mimeType, bluetooth = false)
            }
    }

    /** ACTION_SEND restricted to Bluetooth targets (the "offline share" path). */
    fun bluetoothShare(context: Context, file: File, mimeType: String) {
        val uri = uriFor(context, file) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.android.bluetooth")
        }
        // Not every device has that exact package; fall back to the chooser.
        runCatching { context.startActivity(intent) }
            .onFailure { shareFile(context, file, mimeType, "Share via Bluetooth") }
    }

    /**
     * Shares [url], shortened when [shorten] is on and a shortener host is
     * configured. The shortening is best-effort and synchronous-free: the
     * caller passes the already-shortened link when it has one, otherwise this
     * just shares [url].
     */
    fun shareShortened(context: Context, url: String, shortUrl: String?, shorten: Boolean) {
        val text = if (shorten && !shortUrl.isNullOrBlank()) shortUrl else url
        shareText(context, text, "Share link")
    }

    private fun uriFor(context: Context, file: File): Uri? = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}
