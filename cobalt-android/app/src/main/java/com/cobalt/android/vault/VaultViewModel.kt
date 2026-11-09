package com.cobalt.android.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Backs the Vault screen (Phase D7/D8). Owns the unlock state, the entry list,
 * and the import/export/delete operations — all the state the Compose screen
 * and the SAF pickers in [VaultFragment] need.
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(application)

    val entries: LiveData<List<VaultEntry>> = repository.entries

    fun isConfigured(): Boolean = repository.isConfigured()

    fun setPasscode(passcode: String) = repository.setPasscode(passcode)

    fun checkPasscode(input: String): Boolean = repository.checkPasscode(input)

    /** Imports a picked document; [onDone] is called with success/failure. */
    fun import(uri: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(repository.importFrom(uri) != null)
        }
    }

    fun export(entry: VaultEntry, destination: Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(repository.exportTo(entry, destination)) }
    }

    fun delete(entry: VaultEntry) {
        viewModelScope.launch { repository.delete(entry) }
    }

    fun fileFor(entry: VaultEntry) = repository.fileFor(entry)
}
