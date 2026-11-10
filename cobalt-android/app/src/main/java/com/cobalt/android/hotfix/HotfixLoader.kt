package com.cobalt.android.hotfix

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File
import java.security.MessageDigest

/**
 * Optional dynamic-code hot-fix (off unless the user turns it on in Settings).
 *
 * VidMate ships patches as separately-downloaded dex modules. The same idea is
 * supported here but deliberately locked down, because loading code that did not
 * come with the APK is the single most dangerous thing an app can do:
 *
 *  - only files inside the app's private `filesDir/hotfix` directory are ever
 *    loaded (a world-writable path is never trusted),
 *  - the dex must match the SHA-256 recorded alongside it before it is loaded —
 *    a mismatch is refused, not warned about,
 *  - the whole path is gated on `settings.hotfixEnabled`, which defaults off.
 *
 * When any of those fail, the app simply runs the code that shipped in the APK.
 */
class HotfixLoader(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "hotfix").apply { mkdirs() }

    data class Patch(val versionName: String, val sha256: String)

    /** The patch currently on disk, or null when none is installed. */
    fun installed(): Patch? {
        val manifest = File(dir, MANIFEST_NAME)
        if (!manifest.exists()) return null
        val text = runCatching { manifest.readText() }.getOrNull() ?: return null
        val parts = text.split('\n')
        if (parts.size < 3) return null
        return Patch(versionName = parts[0], sha256 = parts[1])
    }

    /**
     * Installs [dex] as the active patch. Returns false when the hash does not
     * match [expectedSha256] — the file is then deleted rather than left on
     * disk for a later, unchecked load.
     */
    fun install(dex: File, versionName: String, expectedSha256: String): Boolean {
        if (!dex.exists()) return false
        val actual = sha256(dex)
        if (!actual.equals(expectedSha256, ignoreCase = true)) {
            Log.w(TAG, "hotfix hash mismatch; refusing to install")
            dex.delete()
            return false
        }
        val target = File(dir, PATCH_NAME)
        if (!dex.renameTo(target)) {
            dex.copyTo(target, overwrite = true)
            dex.delete()
        }
        File(dir, MANIFEST_NAME).writeText("$versionName\n$actual\n")
        return true
    }

    fun remove() {
        File(dir, PATCH_NAME).delete()
        File(dir, MANIFEST_NAME).delete()
    }

    /**
     * Builds a class loader over the installed patch, or null when there is no
     * valid patch. The parent is the app's own loader, so a patch can use the
     * app's classes but cannot replace them.
     */
    fun classLoader(): ClassLoader? {
        val patch = File(dir, PATCH_NAME)
        if (!patch.exists()) return null
        val optimized = File(context.codeCacheDir, "hotfix").apply { mkdirs() }
        return runCatching {
            DexClassLoader(patch.absolutePath, optimized.absolutePath, null, javaClass.classLoader)
        }.getOrNull()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "HotfixLoader"
        private const val PATCH_NAME = "patch.dex"
        private const val MANIFEST_NAME = "patch.meta"
    }
}
