package com.example.contactexplorer.feature.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactGroup
import com.example.contactexplorer.data.OfflineIntelligence

@Composable
fun KeywordCategorizerDialog(
    contacts: List<Contact>,
    groups: List<ContactGroup>,
    onDismiss: () -> Unit,
    onAssign: (String, List<String>) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Auto-detect clusters
    val clusters = remember(contacts) { OfflineIntelligence.detectKeywordClusters(contacts) }

    // Custom keyword state
    var customKeyword by rememberSaveable { mutableStateOf("") }
    var customGroupName by rememberSaveable { mutableStateOf("") }
    val customMatches = remember(customKeyword, contacts) {
        if (customKeyword.isBlank()) emptyList()
        else OfflineIntelligence.findContactsByKeyword(contacts, customKeyword)
    }
    val selectedCustomContactIds = remember(customMatches) {
        mutableStateListOf<String>().apply { addAll(customMatches.map { it.id }) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Keyword Categorizer")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Auto Detect") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Custom Keyword") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // AUTO DETECT TAB
                    if (clusters.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No keyword clusters detected yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Contacts with similar roles (e.g. 'classmate', 'advocate', 'doctor', 'gym') will appear here automatically. You can also use the Custom Keyword tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(clusters) { cluster ->
                                var clusterGroupName by remember(cluster) { mutableStateOf(cluster.suggestedCategory) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                cluster.suggestedCategory,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                            ) {
                                                Text(
                                                    "keyword: ${cluster.keyword}",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }

                                        Text(
                                            "${cluster.contacts.size} contacts found: " +
                                                cluster.contacts.take(4).joinToString(", ") { it.name } +
                                                if (cluster.contacts.size > 4) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        OutlinedTextField(
                                            value = clusterGroupName,
                                            onValueChange = { clusterGroupName = it },
                                            label = { Text("Group Name") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Button(
                                            onClick = {
                                                onAssign(
                                                    clusterGroupName.ifBlank { cluster.suggestedCategory },
                                                    cluster.contacts.map { it.id }
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Group ${cluster.contacts.size} Contacts")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // CUSTOM KEYWORD TAB
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Enter a keyword (e.g., 'class', 'adv', 'college', 'doc') to find all matching contacts and assign them to a group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = customKeyword,
                            onValueChange = {
                                customKeyword = it
                                if (customGroupName.isBlank() && it.isNotBlank()) {
                                    customGroupName = it.trim().replaceFirstChar { char ->
                                        if (char.isLowerCase()) char.titlecase() else char.toString()
                                    }
                                }
                            },
                            label = { Text("Search Keyword") },
                            placeholder = { Text("e.g. class, adv, hospital...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customGroupName,
                            onValueChange = { customGroupName = it },
                            label = { Text("Target Group Name") },
                            placeholder = { Text("Group name to create or update") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (groups.isNotEmpty()) {
                            Text(
                                "Or pick existing group:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groups.take(3).forEach { g ->
                                    val isSelected = customGroupName.equals(g.name, ignoreCase = true)
                                    if (isSelected) {
                                        Button(
                                            onClick = { customGroupName = g.name },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                        ) {
                                            Text(g.name)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { customGroupName = g.name },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                        ) {
                                            Text(g.name)
                                        }
                                    }
                                }
                            }
                        }

                        if (customKeyword.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${customMatches.size} match(es)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (customMatches.isNotEmpty()) {
                                    TextButton(onClick = {
                                        if (selectedCustomContactIds.size == customMatches.size) {
                                            selectedCustomContactIds.clear()
                                        } else {
                                            selectedCustomContactIds.clear()
                                            selectedCustomContactIds.addAll(customMatches.map { it.id })
                                        }
                                    }) {
                                        Text(if (selectedCustomContactIds.size == customMatches.size) "Deselect all" else "Select all")
                                    }
                                }
                            }

                            customMatches.forEach { contact ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = contact.id in selectedCustomContactIds,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedCustomContactIds.add(contact.id)
                                            else selectedCustomContactIds.remove(contact.id)
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            contact.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val sub = contact.phone.ifBlank { contact.company.ifBlank { contact.note } }
                                        if (sub.isNotBlank()) {
                                            Text(
                                                sub,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (customMatches.isEmpty()) {
                                Text(
                                    "No contacts matching '$customKeyword'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    enabled = customGroupName.isNotBlank() && selectedCustomContactIds.isNotEmpty(),
                    onClick = { onAssign(customGroupName.trim(), selectedCustomContactIds.toList()) }
                ) {
                    Text("Group (${selectedCustomContactIds.size})")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
