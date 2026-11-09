package com.cobalt.android.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Vault screen (Phase D7). Two states: a passcode gate, then the list of
 * vaulted files with add / export / delete actions.
 *
 * The gate is a real check against the stored passcode (see
 * `VaultRepository.checkPasscode`); the vault's data protection itself is the
 * app-private directory, not this screen — the gate stops casual access by
 * whoever is holding the phone, which is the threat it is meant to address.
 */
@Composable
fun VaultScreen(
    locked: Boolean,
    configured: Boolean,
    entries: List<VaultEntry>,
    onUnlock: (String) -> Boolean,
    onCreatePasscode: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (VaultEntry) -> Unit,
    onExport: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit
) {
    if (locked) {
        PasscodeGate(configured, onUnlock, onCreatePasscode)
        return
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Vault",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onAdd) { Text("add file") }
        }
        Spacer(Modifier.height(12.dp))
        if (entries.isEmpty()) {
            Text(
                "No vaulted files yet. Add one to keep it out of your gallery.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries, key = { it.id }) { e ->
                VaultRow(e, onOpen, onExport, onDelete)
            }
        }
    }
}

@Composable
private fun PasscodeGate(
    configured: Boolean,
    onUnlock: (String) -> Boolean,
    onCreatePasscode: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (configured) "Enter vault passcode" else "Create a vault passcode",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = false },
            singleLine = true,
            isError = error,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            label = { Text("passcode") }
        )
        if (error) {
            Spacer(Modifier.height(6.dp))
            Text("wrong passcode", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (input.isBlank()) return@Button
            if (configured) {
                if (!onUnlock(input)) error = true
            } else {
                onCreatePasscode(input)
            }
        }) { Text(if (configured) "unlock" else "create") }
    }
}

@Composable
private fun VaultRow(
    entry: VaultEntry,
    onOpen: (VaultEntry) -> Unit,
    onExport: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(
                "%.1f MB".format(entry.sizeBytes / 1_048_576.0),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        TextButton(onClick = { onOpen(entry) }) { Text("open") }
        TextButton(onClick = { onExport(entry) }) { Text("export") }
        TextButton(onClick = { onDelete(entry) }) { Text("delete") }
    }
}
