// AIDL contract for the cross-process download service (D5). The heavy
// download/transcode work runs in a separate `:download` process; the UI and
// other apps bind here rather than starting an Intent per transfer.
package com.cobalt.android.download;

interface IDownloadService {
    /** Number of transfers currently running. */
    int activeCount();

    /** Enqueue an HTTP(S) download. */
    void startHttps(String url, String filename, String mimeType);

    /** Cancel an in-progress transfer by its record id. */
    void cancel(long recordId);
}
