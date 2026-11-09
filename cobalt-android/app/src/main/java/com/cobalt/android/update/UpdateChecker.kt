package com.cobalt.android.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Self-update check (Phase I2).
 *
 * The app is not distributed through Play, so an in-app updater is the only way
 * to ship a fix to an installed copy. The check reads an `update` block from the
 * same public `remote-config.json` the host list and tab config already come
 * from, so a new version is published by editing one JSON file — no rebuild of
 * the client needed.
 *
 * Scope: this downloads and *offers* the APK; the actual install is the
 * platform's dialog ([ApkInstaller]). That dialog is the user's consent, and it
 * is the only way to install without Play, so there is no silent-install path
 * and none is attempted. The block is absent by default: with no `update.apkUrl`
 * configured, [check] returns null and the UI simply says the app is up to date,
 * which is the truthful state for a source build.
 */
class UpdateChecker(
    private val currentVersionCode: Int,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {

    data class Update(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val notes: String
    )

    /**
     * Returns the available update, or null when the config has no update block
     * or the published version is not newer than [currentVersionCode].
     */
    suspend fun check(): Update? = withContext(Dispatchers.IO) {
        val json = runCatching { fetchConfig() }.getOrNull() ?: return@withContext null
        parseUpdate(json, currentVersionCode)
    }

    /**
     * Streams [apkUrl] to [dest], returning the file or null on failure. The
     * caller supplies a path inside the app's private files dir so the partial
     * download is never world-readable.
     */
    suspend fun download(apkUrl: String, dest: java.io.File): java.io.File? =
        withContext(Dispatchers.IO) {
            runCatching {
                dest.parentFile?.mkdirs()
                val request = Request.Builder().url(apkUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    val body = response.body ?: return@runCatching null
                    dest.outputStream().use { out -> body.byteStream().copyTo(out) }
                    dest
                }
            }.getOrNull()
        }

    private fun fetchConfig(): JSONObject? {
        val request = Request.Builder().url(CONFIG_URL).build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()?.let { JSONObject(it) }
            }
        }.getOrNull()
    }

    companion object {
        private const val CONFIG_URL =
            "https://raw.githubusercontent.com/Zapier-codes/cobalt-android/master/remote-config.json"

        /**
         * Pure comparison, so the "is this newer?" decision is unit tested
         * rather than eyeballed against a live file. Requires a real apkUrl:
         * a version bump with no download URL is not actionable, so it is not
         * reported as an update.
         */
        fun parseUpdate(config: JSONObject, currentVersionCode: Int): Update? {
            val block = config.optJSONObject("update") ?: return null
            val versionCode = block.optInt("versionCode", 0)
            val apkUrl = block.optString("apkUrl").trim()
            if (versionCode <= currentVersionCode) return null
            if (!apkUrl.startsWith("http://") && !apkUrl.startsWith("https://")) return null
            return Update(
                versionCode = versionCode,
                versionName = block.optString("versionName").trim().ifBlank { versionCode.toString() },
                apkUrl = apkUrl,
                notes = block.optString("notes").trim()
            )
        }
    }
}
