package com.example.contactexplorer.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.contactexplorer.data.ContactGroup

@Composable
fun GroupManagerDialog(
    groups: List<ContactGroup>,
    groupCounts: Map<Long, Int>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onRename: (ContactGroup) -> Unit,
    onDelete: (ContactGroup) -> Unit,
    onMergeGroups: () -> Unit,
    onKeywordCategorizer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Contact groups")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onKeywordCategorizer,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Auto Group", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onMergeGroups,
                        modifier = Modifier.weight(1f),
                        enabled = groups.size >= 2
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CallMerge, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Merge", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (groups.isEmpty()) {
                    Text(
                        "No groups yet. Create one or use Auto Group to organize contacts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                groups.forEach { group ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(group.name, fontWeight = FontWeight.Medium)
                            Text(
                                "${groupCounts[group.id] ?: 0} contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRename(group) }) { Icon(Icons.Default.Edit, "Rename ${group.name}") }
                        IconButton(onClick = { onDelete(group) }) { Icon(Icons.Default.Delete, "Delete ${group.name}") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCreate) { Text("New group") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
fun MergeGroupsDialog(
    groups: List<ContactGroup>,
    groupCounts: Map<Long, Int>,
    onDismiss: () -> Unit,
    onMerge: (Long, List<Long>) -> Unit
) {
    if (groups.size < 2) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Merge Groups") },
            text = { Text("You need at least 2 groups to perform a merge.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var targetGroupId by rememberSaveable { mutableStateOf(groups.first().id) }
    var selectedSourceGroupIds by rememberSaveable { mutableStateOf(listOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Merge Groups")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Consolidate contacts from multiple groups into one target group. Merged source groups will be deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Target Group (Keep this group):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                groups.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetGroupId == group.id,
                            onClick = {
                                targetGroupId = group.id
                                selectedSourceGroupIds = selectedSourceGroupIds - group.id
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.name, fontWeight = if (targetGroupId == group.id) FontWeight.Bold else FontWeight.Normal)
                            Text(
                                "${groupCounts[group.id] ?: 0} contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Source Groups to Merge & Delete:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                val eligibleSources = groups.filter { it.id != targetGroupId }
                eligibleSources.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = group.id in selectedSourceGroupIds,
                            onCheckedChange = { checked ->
                                selectedSourceGroupIds = if (checked) selectedSourceGroupIds + group.id else selectedSourceGroupIds - group.id
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.name)
                            Text(
                                "${groupCounts[group.id] ?: 0} contacts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (selectedSourceGroupIds.isNotEmpty()) {
                    val targetName = groups.firstOrNull { it.id == targetGroupId }?.name ?: "Target"
                    val totalSourceContacts = selectedSourceGroupIds.sumOf { groupCounts[it] ?: 0 }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "Merging ${selectedSourceGroupIds.size} group(s) (~$totalSourceContacts contacts) into '$targetName'.",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedSourceGroupIds.isNotEmpty(),
                onClick = { onMerge(targetGroupId, selectedSourceGroupIds) }
            ) {
                Text("Merge (${selectedSourceGroupIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GroupPickerDialog(
    title: String,
    groups: List<ContactGroup>,
    selectedGroupId: Long?,
    mixedSelection: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ungrouped", Modifier.weight(1f))
                    if (!mixedSelection && selectedGroupId == null) Icon(Icons.Default.Check, null)
                }
                groups.forEach { group ->
                    TextButton(onClick = { onSelect(group.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(group.name, Modifier.weight(1f))
                        if (!mixedSelection && selectedGroupId == group.id) Icon(Icons.Default.Check, null)
                    }
                }
                if (groups.isEmpty()) {
                    Text("Create a group from the Groups tab first.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
