/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.contactexplorer.ExplorerViewModel
import com.example.contactexplorer.FilterMode
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.SearchRequest
import com.example.contactexplorer.feature.ai.KeywordCategorizerDialog
import com.example.contactexplorer.feature.groups.GroupManagerDialog
import com.example.contactexplorer.feature.groups.GroupPickerDialog
import com.example.contactexplorer.feature.groups.GroupStateLogic
import com.example.contactexplorer.feature.groups.MergeGroupsDialog
import com.example.contactexplorer.plugins.FeatureDescriptor
import com.example.contactexplorer.plugins.KotlinSearchEngine
import com.example.contactexplorer.ui.components.ConfirmDialog
import com.example.contactexplorer.ui.components.ContactFormDialog
import com.example.contactexplorer.ui.components.ContactRow
import com.example.contactexplorer.ui.components.ContactTile
import com.example.contactexplorer.ui.components.EmptyState
import com.example.contactexplorer.ui.components.GroupCard
import com.example.contactexplorer.ui.components.GroupDialogState
import com.example.contactexplorer.ui.components.Section
import com.example.contactexplorer.ui.components.SelectionDockItem
import com.example.contactexplorer.ui.components.ShareContactDialog
import com.example.contactexplorer.ui.components.SortMode
import com.example.contactexplorer.ui.components.TagPickerDialog
import com.example.contactexplorer.ui.components.TextEntryDialog
import com.example.contactexplorer.ui.components.UngroupedCard

private val NullableStringListSaver = listSaver<List<String>?, String>(
    save = { list -> list ?: listOf("__CE_NULL__") },
    restore = { list -> if (list.size == 1 && list[0] == "__CE_NULL__") null else list }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    vm: ExplorerViewModel,
    activeFeatureIds: Set<String>,
    registeredFeatures: List<FeatureDescriptor>,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFeature: (String) -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refresh() }

    val uiState by vm.uiState.collectAsState()
    val selectedIds = uiState.selectedIds

    var section by rememberSaveable { mutableStateOf(Section.ALL) }
    var groupFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var grid by rememberSaveable { mutableStateOf(false) }
    var sort by rememberSaveable { mutableStateOf(SortMode.RELEVANCE) }
    var sortMenu by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showTitleDetailDialog by rememberSaveable { mutableStateOf(false) }

    // Dialog state
    var editContactId by rememberSaveable { mutableStateOf<String?>(null) }
    val editContact = editContactId?.let { id -> vm.contacts.find { it.id == id } }
    var showContactForm by rememberSaveable { mutableStateOf(false) }
    var showGroupManager by rememberSaveable { mutableStateOf(false) }
    var showMergeGroupsDialog by rememberSaveable { mutableStateOf(false) }
    var showKeywordCategorizerDialog by rememberSaveable { mutableStateOf(false) }
    var groupDialog by rememberSaveable { mutableStateOf<GroupDialogState?>(null) }
    var assignContactIds by rememberSaveable(stateSaver = NullableStringListSaver) { mutableStateOf<List<String>?>(null) }
    var showTagPickerFor by rememberSaveable(stateSaver = NullableStringListSaver) { mutableStateOf<List<String>?>(null) }
    var shareContactIds by rememberSaveable(stateSaver = NullableStringListSaver) { mutableStateOf<List<String>?>(null) }
    var deleteIdsTarget by rememberSaveable(stateSaver = NullableStringListSaver) { mutableStateOf<List<String>?>(null) }

    // Calling permission launcher
    var pendingCallNumber by rememberSaveable { mutableStateOf<String?>(null) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        vm.refresh()
        pendingCallNumber?.let { number ->
            vm.launchCall(number)
            pendingCallNumber = null
        }
    }

    val requestCall: (Contact) -> Unit = { contact ->
        val number = contact.phone.ifBlank { contact.phones.firstOrNull()?.value.orEmpty() }
        if (number.isNotBlank()) {
            if (vm.canCallPhone) {
                vm.launchCall(number)
            } else {
                pendingCallNumber = number
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    // Back handling
    BackHandler(selectedIds.isNotEmpty()) { vm.clearSelection() }
    BackHandler(selectedIds.isEmpty() && section == Section.GROUPS && groupFilter != null) { groupFilter = null }

    // Filter section contacts
    val sectionContacts = when (section) {
        Section.ALL -> vm.contacts
        Section.FAVORITES -> vm.contacts.filter { it.id in vm.favorites }
        Section.RECENT -> vm.recentIds.mapNotNull { id -> vm.contacts.find { it.id == id } }
        Section.GROUPS -> when (groupFilter) {
            null -> emptyList()
            -1L -> vm.contacts.filter { c -> vm.memberships.none { it.contactId == c.id } }
            else -> vm.contacts.filter { contact ->
                vm.memberships.any { it.contactId == contact.id && it.groupId == groupFilter }
            }
        }
    }

    val groupNames = remember(vm.memberships, vm.groups) {
        vm.memberships.groupBy { it.contactId }.mapValues { (_, links) ->
            links.mapNotNull { link -> vm.groups.find { it.id == link.groupId }?.name }
        }
    }
    val tagNames = remember(vm.tagAssignments, vm.tags) {
        vm.tagAssignments.groupBy { it.contactId }.mapValues { (_, links) ->
            links.mapNotNull { link -> vm.tags.find { it.id == link.tagId }?.name }
        }
    }

    val searchEngine = remember { KotlinSearchEngine() }
    val searchedContacts = searchEngine.search(
        SearchRequest(
            query = search,
            contacts = sectionContacts,
            groupNames = groupNames,
            tagNames = tagNames,
            favorites = vm.favorites,
            recency = vm.recentIds.mapIndexed { index, id -> id to (vm.recentIds.size - index) }.toMap(),
        )
    )

    val contacts = searchedContacts.let { filtered ->
        when (sort) {
            SortMode.RELEVANCE -> if (search.isBlank()) filtered.sortedBy { it.name.lowercase() } else filtered
            SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortMode.MODIFIED -> filtered.sortedByDescending { it.lastModified }
            SortMode.MOST_CONTACTED -> filtered.sortedByDescending { it.timesContacted }
            SortMode.LEAST_CONTACTED -> filtered.sortedBy { it.timesContacted }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { vm.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 2.dp
                            ) {
                                TextButton(
                                    onClick = {
                                        if (selectedIds.size == contacts.size) vm.clearSelection()
                                        else vm.selectAll(contacts.map { it.id })
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (selectedIds.size == contacts.size) "Clear all" else "Select all",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        if (section == Section.GROUPS && groupFilter != null) {
                            IconButton(onClick = { groupFilter = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "All groups")
                            }
                        }
                    },
                    title = {
                        val currentTitle = when (section) {
                            Section.ALL -> "Contact Explorer"
                            Section.FAVORITES -> "Favorites"
                            Section.RECENT -> "Recent"
                            Section.GROUPS -> groupFilter?.let { id ->
                                if (id == -1L) "Ungrouped Contacts" else vm.groups.find { it.id == id }?.name
                            } ?: "Groups"
                        }
                        val subtitleText = when {
                            section == Section.ALL -> "${contacts.size} contacts"
                            section == Section.GROUPS && groupFilter == null -> "${vm.groups.size} groups"
                            section == Section.GROUPS && groupFilter != null -> "${contacts.size} contacts"
                            else -> null
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTitleDetailDialog = true }
                        ) {
                            Text(
                                text = currentTitle,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(5.dp)
                                        ) {}
                                        Text(
                                            text = "A CTYV PRODUCT",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                if (subtitleText != null) {
                                    Text(
                                        text = "• $subtitleText",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { grid = !grid }) {
                            Icon(if (grid) Icons.Default.ViewList else Icons.Default.GridView, "Change view")
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Settings & Plugins")
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, "More options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort contacts (${sort.title})") },
                                    leadingIcon = { Icon(Icons.Default.Sort, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        sortMenu = true
                                    }
                                )
                                if (section == Section.GROUPS) {
                                    if (groupFilter != null && groupFilter != -1L) {
                                        val currentGroup = vm.groups.find { it.id == groupFilter }
                                        if (currentGroup != null) {
                                            DropdownMenuItem(
                                                text = { Text("Rename this group") },
                                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    groupDialog = GroupDialogState(currentGroup.id, currentGroup.name)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete this group") },
                                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    groupDialog = GroupDialogState(currentGroup.id, currentGroup.name, deleting = true)
                                                }
                                            )
                                        }
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Auto Group (AI)") },
                                        leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showKeywordCategorizerDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Manage all groups") },
                                        leadingIcon = { Icon(Icons.Default.Folder, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showGroupManager = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Merge groups") },
                                        leadingIcon = { Icon(Icons.Default.CallMerge, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            showMergeGroupsDialog = true
                                        }
                                    )
                                }

                                registeredFeatures.filter { it.id in activeFeatureIds && it.id in setOf("tags", "duplicates", "recycle-bin", "timeline", "smart-groups", "graph") }.forEach { feat ->
                                    DropdownMenuItem(
                                        text = { Text(feat.name) },
                                        leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            onNavigateToFeature(feat.id)
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Zero-Trust Vault & Export") },
                                    leadingIcon = { Icon(Icons.Default.ImportExport, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onNavigateToExport()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings & Plugins") },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onNavigateToSettings()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                val singleContact = if (selectedIds.size == 1) vm.contacts.firstOrNull { it.id == selectedIds.first() } else null
                val hasPhone = singleContact?.phone?.isNotBlank() == true || singleContact?.phones?.any { it.value.isNotBlank() } == true

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (singleContact != null && hasPhone) {
                                val number = singleContact.phone.ifBlank { singleContact.phones.firstOrNull()?.value.orEmpty() }
                                SelectionDockItem(
                                    icon = Icons.Default.Message,
                                    label = "Message",
                                    onClick = { vm.launchSms(number) }
                                )
                                SelectionDockItem(
                                    icon = Icons.Default.Call,
                                    label = "Call",
                                    onClick = { requestCall(singleContact) }
                                )
                            } else {
                                SelectionDockItem(
                                    icon = Icons.Default.Folder,
                                    label = "Group",
                                    onClick = { assignContactIds = selectedIds.toList() }
                                )
                                SelectionDockItem(
                                    icon = Icons.Default.Label,
                                    label = "Tag",
                                    onClick = { showTagPickerFor = selectedIds.toList() }
                                )
                            }

                            SelectionDockItem(
                                icon = Icons.Default.Share,
                                label = "Share",
                                onClick = { shareContactIds = selectedIds.toList() }
                            )

                            if (vm.canWriteContacts) {
                                SelectionDockItem(
                                    icon = Icons.Default.Delete,
                                    label = "Delete",
                                    isDestructive = true,
                                    onClick = { deleteIdsTarget = selectedIds.toList() }
                                )
                            }
                        }
                    }
                }
            } else {
                NavigationBar {
                    val sections = buildList {
                        add(Section.ALL to "Contacts")
                        if ("favorites" in activeFeatureIds) add(Section.FAVORITES to "Favorites")
                        if ("recent-contacts" in activeFeatureIds) add(Section.RECENT to "Recent")
                        if ("contact-groups" in activeFeatureIds) add(Section.GROUPS to "Groups")
                    }
                    sections.forEach { (item, label) ->
                        val icon = when (item) {
                            Section.ALL -> Icons.Default.Contacts
                            Section.FAVORITES -> Icons.Default.Favorite
                            Section.RECENT -> Icons.Default.MoreVert
                            Section.GROUPS -> Icons.Default.Groups
                        }
                        NavigationBarItem(
                            selected = section == item,
                            onClick = {
                                section = item
                                if (item != Section.GROUPS) groupFilter = null
                                vm.clearSelection()
                                vm.setActiveFilter(
                                    when (item) {
                                        Section.ALL -> FilterMode.ALL
                                        Section.FAVORITES -> FilterMode.FAVORITES
                                        Section.RECENT -> FilterMode.RECENT
                                        Section.GROUPS -> FilterMode.GROUPS
                                    }
                                )
                            },
                            icon = { Icon(icon, null) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (vm.hasContactsPermission && vm.canWriteContacts && selectedIds.isEmpty()) {
                FloatingActionButton(onClick = { editContactId = null; showContactForm = true }) {
                    Icon(Icons.Default.Create, "Create contact")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search name, phone, email, notes…") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )

            if (!vm.hasContactsPermission) {
                EmptyState(
                    title = "Allow contact access",
                    message = "Contact Explorer uses your phone's address book. Changes and deletions are reflected on your device.",
                    action = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
                    },
                    actionLabel = "Grant contact access",
                )
            } else {
                if (!vm.canWriteContacts) {
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Read-only access. Allow contact editing to create, change, or delete phone contacts.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
                            }) { Text("Allow") }
                        }
                    }
                }

                if (section == Section.GROUPS && groupFilter == null) {
                    val ungroupedContacts = vm.contacts.filter { c -> vm.memberships.none { it.contactId == c.id } }
                    val displayedGroups = if (search.isBlank()) vm.groups else vm.groups.filter { it.name.contains(search, ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showKeywordCategorizerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Auto Group (AI)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                OutlinedButton(
                                    onClick = { showMergeGroupsDialog = true },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CallMerge, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Merge", style = MaterialTheme.typography.labelMedium)
                                }
                                OutlinedButton(
                                    onClick = { groupDialog = GroupDialogState(null, "") },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("New", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        item {
                            UngroupedCard(
                                count = ungroupedContacts.size,
                                onClick = { groupFilter = -1L }
                            )
                        }

                        if (displayedGroups.isEmpty()) {
                            item {
                                EmptyState(
                                    title = if (search.isBlank()) "No custom groups yet" else "No matching groups",
                                    message = if (search.isBlank()) "Organize your contacts with Auto Group (AI) or create custom groups." else "Try searching with a different keyword.",
                                    action = { groupDialog = GroupDialogState(null, "") },
                                    actionLabel = "Create a group"
                                )
                            }
                        } else {
                            items(displayedGroups, key = { it.id }) { group ->
                                val members = vm.contacts.filter { c -> vm.memberships.any { it.contactId == c.id && it.groupId == group.id } }
                                GroupCard(
                                    group = group,
                                    count = members.size,
                                    previewNames = members.take(3).map { it.name },
                                    onClick = { groupFilter = group.id },
                                    onRename = { groupDialog = GroupDialogState(group.id, group.name) },
                                    onDelete = { groupDialog = GroupDialogState(group.id, group.name, deleting = true) }
                                )
                            }
                        }
                    }
                } else {
                    when {
                        vm.loading && vm.contacts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading contacts…") }
                        contacts.isEmpty() -> EmptyState(
                            title = when (section) {
                                Section.ALL -> if (search.isBlank()) "No contacts yet" else "No matching contacts"
                                Section.FAVORITES -> "No favorites yet"
                                Section.RECENT -> "No recently viewed contacts"
                                Section.GROUPS -> if (vm.groups.isEmpty()) "Create your first group" else "No contacts in this group"
                            },
                            message = when (section) {
                                Section.ALL -> "Add a contact with the + button, or add people to your phone first."
                                Section.FAVORITES -> "Mark contacts as favorites to keep them close at hand."
                                Section.RECENT -> "Contacts you open will show up here."
                                Section.GROUPS -> "Assign contacts to this group using the Move to group action."
                            },
                            action = if (section == Section.GROUPS && groupFilter == null) ({ showGroupManager = true }) else null,
                            actionLabel = if (section == Section.GROUPS && groupFilter == null) "Manage groups" else null,
                        )
                        grid -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(160.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(contacts, key = { it.id }) { contact ->
                                ContactTile(
                                    contact = contact,
                                    selected = contact.id in selectedIds,
                                    favorite = contact.id in vm.favorites,
                                    onOpen = {
                                        if (selectedIds.isNotEmpty()) {
                                            vm.toggleSelection(contact.id)
                                        } else {
                                            vm.markViewed(contact.id)
                                            onNavigateToDetails(contact.id)
                                        }
                                    },
                                    onLongPress = {
                                        vm.toggleSelection(contact.id)
                                    },
                                    onFavorite = { vm.toggleFavorite(contact.id) },
                                    onCall = { requestCall(contact) }
                                )
                            }
                        }
                        else -> LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 12.dp)
                        ) {
                            items(contacts, key = { it.id }) { contact ->
                                ContactRow(
                                    contact = contact,
                                    selected = contact.id in selectedIds,
                                    favorite = contact.id in vm.favorites,
                                    onOpen = {
                                        if (selectedIds.isNotEmpty()) {
                                            vm.toggleSelection(contact.id)
                                        } else {
                                            vm.markViewed(contact.id)
                                            onNavigateToDetails(contact.id)
                                        }
                                    },
                                    onLongPress = {
                                        vm.toggleSelection(contact.id)
                                    },
                                    onFavorite = { vm.toggleFavorite(contact.id) },
                                    onCall = { requestCall(contact) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showContactForm) {
        ContactFormDialog(
            contact = editContact,
            onDismiss = { showContactForm = false; editContactId = null },
            onSave = { draft ->
                vm.save(editContactId, draft)
                showContactForm = false
                editContactId = null
            }
        )
    }

    val groupCounts = remember(vm.groups, vm.memberships) {
        GroupStateLogic.computeGroupCounts(vm.groups, vm.memberships)
    }

    if (showGroupManager) {
        GroupManagerDialog(
            groups = vm.groups,
            groupCounts = groupCounts,
            onDismiss = { showGroupManager = false },
            onCreate = { groupDialog = GroupDialogState(null, "") },
            onRename = { group -> groupDialog = GroupDialogState(group.id, group.name) },
            onDelete = { group -> groupDialog = GroupDialogState(group.id, group.name, deleting = true) },
            onMergeGroups = { showGroupManager = false; showMergeGroupsDialog = true },
            onKeywordCategorizer = { showGroupManager = false; showKeywordCategorizerDialog = true },
        )
    }

    if (showMergeGroupsDialog) {
        MergeGroupsDialog(
            groups = vm.groups,
            groupCounts = groupCounts,
            onDismiss = { showMergeGroupsDialog = false },
            onMerge = { targetId, sourceIds ->
                vm.mergeGroups(targetId, sourceIds)
                showMergeGroupsDialog = false
            }
        )
    }

    if (showKeywordCategorizerDialog) {
        KeywordCategorizerDialog(
            contacts = vm.contacts,
            groups = vm.groups,
            onDismiss = { showKeywordCategorizerDialog = false },
            onAssign = { groupName, contactIds ->
                vm.createGroupAndAssign(groupName, contactIds)
                showKeywordCategorizerDialog = false
            }
        )
    }

    groupDialog?.let { dialog ->
        if (dialog.deleting) {
            ConfirmDialog(
                title = "Delete ${dialog.name}?",
                message = "Contacts will stay in your address book and become ungrouped.",
                onDismiss = { groupDialog = null },
                onConfirm = {
                    dialog.id?.let { vm.deleteGroup(it) }
                    groupDialog = null
                }
            )
        } else {
            TextEntryDialog(
                title = if (dialog.id == null) "Create group" else "Rename group",
                initial = dialog.name,
                onDismiss = { groupDialog = null },
                onSave = { name ->
                    if (dialog.id == null) vm.createGroup(name)
                    else vm.renameGroup(dialog.id, name)
                    groupDialog = null
                }
            )
        }
    }

    assignContactIds?.let { ids ->
        val groupIds = ids.map { id -> vm.memberships.firstOrNull { it.contactId == id }?.groupId }.distinct()
        GroupPickerDialog(
            title = "Move ${ids.size} contact${if (ids.size == 1) "" else "s"}",
            groups = vm.groups,
            selectedGroupId = groupIds.singleOrNull(),
            mixedSelection = groupIds.size > 1,
            onDismiss = { assignContactIds = null },
            onSelect = { groupId ->
                vm.moveToGroup(ids, groupId)
                vm.clearSelection()
                assignContactIds = null
            }
        )
    }

    showTagPickerFor?.let { ids ->
        TagPickerDialog(
            tags = vm.tags,
            onDismiss = { showTagPickerFor = null },
            onSelect = { tagId ->
                vm.addTagToContacts(ids, tagId)
                showTagPickerFor = null
            }
        )
    }

    shareContactIds?.let { ids ->
        ShareContactDialog(
            contacts = vm.contacts.filter { it.id in ids },
            onDismiss = { shareContactIds = null },
            onShare = { fields ->
                val contactsToShare = vm.contacts.filter { it.id in ids }
                val text = contactsToShare.joinToString("\n\n") { contact ->
                    buildList {
                        add(contact.name)
                        if ("phone" in fields) {
                            if (contact.phone.isNotBlank()) add("Phone: ${contact.phone}")
                            contact.phones.forEach { add("Phone (${it.label}): ${it.value}") }
                        }
                        if ("email" in fields) {
                            if (contact.email.isNotBlank()) add("Email: ${contact.email}")
                            contact.emails.forEach { add("Email (${it.label}): ${it.value}") }
                        }
                        if ("company" in fields && contact.company.isNotBlank()) add("Company: ${contact.company}")
                        if ("address" in fields && contact.address.isNotBlank()) add("Address: ${contact.address}")
                        if ("notes" in fields && contact.note.isNotBlank()) add("Notes: ${contact.note}")
                    }.joinToString("\n")
                }
                val title = if (contactsToShare.size == 1) contactsToShare.first().name else "${contactsToShare.size} Contacts"
                vm.shareText(title, text)
                shareContactIds = null
            }
        )
    }

    deleteIdsTarget?.let { ids ->
        ConfirmDialog(
            title = "Move ${ids.size} contacts to Recycle Bin?",
            message = "These contacts will be removed from the phone address book. You can restore them from Contact Explorer for 30 days.",
            onDismiss = { deleteIdsTarget = null },
            onConfirm = {
                vm.deleteContacts(ids)
                vm.clearSelection()
                deleteIdsTarget = null
            }
        )
    }

    if (sortMenu) {
        AlertDialog(
            onDismissRequest = { sortMenu = false },
            title = { Text("Sort contacts by") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SortMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    sort = mode
                                    sortMenu = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sort == mode,
                                onClick = {
                                    sort = mode
                                    sortMenu = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(mode.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { sortMenu = false }) { Text("Done") }
            }
        )
    }

    if (showTitleDetailDialog) {
        val currentTitle = when (section) {
            Section.ALL -> "Contact Explorer Enterprise"
            Section.FAVORITES -> "Favorites"
            Section.RECENT -> "Recent"
            Section.GROUPS -> groupFilter?.let { id -> if (id == -1L) "Ungrouped Contacts" else vm.groups.find { it.id == id }?.name } ?: "Groups"
        }
        val subtitleText = when {
            section == Section.ALL -> "${contacts.size} contacts managed on-device"
            section == Section.GROUPS && groupFilter == null -> "${vm.groups.size} groups available"
            section == Section.GROUPS && groupFilter != null -> "${contacts.size} contacts in this group"
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { showTitleDetailDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Prominent CTYV Emblem
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "CTYV Emblem",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Text(
                        text = currentTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Text(
                                text = "A CTYV PRODUCT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (subtitleText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AboutInfoRow(label = "Product", value = "Contact Explorer Enterprise")
                            AboutInfoRow(label = "Signature", value = "A CTYV Product")
                            AboutInfoRow(label = "Creator", value = "Priyanshu Sharma (CTYV)")
                            AboutInfoRow(label = "Architecture", value = "Offline-First Zero-Trust Architecture")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = "• 100% On-Device Isolation (Zero Internet Permissions)\n" +
                               "• Zero Analytics, Advertising, or Data Harvesting\n" +
                               "• Sandboxed Local SQLite Room Vault\n" +
                               "• Open Source under the MIT License",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTitleDetailDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
