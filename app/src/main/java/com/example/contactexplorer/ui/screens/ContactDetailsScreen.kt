/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.feature.groups.GroupPickerDialog
import com.example.contactexplorer.ui.components.Avatar
import com.example.contactexplorer.ui.components.DetailLine
import com.example.contactexplorer.ui.components.EmptyState
import com.example.contactexplorer.ui.components.ShareContactDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    contactId: String,
    vm: ExplorerViewModel,
    onBack: () -> Unit,
    onEditContact: (Contact) -> Unit
) {
    val contact = vm.contacts.find { it.id == contactId }

    LaunchedEffect(contactId) {
        vm.markViewed(contactId)
    }

    if (contact == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Contact Not Found") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                EmptyState(
                    title = "Contact Unavailable",
                    message = "This contact may have been deleted or moved.",
                    action = onBack,
                    actionLabel = "Back to list"
                )
            }
        }
        return
    }

    val isFavorite = contact.id in vm.favorites
    val assignedGroups = vm.groups.filter { g ->
        vm.memberships.any { it.contactId == contact.id && it.groupId == g.id }
    }
    val assignedTags = vm.tags.filter { t ->
        vm.tagAssignments.any { it.contactId == contact.id && it.tagId == t.id }
    }

    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showGroupPicker by rememberSaveable { mutableStateOf(false) }
    var showShareDialog by rememberSaveable { mutableStateOf(false) }

    val phones = remember(contact) {
        if (contact.phones.isNotEmpty()) contact.phones
        else if (contact.phone.isNotBlank()) listOf(LabeledValue(contact.phone, "Mobile", 2))
        else emptyList()
    }

    val emails = remember(contact) {
        if (contact.emails.isNotEmpty()) contact.emails
        else if (contact.email.isNotBlank()) listOf(LabeledValue(contact.email, "Home", 1))
        else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite(contact.id) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share contact")
                    }
                    if (vm.canWriteContacts) {
                        IconButton(onClick = { onEditContact(contact) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit contact")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete contact", tint = MaterialTheme.colorScheme.error)
                        }
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
            // Profile Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(contact.name, size = 80.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (contact.company.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = contact.company,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Quick Call/Message Row
            if (phones.isNotEmpty()) {
                val primaryPhone = phones.first().value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.launchCall(primaryPhone) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call")
                    }
                    OutlinedButton(
                        onClick = { vm.launchSms(primaryPhone) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Message, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Message")
                    }
                }
            }

            // Phones Card
            if (phones.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Phone Numbers",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        phones.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(p.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { vm.launchCall(p.value) }) {
                                    Icon(Icons.Default.Call, "Call ${p.value}", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { vm.launchSms(p.value) }) {
                                    Icon(Icons.Default.Message, "Message ${p.value}", tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }

            // Emails Card
            if (emails.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Email Addresses",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        emails.forEach { e ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(e.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(e.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { vm.launchEmail(e.value) }) {
                                    Icon(Icons.Default.Email, "Email ${e.value}", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // Details & Notes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Contact Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    DetailLine("Address", contact.address)
                    DetailLine("Company", contact.company)
                    DetailLine("Notes", contact.note)
                    DetailLine("Groups", assignedGroups.joinToString { it.name }.ifBlank { "Ungrouped" })
                    DetailLine("Tags", assignedTags.joinToString { it.name }.ifBlank { "None" })
                }
            }

            // Group Assignment Action Button
            OutlinedButton(
                onClick = { showGroupPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Folder, null)
                Spacer(Modifier.width(8.dp))
                Text("Move to group")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete '${contact.name}'? It will be moved to the local Recycle Bin and removed from your phone address book.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        vm.delete(contact.id)
                        onBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showGroupPicker) {
        val currentGroupId = vm.memberships.find { it.contactId == contact.id }?.groupId
        GroupPickerDialog(
            title = "Assign Group for ${contact.name}",
            groups = vm.groups,
            selectedGroupId = currentGroupId,
            mixedSelection = false,
            onDismiss = { showGroupPicker = false },
            onSelect = { groupId ->
                vm.moveToGroup(listOf(contact.id), groupId)
                showGroupPicker = false
            }
        )
    }

    if (showShareDialog) {
        ShareContactDialog(
            contacts = listOf(contact),
            onDismiss = { showShareDialog = false },
            onShare = { fields ->
                showShareDialog = false
                val phoneText = if ("phone" in fields) phones.joinToString("; ") { it.value } else ""
                val emailText = if ("email" in fields) emails.joinToString("; ") { it.value } else ""
                val content = buildString {
                    appendLine("Contact: ${contact.name}")
                    if (phoneText.isNotBlank()) appendLine("Phone: $phoneText")
                    if (emailText.isNotBlank()) appendLine("Email: $emailText")
                    if ("company" in fields && contact.company.isNotBlank()) appendLine("Company: ${contact.company}")
                    if ("address" in fields && contact.address.isNotBlank()) appendLine("Address: ${contact.address}")
                    if ("notes" in fields && contact.note.isNotBlank()) appendLine("Notes: ${contact.note}")
                }
                vm.shareText("Contact: ${contact.name}", content)
            }
        )
    }
}
