package com.cobalt.android.vault

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cobalt.android.databinding.FragmentVaultBinding

/**
 * Host for [VaultScreen] (Phase D7/D8). Owns the SAF pickers:
 *
 *  - **import** — `OpenDocument` to choose a file to copy into the vault;
 *  - **export** — `CreateDocument` to choose where a vaulted file is copied
 *    back out;
 *  - **open** — hands the vaulted file to an external viewer as a
 *    `FileProvider` `content://` URI (never a world-readable file path).
 *
 * The unlock state is held here (not in the ViewModel) because it should reset
 * every time the screen is left — closing the screen re-locks the vault.
 */
class VaultFragment : Fragment() {

    private var _binding: FragmentVaultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VaultViewModel by viewModels()

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.import(it) { _ -> } } }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri -> pendingExport?.let { entry -> uri?.let { viewModel.export(entry, it) { _ -> } } }; pendingExport = null }

    private var pendingExport: VaultEntry? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVaultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.vaultCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.vaultCompose.setContent {
            val entries by viewModel.entries.observeAsState(emptyList())
            // Re-lock whenever this fragment is rebuilt (fresh process/nav).
            var locked by remember { mutableStateOf(true) }
            val configured = remember { viewModel.isConfigured() }

            VaultScreen(
                locked = locked,
                configured = configured,
                entries = entries,
                onUnlock = { input -> viewModel.checkPasscode(input).also { if (it) locked = false } },
                onCreatePasscode = { viewModel.setPasscode(it); locked = false },
                onAdd = { importLauncher.launch(arrayOf("*/*")) },
                onOpen = { entry -> openVaulted(entry) },
                onExport = { entry ->
                    pendingExport = entry
                    exportLauncher.launch(entry.displayName)
                },
                onDelete = { viewModel.delete(it) }
            )
        }
    }

    private fun openVaulted(entry: VaultEntry) {
        val file = viewModel.fileFor(entry)
        if (!file.exists()) return
        val uri: Uri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, entry.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
