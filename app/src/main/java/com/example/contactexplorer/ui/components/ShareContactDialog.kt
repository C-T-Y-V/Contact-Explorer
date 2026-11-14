/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.contactexplorer.data.Contact

@Composable
fun ShareContactDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onShare: (Set<String>) -> Unit
) {
    val fields = listOf(
        "phone" to "Phone",
        "email" to "Email",
        "company" to "Company",
        "address" to "Address",
        "notes" to "Notes"
    )
    var selected by rememberSaveable { mutableStateOf(fields.map { it.first }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (contacts.size == 1) "Share ${contacts.firstOrNull()?.name.orEmpty()}" else "Share ${contacts.size} contacts")
        },
        text = {
            Column {
                Text("Choose which fields to include. Names are always shared.", style = MaterialTheme.typography.bodySmall)
                fields.forEach { (key, label) ->
                    val available = contacts.any { contact ->
                        when (key) {
                            "phone" -> contact.phone.isNotBlank()
                            "email" -> contact.email.isNotBlank()
                            "company" -> contact.company.isNotBlank()
                            "address" -> contact.address.isNotBlank()
                            else -> contact.note.isNotBlank()
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = key in selected,
                            enabled = available,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + key else selected - key
                            }
                        )
                        Text(
                            label,
                            color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onShare(selected.toSet()) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
