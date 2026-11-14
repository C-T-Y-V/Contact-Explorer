/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.contactexplorer.ExplorerViewModel
import com.example.contactexplorer.data.ContactTransfer

enum class ExportFormat(val title: String, val extension: String) {
    VCF("vCard format (.vcf)", "vcf"),
    CSV("Spreadsheet CSV (.csv)", "csv"),
    VAULT("Contact Explorer Vault (.cebak)", "cebak")
}

enum class ExportDestination { OFFLINE, ONLINE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportVaultScreen(
    vm: ExplorerViewModel,
    onBack: () -> Unit
) {
    var showAdvancedExportDialog by rememberSaveable { mutableStateOf(false) }
    var showKeepDataVaultDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExportText by rememberSaveable { mutableStateOf<String?>(null) }
    var importFormat by rememberSaveable { mutableStateOf("vcf") }

    val exportVcfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/x-vcard")) { uri ->
        if (uri != null && pendingExportText != null) {
            vm.writeExportToUri(
                uri = uri,
                content = pendingExportText!!,
                onSuccess = { vm.refresh() },
                onError = { /* logged in vm */ }
            )
        }
        pendingExportText = null
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && pendingExportText != null) {
            vm.writeExportToUri(
                uri = uri,
                content = pendingExportText!!,
                onSuccess = { vm.refresh() },
                onError = { /* logged in vm */ }
            )
        }
        pendingExportText = null
    }

    val exportVaultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && pendingExportText != null) {
            vm.writeExportToUri(
                uri = uri,
                content = pendingExportText!!,
                onSuccess = { vm.refresh() },
                onError = { /* logged in vm */ }
            )
        }
        pendingExportText = null
    }

    val importDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importContactsFromUri(uri, importFormat) {
                vm.refresh()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zero-Trust Data Vault", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("100% On-Device Isolation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Zero network permissions requested. Your contacts, groups, tags, and timeline events never leave your local device storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Vault Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Vault Health & Statistics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Contacts:", style = MaterialTheme.typography.bodyMedium)
                        Text("${vm.contacts.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Custom Groups:", style = MaterialTheme.typography.bodyMedium)
                        Text("${vm.groups.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Custom Tags:", style = MaterialTheme.typography.bodyMedium)
                        Text("${vm.tags.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Recycle Bin Items:", style = MaterialTheme.typography.bodyMedium)
                        Text("${vm.deletedContacts.size}", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Export Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Contacts & Vault", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Export in universal vCard (v3.0), Spreadsheet CSV, or full Contact Explorer Vault (.cebak) preserving all custom metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showAdvancedExportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ImportExport, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Export Wizard")
                    }
                }
            }

            // Import Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Import Contacts into Phone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Restore contacts from standard vCard (.vcf), CSV files, or an encrypted/plain JSON Vault backup (.cebak).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                importFormat = "vcf"
                                importDocumentLauncher.launch(arrayOf("text/vcard", "text/x-vcard", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import vCard")
                        }
                        OutlinedButton(
                            onClick = {
                                importFormat = "csv"
                                importDocumentLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import CSV")
                        }
                    }
                    Button(
                        onClick = {
                            importFormat = "vault"
                            importDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restore Complete Vault Backup (.cebak)")
                    }
                }
            }

            // Persistent Safeguard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Persistent Data Safeguard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Keep your contacts safely preserved on device storage so app resets or re-installs never lose custom groups, notes, or tags.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showKeepDataVaultDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Local Safeguards")
                    }
                }
            }
        }
    }

    if (showAdvancedExportDialog) {
        var format by rememberSaveable { mutableStateOf(ExportFormat.VCF) }
        var isOrganized by rememberSaveable { mutableStateOf(true) }
        var destination by rememberSaveable { mutableStateOf(ExportDestination.OFFLINE) }

        val groupMap = remember(vm.memberships, vm.groups) {
            vm.memberships.groupBy { it.contactId }.mapValues { (_, links) ->
                links.mapNotNull { link -> vm.groups.find { it.id == link.groupId }?.name }
            }
        }
        val tagMap = remember(vm.tagAssignments, vm.tags) {
            vm.tagAssignments.groupBy { it.contactId }.mapValues { (_, links) ->
                links.mapNotNull { link -> vm.tags.find { it.id == link.tagId }?.name }
            }
        }

        AlertDialog(
            onDismissRequest = { showAdvancedExportDialog = false },
            title = { Text("Export Contacts Wizard") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select format:", fontWeight = FontWeight.SemiBold)
                    ExportFormat.entries.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = format == f, onClick = { format = f })
                            Spacer(Modifier.width(8.dp))
                            Text(f.title)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Destination:", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = destination == ExportDestination.OFFLINE, onClick = { destination = ExportDestination.OFFLINE })
                        Spacer(Modifier.width(8.dp))
                        Text("Save to Phone Storage (Offline)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = destination == ExportDestination.ONLINE, onClick = { destination = ExportDestination.ONLINE })
                        Spacer(Modifier.width(8.dp))
                        Text("Share via App (Bluetooth/Email/Drive)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdvancedExportDialog = false
                        when (format) {
                            ExportFormat.VAULT -> {
                                val vaultJson = vm.generateVaultBackupJson()
                                if (destination == ExportDestination.OFFLINE) {
                                    pendingExportText = vaultJson
                                    exportVaultLauncher.launch("contact_explorer_vault.cebak")
                                } else {
                                    vm.shareFile("contact_explorer_vault.cebak", vaultJson, "application/json")
                                }
                            }
                            ExportFormat.VCF -> {
                                val vcf = ContactTransfer.toVcf(vm.contacts, groupMap, tagMap, vm.favorites, isOrganized)
                                if (destination == ExportDestination.OFFLINE) {
                                    pendingExportText = vcf
                                    exportVcfLauncher.launch("contacts.vcf")
                                } else {
                                    vm.shareFile("contacts.vcf", vcf, "text/x-vcard")
                                }
                            }
                            ExportFormat.CSV -> {
                                val csv = ContactTransfer.toCsv(vm.contacts, groupMap, tagMap, vm.favorites, isOrganized)
                                if (destination == ExportDestination.OFFLINE) {
                                    pendingExportText = csv
                                    exportCsvLauncher.launch("contacts.csv")
                                } else {
                                    vm.shareFile("contacts.csv", csv, "text/csv")
                                }
                            }
                        }
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvancedExportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showKeepDataVaultDialog) {
        AlertDialog(
            onDismissRequest = { showKeepDataVaultDialog = false },
            title = { Text("Local Storage Safeguard") },
            text = {
                Text(
                    "Contact Explorer stores all custom tags, groups, and contact associations in a sandboxed Room SQLite database located at:\n\n" +
                    "/data/data/com.example.contactexplorer/databases/contact_explorer_vault.db\n\n" +
                    "To create an independent snapshot on external phone storage, use 'Open Export Wizard' -> 'Contact Explorer Vault (.cebak)'."
                )
            },
            confirmButton = {
                TextButton(onClick = { showKeepDataVaultDialog = false }) { Text("Got it") }
            }
        )
    }
}
