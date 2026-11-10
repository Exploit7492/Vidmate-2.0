package com.cobalt.android.download

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * The cross-process (AIDL) face of the download engine (D5). It runs in its own
 * `:download` process (see the manifest), holds no state of its own, and
 * forwards every call to the real [DownloadService] — so a client that only has
 * the AIDL interface (another app, or the UI in a future split) can enumerate
 * and start transfers without knowing the service's Intent contract.
 */
class RemoteDownloadService : Service() {

    private val binder = object : IDownloadService.Stub() {
        override fun activeCount(): Int = DownloadService.activeCount()

        override fun startHttps(url: String?, filename: String?, mimeType: String?) {
            if (url.isNullOrBlank()) return
            DownloadService.startHttps(
                this@RemoteDownloadService,
                cobaltUrl = url,
                filename = filename ?: "download",
                mimeType = mimeType ?: "application/octet-stream",
                cookies = "",
                userAgent = "",
                originalUrl = url
            )
        }

        override fun cancel(recordId: Long) {
            DownloadCanceller.cancel(this@RemoteDownloadService, recordId)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
