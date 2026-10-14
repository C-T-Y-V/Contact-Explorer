package com.example.contactexplorer.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.contactexplorer.ExplorerViewModel
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactTag
import com.example.contactexplorer.data.OfflineIntelligence
import com.example.contactexplorer.data.SmartGroupRule
import java.text.DateFormat
import java.util.Date

class BuiltInFeatureScreens(
    private val vm: ExplorerViewModel,
    private val onAddTagToContacts: (List<String>) -> Unit,
    private val onAssignTag: (List<String>, Long) -> Unit,
    private val onImportCsv: () -> Unit,
    private val onImportVcf: () -> Unit,
    private val onExportCsv: (List<String>?) -> Unit,
    private val onExportVcf: (List<String>?) -> Unit,
    private val onDeleteContacts: (List<String>) -> Unit,
    private val onShareContacts: (List<String>) -> Unit,
) : ExplorerPlugin {
    override val id = "v2-v3-screens"
    override val features = listOf(
        FeatureDescriptor("tags", "Tags", "Create tags and organize contacts with labels.", FeatureCategory.ORGANIZATION),
        FeatureDescriptor("recycle-bin", "Recycle Bin", "Restore deleted contacts or remove their saved copies.", FeatureCategory.DATA),
        FeatureDescriptor("duplicates", "Duplicates", "Review contacts with matching names, phone numbers, or email.", FeatureCategory.INSIGHTS),
        FeatureDescriptor("timeline", "Timeline", "Review recent contact activity stored on this device.", FeatureCategory.INSIGHTS),
        FeatureDescriptor("smart-groups", "Smart Groups", "Save local rules that automatically group matching contacts.", FeatureCategory.ORGANIZATION),
        FeatureDescriptor("graph", "Graph", "Explore links shared through groups and tags.", FeatureCategory.GRAPH),
        FeatureDescriptor("transfer", "Import / Export", "Import and export contacts as CSV or vCard.", FeatureCategory.DATA),
    )

    private fun feature(id: String) = features.single { it.id == id }

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature("tags")) {
            screen(ScreenContribution("tags", "tags", "Tags", Icons.Default.Label) { TagsFeatureScreen(vm, onAssignTag) })
            selectionAction(SelectionActionContribution("bulk.add-tag", "tags", "Add tag", Icons.Default.Label, onAddTagToContacts))
            contactAction(ContactActionContribution("contact.add-tag", "tags", "Add tag", Icons.Default.Label, invoke = { onAddTagToContacts(listOf(it.id)) }))
        }
        registry.feature(feature("recycle-bin")) {
            screen(ScreenContribution("recycle-bin", "recycle-bin", "Recycle Bin", Icons.Default.Restore) { RecycleBinFeatureScreen(vm) })
            selectionAction(SelectionActionContribution("bulk.delete", "recycle-bin", "Delete", Icons.Default.Delete, onDeleteContacts))
        }
        registry.feature(feature("duplicates")) {
            screen(ScreenContribution("duplicates", "duplicates", "Duplicates", Icons.Default.Difference) { DuplicateFeatureScreen(vm) })
        }
        registry.feature(feature("timeline")) {
            screen(ScreenContribution("timeline", "timeline", "Timeline", Icons.Default.History) { TimelineFeatureScreen(vm) })
        }
        registry.feature(feature("smart-groups")) {
            screen(ScreenContribution("smart-groups", "smart-groups", "Smart Groups", Icons.Default.AutoAwesome) { SmartGroupsFeatureScreen(vm) })
        }
        registry.feature(feature("graph")) {
            screen(ScreenContribution("relationships", "graph", "Relationships", Icons.Default.Schema) { RelationshipsFeatureScreen(vm) })
        }
        registry.feature(feature("transfer")) {
            screen(ScreenContribution("transfer", "transfer", "Import / Export", Icons.Default.ImportExport) {
                ImportExportFeatureScreen(vm, onImportCsv, onImportVcf, onExportCsv, onExportVcf)
            })
            selectionAction(SelectionActionContribution("bulk.share", "transfer", "Share", Icons.Default.Share, onShareContacts))
            selectionAction(SelectionActionContribution("bulk.export-vcf", "transfer", "Export VCF", Icons.Default.ImportExport) { onExportVcf(it) })
            selectionAction(SelectionActionContribution("bulk.export-csv", "transfer", "Export CSV", Icons.Default.ImportExport) { onExportCsv(it) })
        }
    }
}

@Composable
private fun TagsFeatureScreen(vm: ExplorerViewModel, onAssign: (List<String>, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedTagId by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Tags", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Create local tags and apply them to one or more contacts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("New tag") }, modifier = Modifier.weight(1f), singleLine = true)
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.saveTag(name); name = "" }) { Text("Add") }
        }
        OutlinedButton(onClick = vm::autoTagContacts, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("Suggest tags on device")
        }
        Text("Tags are stored locally and never sent to a cloud service.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(vm.tags, key = ContactTag::id) { tag ->
                val assigned = vm.tagAssignments.filter { it.tagId == tag.id }.mapNotNull { link -> vm.contacts.find { it.id == link.contactId } }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(tag.name, fontWeight = FontWeight.SemiBold); Text("${assigned.size} contacts", style = MaterialTheme.typography.bodySmall) }
                            TextButton(onClick = { selectedTagId = tag.id }) { Text("Apply") }
                            IconButton(onClick = { vm.deleteTag(tag.id) }) { Icon(Icons.Default.Delete, "Delete tag") }
                        }
                        if (assigned.isNotEmpty()) Text(assigned.joinToString { it.name }, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
    }
    selectedTagId?.let { tagId ->
        ContactPickerDialog(vm.contacts, onDismiss = { selectedTagId = null }, onPick = { selected -> onAssign(selected, tagId); selectedTagId = null })
    }
}

@Composable
private fun ContactPickerDialog(contacts: List<Contact>, onDismiss: () -> Unit, onPick: (List<String>) -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply tag to contacts") },
        text = { LazyColumn { items(contacts, key = Contact::id) { contact ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(checked = contact.id in selected, onCheckedChange = { checked -> selected = if (checked) selected + contact.id else selected - contact.id })
                Text(contact.name)
            }
        } } },
        confirmButton = { TextButton(onClick = { onPick(selected.toList()) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RecycleBinFeatureScreen(vm: ExplorerViewModel) {
    var permanent by remember { mutableStateOf<com.example.contactexplorer.data.DeletedContact?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Recycle Bin", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Deleted contacts are kept for 30 days, then removed automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Text(
                "ℹ️ Deleting contacts removes them from your phone address book and synced cloud accounts (e.g. Google). Restoring will re-add them as local contacts with all saved numbers and details preserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }
        if (vm.deletedContacts.isEmpty()) Text("Recycle Bin is empty.", modifier = Modifier.padding(top = 24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(vm.deletedContacts, key = { it.contactId }) { contact ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, fontWeight = FontWeight.SemiBold)
                            Text("Deleted ${DateFormat.getDateTimeInstance().format(Date(contact.deletedAt))}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { vm.restoreDeleted(contact) }) { Icon(Icons.Default.Restore, null); Text("Restore") }
                        IconButton(onClick = { permanent = contact }) { Icon(Icons.Default.Delete, "Delete permanently") }
                    }
                }
            }
        }
    }
    permanent?.let { contact -> AlertDialog(
        onDismissRequest = { permanent = null }, title = { Text("Delete ${contact.name} permanently?") },
        text = { Text("This removes the saved restore copy from Contact Explorer.") },
        confirmButton = { TextButton(onClick = { vm.permanentlyDelete(contact); permanent = null }) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = { permanent = null }) { Text("Cancel") } },
    ) }
}

@Composable
private fun DuplicateFeatureScreen(vm: ExplorerViewModel) {
    val groups = OfflineIntelligence.findDuplicateGroups(vm.contacts)
    var mergeCandidate by remember { mutableStateOf<com.example.contactexplorer.data.DuplicateGroup?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Possible duplicates", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Matches are found locally by matching phone numbers, email addresses, and similar names.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (groups.isEmpty()) Text("No possible duplicates found.", modifier = Modifier.padding(top = 24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(groups, key = { group -> group.contacts.map(Contact::id).sorted().joinToString("-") }) { group ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(group.reason, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Text("Confidence: ${group.confidence}% · ${group.contacts.size} contacts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { mergeCandidate = group }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                Icon(Icons.Default.CallMerge, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Merge")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        group.contacts.forEach { contact ->
                            val phones = if (contact.phones.isNotEmpty()) contact.phones.joinToString(", ") { it.value } else contact.phone
                            val subtext = listOf(phones, contact.email, contact.company).filter { it.isNotBlank() }.joinToString(" • ")
                            Text("${contact.name}${if (subtext.isNotBlank()) " — $subtext" else ""}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    mergeCandidate?.let { group ->
        val primary = group.contacts.first()
        val others = group.contacts.drop(1)
        AlertDialog(
            onDismissRequest = { mergeCandidate = null },
            title = { Text("Merge ${group.contacts.size} contacts?") },
            text = {
                Text("All phone numbers, email addresses, notes, tags, and groups from '${others.joinToString { it.name }}' will be consolidated into '${primary.name}', and redundant duplicate entries will be removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.mergeContacts(target = primary, sources = group.contacts)
                    mergeCandidate = null
                }) { Text("Merge") }
            },
            dismissButton = { TextButton(onClick = { mergeCandidate = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TimelineFeatureScreen(vm: ExplorerViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Contact timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Recent views, edits, calls, shares, imports, and recycle-bin actions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(vm.events, key = { it.id }) { event ->
                Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) { Text("${event.contactName} · ${event.action.lowercase().replace('_', ' ')}", fontWeight = FontWeight.Medium); Text(event.details, style = MaterialTheme.typography.bodySmall) }
                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.occurredAt)), style = MaterialTheme.typography.labelSmall)
                } }
            }
        }
    }
}

@Composable
private fun SmartGroupsFeatureScreen(vm: ExplorerViewModel) {
    var showCreate by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Smart Groups", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Groups update automatically from local contact fields.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { showCreate = true }, modifier = Modifier.padding(top = 10.dp)) { Text("Create smart group") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(vm.smartGroups, key = SmartGroupRule::id) { rule ->
                val matching = vm.contacts.filter { contact -> matchesSmartRule(vm, rule, contact) }
                Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(rule.name, fontWeight = FontWeight.SemiBold); Text("${rule.field}: ${rule.value} · ${matching.size} contacts", style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = { vm.deleteSmartGroup(rule.id) }) { Icon(Icons.Default.Delete, "Delete smart group") }
                } }
            }
        }
    }
    if (showCreate) SmartGroupDialog(onDismiss = { showCreate = false }, onCreate = { name, field, value -> vm.createSmartGroup(name, field, value); showCreate = false })
}

private fun matchesSmartRule(vm: ExplorerViewModel, rule: SmartGroupRule, contact: Contact): Boolean = when (rule.field) {
    "company" -> contact.company.contains(rule.value, true)
    "email" -> contact.email.contains(rule.value, true)
    "city" -> contact.address.contains(rule.value, true)
    "notes" -> contact.note.contains(rule.value, true)
    "group" -> vm.memberships.filter { it.contactId == contact.id }.any { member -> vm.groups.find { it.id == member.groupId }?.name?.contains(rule.value, true) == true }
    "tag" -> vm.tagAssignments.filter { it.contactId == contact.id }.any { member -> vm.tags.find { it.id == member.tagId }?.name?.contains(rule.value, true) == true }
    else -> false
}

@Composable
private fun SmartGroupDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var field by remember { mutableStateOf("company") }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create smart group") }, text = {
        Column {
            OutlinedTextField(name, { name = it }, label = { Text("Group name") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedButton(onClick = { menu = true }) { Text("Match ${field}") }
                DropdownMenu(menu, { menu = false }) { listOf("company", "email", "city", "notes", "group", "tag").forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { field = item; menu = false }) } }
            }
            OutlinedTextField(value, { value = it }, label = { Text("Contains") }, singleLine = true)
        }
    }, confirmButton = { TextButton(onClick = { if (name.isNotBlank() && value.isNotBlank()) onCreate(name, field, value) }) { Text("Create") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun RelationshipsFeatureScreen(vm: ExplorerViewModel) {
    val relations = vm.groups.map { group ->
        "Group · ${group.name}" to vm.memberships.filter { it.groupId == group.id }.mapNotNull { link -> vm.contacts.find { it.id == link.contactId }?.name }
    } + vm.tags.mapNotNull { tag ->
        val names = vm.tagAssignments.filter { it.tagId == tag.id }.mapNotNull { link -> vm.contacts.find { it.id == link.contactId }?.name }
        if (names.size > 1) "Tag · ${tag.name}" to names else null
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Relationship map", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Contacts are linked when they share a group or tag. This map stays on your device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(relations, key = { it.first }) { (title, names) ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(names.ifEmpty { listOf("No contacts") }.joinToString("  ↔  ")) } }
            }
        }
    }
}

@Composable
private fun ImportExportFeatureScreen(
    vm: ExplorerViewModel,
    onImportCsv: () -> Unit,
    onImportVcf: () -> Unit,
    onExportCsv: (List<String>?) -> Unit,
    onExportVcf: (List<String>?) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Contact portability", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Import contacts from CSV or vCard, or export the phone contacts managed by this app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onImportVcf, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.ImportExport, null); Spacer(Modifier.width(8.dp)); Text("Import VCF") }
        Button(onClick = onImportCsv, modifier = Modifier.fillMaxWidth()) { Text("Import CSV") }
        OutlinedButton(onClick = { onExportVcf(null) }, modifier = Modifier.fillMaxWidth()) { Text("Export all as VCF") }
        OutlinedButton(onClick = { onExportCsv(null) }, modifier = Modifier.fillMaxWidth()) { Text("Export all as CSV") }
        Text("${vm.contacts.size} contacts available", style = MaterialTheme.typography.bodySmall)
    }
}
