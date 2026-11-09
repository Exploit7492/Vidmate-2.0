package com.cobalt.android.vault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.LiveData
import com.cobalt.android.util.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The private vault (Phase D7/D8): media kept in an app-private directory,
 * behind a passcode.
 *
 * "Private" here means exactly what it says — the files live under
 * `filesDir/vault/`, which no other app can read and which is excluded from the
 * media scanner, so a gallery/file-manager app will not list them. It is *not*
 * encryption, and the code does not pretend otherwise.
 *
 * Import (D8) copies a user-picked document into the vault; export copies a
 * vaulted file back out to a user-picked location. Both stream through
 * `ContentResolver` so nothing is buffered whole in memory.
 */
class VaultRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = VaultDatabase.getInstance(appContext).vaultDao()
    private val settings = SettingsRepository(appContext)

    val entries: LiveData<List<VaultEntry>> = dao.getAllLive()

    fun isConfigured(): Boolean = settings.vaultPasscode.isNotBlank()

    fun setPasscode(passcode: String) {
        settings.vaultPasscode = passcode
    }

    fun checkPasscode(input: String): Boolean =
        settings.vaultPasscode.isNotBlank() && input == settings.vaultPasscode

    private fun vaultDir(): File = File(appContext.filesDir, "vault").apply { mkdirs() }

    fun fileFor(entry: VaultEntry): File = File(vaultDir(), entry.id.toString())

    /**
     * Copies the document at [sourceUri] into the vault and records it.
     * Returns the new entry id, or null if the document could not be read.
     */
    suspend fun importFrom(sourceUri: Uri): Long? = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val name = queryDisplayName(sourceUri) ?: "vaulted_file"
        val mime = resolver.getType(sourceUri) ?: "application/octet-stream"
        // Insert first to get the id that names the file on disk.
        val id = dao.insert(VaultEntry(displayName = name, mimeType = mime))
        val target = File(vaultDir(), id.toString())
        val copied = runCatching {
            resolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
        if (!copied) {
            target.delete()
            dao.deleteById(id)
            return@withContext null
        }
        // Re-insert with the real size so the list can show it.
        dao.deleteById(id)
        val size = target.length()
        val finalId = dao.insert(VaultEntry(id = id, displayName = name, mimeType = mime, sizeBytes = size))
        finalId
    }

    /** Copies a vaulted file back out to [destinationUri]. Returns success. */
    suspend fun exportTo(entry: VaultEntry, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val file = fileFor(entry)
        if (!file.exists()) return@withContext false
        runCatching {
            appContext.contentResolver.openOutputStream(destinationUri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            } != null
        }.getOrDefault(false)
    }

    /** Permanently deletes a vaulted file and its row. */
    suspend fun delete(entry: VaultEntry) = withContext(Dispatchers.IO) {
        fileFor(entry).delete()
        dao.deleteById(entry.id)
    }

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()
}
