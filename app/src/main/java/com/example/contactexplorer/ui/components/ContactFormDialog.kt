/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactDraft
import com.example.contactexplorer.data.LabeledValue

@Composable
fun ContactFormDialog(
    contact: Contact?,
    onDismiss: () -> Unit,
    onSave: (ContactDraft) -> Unit
) {
    var name by rememberSaveable(contact?.id) { mutableStateOf(contact?.name.orEmpty()) }
    val initialPhones = remember(contact) {
        if (contact != null && contact.phones.isNotEmpty()) contact.phones.toMutableStateList()
        else if (contact != null && contact.phone.isNotBlank()) mutableStateListOf(LabeledValue(contact.phone))
        else mutableStateListOf(LabeledValue(""))
    }
    val initialEmails = remember(contact) {
        if (contact != null && contact.emails.isNotEmpty()) contact.emails.toMutableStateList()
        else if (contact != null && contact.email.isNotBlank()) mutableStateListOf(LabeledValue(contact.email, "Home", 1))
        else mutableStateListOf(LabeledValue("", "Home", 1))
    }
    var company by rememberSaveable(contact?.id) { mutableStateOf(contact?.company.orEmpty()) }
    var address by rememberSaveable(contact?.id) { mutableStateOf(contact?.address.orEmpty()) }
    var note by rememberSaveable(contact?.id) { mutableStateOf(contact?.note.orEmpty()) }
    var error by rememberSaveable(contact?.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        title = { Text(if (contact == null) "New contact" else "Edit contact") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(480.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = false },
                        label = { Text("Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = error
                    )
                }

                item { Text("Phone numbers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                itemsIndexed(initialPhones) { index, phoneItem ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = phoneItem.value,
                            onValueChange = { initialPhones[index] = phoneItem.copy(value = it) },
                            label = { Text(if (index == 0) "Primary Phone" else "Phone ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (initialPhones.size > 1) {
                            IconButton(onClick = { initialPhones.removeAt(index) }) {
                                Icon(Icons.Default.Delete, "Remove phone")
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { initialPhones.add(LabeledValue("")) }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add another phone number")
                    }
                }

                item { Text("Email addresses", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
                itemsIndexed(initialEmails) { index, emailItem ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = emailItem.value,
                            onValueChange = { initialEmails[index] = emailItem.copy(value = it) },
                            label = { Text(if (index == 0) "Primary Email" else "Email ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (initialEmails.size > 1) {
                            IconButton(onClick = { initialEmails.removeAt(index) }) {
                                Icon(Icons.Default.Delete, "Remove email")
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { initialEmails.add(LabeledValue("", "Home", 1)) }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add another email address")
                    }
                }

                item { OutlinedTextField(company, { company = it }, label = { Text("Company") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(address, { address = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) error = true
                else {
                    val validPhones = initialPhones.filter { it.value.isNotBlank() }
                    val validEmails = initialEmails.filter { it.value.isNotBlank() }
                    onSave(
                        ContactDraft(
                            name = name.trim(),
                            phone = validPhones.firstOrNull()?.value.orEmpty(),
                            email = validEmails.firstOrNull()?.value.orEmpty(),
                            company = company.trim(),
                            address = address.trim(),
                            note = note.trim(),
                            phones = validPhones,
                            emails = validEmails,
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
