package com.example.contactexplorer

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.DialogProperties
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactDraft
import com.example.contactexplorer.data.ContactTransfer
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.data.OfflineIntelligence
import com.example.contactexplorer.data.KeywordCluster
import com.example.contactexplorer.data.SearchEngine
import com.example.contactexplorer.data.SearchRequest
import com.example.contactexplorer.plugins.AppearancePlugin
import com.example.contactexplorer.plugins.BulkGroupPlugin
import com.example.contactexplorer.plugins.BuiltInFeatureScreens
import com.example.contactexplorer.plugins.DirectCallingPlugin
import com.example.contactexplorer.plugins.FeatureManagerPlugin
import com.example.contactexplorer.plugins.FeatureDescriptor
import com.example.contactexplorer.plugins.FeatureCategory
import com.example.contactexplorer.plugins.CoreServicesPlugin
import com.example.contactexplorer.plugins.NavigationFeaturesPlugin
import com.example.contactexplorer.plugins.ExplorerPluginRegistry
import com.example.contactexplorer.plugins.KotlinSearchEngine
import com.example.contactexplorer.plugins.SearchFeaturePlugin
import com.example.contactexplorer.plugins.AiFeaturePlugin
import com.example.contactexplorer.permissions.ContactPermissions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ContactExplorerApp() }
    }
}

private enum class Section { ALL, FAVORITES, RECENT, GROUPS }
private enum class SortMode(val title: String) {
    RELEVANCE("Best match"), NAME_ASC("Name A to Z"), NAME_DESC("Name Z to A"), MODIFIED("Recently modified"),
    MOST_CONTACTED("Most contacted"), LEAST_CONTACTED("Least contacted")
}

// Contact Book Icon Inspired Color Palettes (Obsidian Dark with Neon Cyan & Warm Amber Lighting)
private val IconBookDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00D2FF), // Vibrant neon cyan rim glow
    onPrimary = Color(0xFF030A12),
    primaryContainer = Color(0xFF0C2B4D), // Deep translucent electric blue
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFFFFB020), // Warm amber / gold tab divider glow
    onSecondary = Color(0xFF2E1700),
    secondaryContainer = Color(0xFF4A2A04),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF38E1FF), // Electric ice highlight
    onTertiary = Color(0xFF001F29),
    tertiaryContainer = Color(0xFF083344),
    onTertiaryContainer = Color(0xFFB5EFFF),
    background = Color(0xFF070B12), // Deep obsidian black
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0E1626), // Midnight navy book cover surface
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF152238),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF111C2E),
    surfaceContainerHigh = Color(0xFF17253D),
    surfaceContainerHighest = Color(0xFF1E2F4D),
    outline = Color(0xFF1E385C), // Cyan-tinted rim border
    outlineVariant = Color(0xFF122035),
)

private val IconBookLightColorScheme = lightColorScheme(
    primary = Color(0xFF007A99),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0F0FD),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFFB45309), // Amber tab accent
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
)

@Composable
private fun ContactExplorerApp(vm: ExplorerViewModel = viewModel()) {
    val colors = when (vm.themeMode) {
        ThemeMode.DARK -> IconBookDarkColorScheme
        ThemeMode.SYSTEM -> if (androidx.compose.foundation.isSystemInDarkTheme()) IconBookDarkColorScheme else IconBookLightColorScheme
        ThemeMode.LIGHT -> IconBookLightColorScheme
    }
    MaterialTheme(colorScheme = colors) { ContactExplorerContent(vm) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactExplorerContent(vm: ExplorerViewModel) {
    val context = LocalContext.current
    val contactPermissions = remember(context) { ContactPermissions(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val revision = vm.revision
    val featurePreferences = remember(context) { context.getSharedPreferences("explorer_features", android.content.Context.MODE_PRIVATE) }
    var savedFeatureIds by remember(context) { mutableStateOf(featurePreferences.getStringSet("enabled_ids", null)?.toSet()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.refresh() }
    var section by remember { mutableStateOf(Section.ALL) }
    var groupFilter by remember { mutableStateOf<Long?>(null) }
    var search by remember { mutableStateOf("") }
    var grid by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(SortMode.RELEVANCE) }
    var sortMenu by remember { mutableStateOf(false) }
    var showGroups by remember { mutableStateOf(false) }
    var editContact by remember { mutableStateOf<Contact?>(null) }
    var showContactForm by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Contact?>(null) }
    var showGroupManager by remember { mutableStateOf(false) }
    var showMergeGroupsDialog by remember { mutableStateOf(false) }
    var showKeywordCategorizerDialog by remember { mutableStateOf(false) }
    var groupDialog by remember { mutableStateOf<GroupDialog?>(null) }
    var assignContactIds by remember { mutableStateOf<List<String>?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Contact?>(null) }
    var deleteIdsTarget by remember { mutableStateOf<List<String>?>(null) }
    var showFeaturesMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTitleDetailDialog by remember { mutableStateOf(false) }
    var activeFeatureId by remember { mutableStateOf<String?>(null) }
    var showTagPickerFor by remember { mutableStateOf<List<String>?>(null) }
    var shareContactIds by remember { mutableStateOf<List<String>?>(null) }
    var importFormat by remember { mutableStateOf("vcf") }
    val selectedIds = remember { mutableStateListOf<String>() }

    val importDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not read selected file.")
        }.onSuccess { text ->
            val drafts = if (importFormat == "csv") ContactTransfer.fromCsv(text) else ContactTransfer.fromVcf(text)
            drafts.forEach(vm::importContact)
            Toast.makeText(context, "Importing ${drafts.size} contacts", Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(context, it.message ?: "Could not import file.", Toast.LENGTH_LONG).show() }
    }
    var pendingExportText by remember { mutableStateOf("") }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingExportText) } ?: error("Could not write export file.") }
            .onFailure { Toast.makeText(context, it.message ?: "Could not export contacts.", Toast.LENGTH_LONG).show() }
    }
    val exportVcfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/vcard")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingExportText) } ?: error("Could not write export file.") }
            .onFailure { Toast.makeText(context, it.message ?: "Could not export contacts.", Toast.LENGTH_LONG).show() }
    }

    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        vm.refresh()
        pendingCallNumber?.let { number ->
            if (isGranted) launchDirectCall(context, number) else launchDialAction(context, number)
            pendingCallNumber = null
        }
    }

    val requestCall: (Contact) -> Unit = { contact ->
        val number = contact.phone.ifBlank { contact.phones.firstOrNull()?.value.orEmpty() }
        if (number.isNotBlank()) {
            vm.recordCall(contact)
            val hasCallPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCallPermission) {
                launchDirectCall(context, number)
            } else {
                pendingCallNumber = number
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }
    val builtInPlugins = listOf(
        CoreServicesPlugin(),
        NavigationFeaturesPlugin(),
        DirectCallingPlugin(requestCall),
        BulkGroupPlugin { ids -> assignContactIds = ids },
        AppearancePlugin(vm.themeMode) { showThemeDialog = true },
        SearchFeaturePlugin(KotlinSearchEngine()),
        AiFeaturePlugin(),
        BuiltInFeatureScreens(
            vm = vm,
            onAddTagToContacts = { ids -> showTagPickerFor = ids },
            onAssignTag = vm::addTagToContacts,
            onImportCsv = { importFormat = "csv"; importDocumentLauncher.launch(arrayOf("text/*", "application/*")) },
            onImportVcf = { importFormat = "vcf"; importDocumentLauncher.launch(arrayOf("text/*", "application/*")) },
            onExportCsv = { ids ->
                pendingExportText = ContactTransfer.toCsv(ids?.let { selected -> vm.contacts.filter { it.id in selected } } ?: vm.contacts)
                exportCsvLauncher.launch("contacts.csv")
            },
            onExportVcf = { ids ->
                pendingExportText = ContactTransfer.toVcf(ids?.let { selected -> vm.contacts.filter { it.id in selected } } ?: vm.contacts)
                exportVcfLauncher.launch("contacts.vcf")
            },
            onDeleteContacts = { ids -> deleteIdsTarget = ids },
            onShareContacts = { ids -> shareContactIds = ids },
        ),
    )
    val defaultEnabledIds = builtInPlugins.flatMap { it.features }
        .filter(FeatureDescriptor::enabledByDefault).mapTo(mutableSetOf(), FeatureDescriptor::id)
    val registeredFeatureIds = builtInPlugins.flatMap { it.features }.mapTo(mutableSetOf(), FeatureDescriptor::id)
    val previouslyKnownIds = featurePreferences.getStringSet("known_ids", null)?.toSet().orEmpty()
    val activeFeatureIds = (savedFeatureIds ?: defaultEnabledIds).toMutableSet().apply {
        if (savedFeatureIds != null) {
            builtInPlugins.flatMap { it.features }
                .filter { it.enabledByDefault && it.id !in previouslyKnownIds }
                .forEach { add(it.id) }
        }
    }
    val managerPlugin = FeatureManagerPlugin(
        registeredFeatures = builtInPlugins.flatMap { it.features },
        isEnabled = { it in activeFeatureIds },
        onToggle = { id, enabled ->
            if (enabled) activeFeatureIds.add(id) else activeFeatureIds.remove(id)
            if (!enabled && ((id == "favorites" && section == Section.FAVORITES) ||
                    (id == "recent-contacts" && section == Section.RECENT) ||
                    (id == "contact-groups" && section == Section.GROUPS))) {
                section = Section.ALL
                groupFilter = null
            }
            savedFeatureIds = activeFeatureIds.toSet()
            featurePreferences.edit()
                .putStringSet("enabled_ids", activeFeatureIds)
                .putStringSet("known_ids", registeredFeatureIds)
                .apply()
        },
    )
    val featureRegistry = ExplorerPluginRegistry.install(builtInPlugins + managerPlugin).enabled(activeFeatureIds)

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(vm.message, revision) {
        vm.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.clearMessage() }
    }
    BackHandler(selectedIds.isNotEmpty()) { selectedIds.clear() }
    BackHandler(activeFeatureId != null) { activeFeatureId = null }
    BackHandler(selectedIds.isEmpty() && activeFeatureId == null && section == Section.GROUPS && groupFilter != null) { groupFilter = null }

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
    val groupNames = vm.memberships.groupBy { it.contactId }.mapValues { (_, links) -> links.mapNotNull { link -> vm.groups.find { it.id == link.groupId }?.name } }
    val tagNames = vm.tagAssignments.groupBy { it.contactId }.mapValues { (_, links) -> links.mapNotNull { link -> vm.tags.find { it.id == link.tagId }?.name } }
    val searchEngine = featureRegistry.service("search.engine", SearchEngine::class)
    val searchedContacts = searchEngine?.search(SearchRequest(
        query = search,
        contacts = sectionContacts,
        groupNames = groupNames,
        tagNames = tagNames,
        favorites = vm.favorites,
        recency = vm.recentIds.mapIndexed { index, id -> id to (vm.recentIds.size - index) }.toMap(),
    )) ?: if (search.isBlank()) sectionContacts else emptyList()
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
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4C8DF6),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${selectedIds.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 2.dp
                            ) {
                                TextButton(
                                    onClick = {
                                        if (selectedIds.size == contacts.size) selectedIds.clear()
                                        else {
                                            selectedIds.clear()
                                            selectedIds.addAll(contacts.map { it.id })
                                        }
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (selectedIds.size == contacts.size) "Clear all" else "Select all",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 2.dp
                            ) {
                                TextButton(
                                    onClick = { selectedIds.clear() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    navigationIcon = {
                        if (activeFeatureId != null) IconButton(onClick = { activeFeatureId = null }) { Icon(Icons.Default.ArrowBack, "Back to contacts") }
                        else if (section == Section.GROUPS && groupFilter != null) IconButton(onClick = { groupFilter = null }) { Icon(Icons.Default.ArrowBack, "All groups") }
                    },
                    title = {
                        val activeFeature = featureRegistry.screens.firstOrNull { it.id == activeFeatureId }
                        val currentTitle = activeFeature?.label ?: when (section) {
                            Section.ALL -> "Contact Explorer"
                            Section.FAVORITES -> "Favorites"
                            Section.RECENT -> "Recent"
                            Section.GROUPS -> groupFilter?.let { id -> if (id == -1L) "Ungrouped Contacts" else vm.groups.find { it.id == id }?.name } ?: "Groups"
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
                                .clickable {
                                    showTitleDetailDialog = true
                                }
                        ) {
                            Text(
                                text = currentTitle,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (subtitleText != null) {
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { grid = !grid }) {
                            Icon(if (grid) Icons.Default.ViewList else Icons.Default.GridView, "Change view")
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
                                                    groupDialog = GroupDialog(currentGroup.id, currentGroup.name)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete this group") },
                                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    groupDialog = GroupDialog(currentGroup.id, currentGroup.name, deleting = true)
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
                                featureRegistry.toolbarActions.forEach { action ->
                                    DropdownMenuItem(
                                        text = { Text(action.label) },
                                        leadingIcon = { Icon(action.icon, null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            action.invoke()
                                        }
                                    )
                                }
                                if (featureRegistry.screens.isNotEmpty()) {
                                    featureRegistry.screens.forEach { screen ->
                                        DropdownMenuItem(
                                            text = { Text(screen.label) },
                                            leadingIcon = { Icon(screen.icon, null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                activeFeatureId = screen.id
                                                selectedIds.clear()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                var showSelectionMoreMenu by remember { mutableStateOf(false) }
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
                                    onClick = { launchSmsAction(context, number) }
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

                            Box {
                                SelectionDockItem(
                                    icon = Icons.Default.MoreVert,
                                    label = "More",
                                    onClick = { showSelectionMoreMenu = true }
                                )
                                DropdownMenu(
                                    expanded = showSelectionMoreMenu,
                                    onDismissRequest = { showSelectionMoreMenu = false }
                                ) {
                                    if (singleContact != null && hasPhone) {
                                        DropdownMenuItem(
                                            text = { Text("Move to group") },
                                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                                            onClick = {
                                                showSelectionMoreMenu = false
                                                assignContactIds = selectedIds.toList()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add tag") },
                                            leadingIcon = { Icon(Icons.Default.Label, null) },
                                            onClick = {
                                                showSelectionMoreMenu = false
                                                showTagPickerFor = selectedIds.toList()
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Export VCF") },
                                        leadingIcon = { Icon(Icons.Default.ImportExport, null) },
                                        onClick = {
                                            showSelectionMoreMenu = false
                                            pendingExportText = ContactTransfer.toVcf(vm.contacts.filter { it.id in selectedIds })
                                            exportVcfLauncher.launch("contacts.vcf")
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export CSV") },
                                        leadingIcon = { Icon(Icons.Default.ImportExport, null) },
                                        onClick = {
                                            showSelectionMoreMenu = false
                                            pendingExportText = ContactTransfer.toCsv(vm.contacts.filter { it.id in selectedIds })
                                            exportCsvLauncher.launch("contacts.csv")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                NavigationBar {
                    buildList {
                        add(Section.ALL to "Contacts")
                        if ("favorites" in activeFeatureIds) add(Section.FAVORITES to "Favorites")
                        if ("recent-contacts" in activeFeatureIds) add(Section.RECENT to "Recent")
                        if ("contact-groups" in activeFeatureIds) add(Section.GROUPS to "Groups")
                    }.forEach { (item, label) ->
                        val icon = when (item) {
                            Section.ALL -> Icons.Default.Contacts
                            Section.FAVORITES -> Icons.Default.Favorite
                            Section.RECENT -> Icons.Default.MoreVert
                            Section.GROUPS -> Icons.Default.Groups
                        }
                        NavigationBarItem(selected = section == item, onClick = {
                            section = item
                            if (item != Section.GROUPS) groupFilter = null
                            selectedIds.clear()
                        }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeFeatureId == null && vm.hasContactsPermission && vm.canWriteContacts && selectedIds.isEmpty()) FloatingActionButton(onClick = { editContact = null; showContactForm = true }) {
                Icon(Icons.Default.Create, "Create contact")
            }
        },
    ) { padding ->
        val activeScreen = featureRegistry.screens.firstOrNull { it.id == activeFeatureId }
        if (activeScreen != null) Box(Modifier.fillMaxSize().padding(padding)) { activeScreen.content() }
        else Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (searchEngine != null) OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search name, phone, email, company, address, notes…") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
            if (!vm.hasContactsPermission) {
                EmptyState(
                    title = "Allow contact access",
                    message = "Contact Explorer uses your phone's address book. Changes and deletions are reflected on your device.",
                    action = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) },
                    actionLabel = "Grant contact access",
                )
            } else {
                if (!vm.canWriteContacts) {
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Read-only access. Allow contact editing to create, change, or delete phone contacts.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) }) { Text("Allow") }
                        }
                    }
                }
                if (section == Section.GROUPS && groupFilter == null) {
                    val ungroupedContacts = vm.contacts.filter { c -> vm.memberships.none { it.contactId == c.id } }
                    val displayedGroups = if (search.isBlank()) vm.groups else vm.groups.filter { it.name.contains(search, ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(bottom = 12.dp),
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
                                    onClick = { groupDialog = GroupDialog(null, "") },
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
                                    action = { groupDialog = GroupDialog(null, "") },
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
                                    onRename = { groupDialog = GroupDialog(group.id, group.name) },
                                    onDelete = { groupDialog = GroupDialog(group.id, group.name, deleting = true) }
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
                        grid -> LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                            items(contacts, key = { it.id }) { contact ->
                                ContactTile(contact, contact.id in selectedIds, contact.id in vm.favorites,
                                    onOpen = { if (selectedIds.isNotEmpty()) toggleSelected(selectedIds, contact.id) else { detail = contact; vm.markViewed(contact.id) } },
                                    onLongPress = { toggleSelected(selectedIds, contact.id) }, onFavorite = { vm.toggleFavorite(contact.id) },
                                    onCall = { featureRegistry.contactActions.firstOrNull { it.id == "direct-calling.call" }?.invoke?.invoke(contact) })
                            }
                        }
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)) {
                            items(contacts, key = { it.id }) { contact ->
                                ContactRow(contact, contact.id in selectedIds, contact.id in vm.favorites,
                                    onOpen = { if (selectedIds.isNotEmpty()) toggleSelected(selectedIds, contact.id) else { detail = contact; vm.markViewed(contact.id) } },
                                    onLongPress = { toggleSelected(selectedIds, contact.id) }, onFavorite = { vm.toggleFavorite(contact.id) },
                                    onCall = { featureRegistry.contactActions.firstOrNull { it.id == "direct-calling.call" }?.invoke?.invoke(contact) })
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { contact ->
        ContactDetailsDialog(contact, contact.id in vm.favorites,
            groups = vm.groups.filter { group -> vm.memberships.any { it.contactId == contact.id && it.groupId == group.id } },
            canWrite = vm.canWriteContacts,
            onCall = { requestCall(contact) },
            onShare = { shareContactIds = listOf(contact.id) },
            onDismiss = { detail = null }, onFavorite = { vm.toggleFavorite(contact.id) },
            onEdit = { detail = null; editContact = contact; showContactForm = true },
            onDelete = { detail = null; deleteTarget = contact },
            onMoveGroup = { detail = null; assignContactIds = listOf(contact.id) },
        )
    }
    if (showContactForm) ContactFormDialog(editContact, onDismiss = { showContactForm = false; editContact = null }) { draft ->
        vm.save(editContact?.id, draft); showContactForm = false; editContact = null
    }
    if (showGroupManager) GroupManagerDialog(
        groups = vm.groups,
        groupCounts = vm.groups.associate { group -> group.id to vm.memberships.count { it.groupId == group.id } },
        onDismiss = { showGroupManager = false },
        onCreate = { groupDialog = GroupDialog(null, "") },
        onRename = { group -> groupDialog = GroupDialog(group.id, group.name) },
        onDelete = { group -> groupDialog = GroupDialog(group.id, group.name, deleting = true) },
        onMergeGroups = { showGroupManager = false; showMergeGroupsDialog = true },
        onKeywordCategorizer = { showGroupManager = false; showKeywordCategorizerDialog = true },
    )
    if (showMergeGroupsDialog) {
        MergeGroupsDialog(
            groups = vm.groups,
            groupCounts = vm.groups.associate { group -> group.id to vm.memberships.count { it.groupId == group.id } },
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
        if (dialog.deleting) ConfirmDialog("Delete ${dialog.name}?", "Contacts will stay in your address book and become ungrouped.",
            onDismiss = { groupDialog = null }, onConfirm = { vm.deleteGroup(dialog.id!!); groupDialog = null })
        else TextEntryDialog(if (dialog.id == null) "Create group" else "Rename group", dialog.name,
            onDismiss = { groupDialog = null }, onSave = { name -> if (dialog.id == null) vm.createGroup(name) else vm.renameGroup(dialog.id, name); groupDialog = null })
    }
    assignContactIds?.let { ids ->
        val groupIds = ids.map { id -> vm.memberships.firstOrNull { it.contactId == id }?.groupId }.distinct()
        GroupPickerDialog("Move ${ids.size} contact${if (ids.size == 1) "" else "s"}", vm.groups,
            selectedGroupId = groupIds.singleOrNull(), mixedSelection = groupIds.size > 1,
            onDismiss = { assignContactIds = null },
            onSelect = { groupId -> vm.moveToGroup(ids, groupId); selectedIds.clear(); assignContactIds = null },
        )
    }
    if (showThemeDialog) ThemeModeDialog(vm.themeMode, onDismiss = { showThemeDialog = false }, onSelect = { mode -> vm.updateThemeMode(mode); showThemeDialog = false })
    showTagPickerFor?.let { ids ->
        TagPickerDialog(vm.tags, onDismiss = { showTagPickerFor = null }, onSelect = { tagId -> vm.addTagToContacts(ids, tagId); showTagPickerFor = null })
    }
    shareContactIds?.let { ids ->
        ShareContactDialog(
            contacts = vm.contacts.filter { it.id in ids },
            onDismiss = { shareContactIds = null },
            onShare = { fields ->
                val contactsToShare = vm.contacts.filter { it.id in ids }
                shareContacts(context, vm, contactsToShare, fields)
                shareContactIds = null
            },
        )
    }
    deleteIdsTarget?.let { ids -> ConfirmDialog(
        "Move ${ids.size} contacts to Recycle Bin?",
        "These contacts will be removed from the phone address book. You can restore them from Contact Explorer for 30 days.",
        onDismiss = { deleteIdsTarget = null },
        onConfirm = { vm.deleteContacts(ids); selectedIds.clear(); deleteIdsTarget = null },
    ) }
    deleteTarget?.let { contact -> ConfirmDialog("Move ${contact.name} to Recycle Bin?", "This removes the contact from the phone's address book. You can restore it from Contact Explorer for 30 days.",
        onDismiss = { deleteTarget = null }, onConfirm = { vm.delete(contact.id); deleteTarget = null }) }

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
            Section.ALL -> "Contact Explorer"
            Section.FAVORITES -> "Favorites"
            Section.RECENT -> "Recent"
            Section.GROUPS -> groupFilter?.let { id -> if (id == -1L) "Ungrouped Contacts" else vm.groups.find { it.id == id }?.name } ?: "Groups"
        }
        val subtitleText = when {
            section == Section.ALL -> "${contacts.size} contacts total"
            section == Section.GROUPS && groupFilter == null -> "${vm.groups.size} groups available"
            section == Section.GROUPS && groupFilter != null -> "${contacts.size} contacts in this group"
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { showTitleDetailDialog = false },
            title = { Text(currentTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(subtitleText, style = MaterialTheme.typography.bodyLarge)
                    if (section == Section.GROUPS && groupFilter != null && groupFilter != -1L) {
                        val grp = vm.groups.find { it.id == groupFilter }
                        if (grp != null) {
                            Text("Use the ⋮ menu at top right to rename or delete this group.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTitleDetailDialog = false }) { Text("OK") }
            }
        )
    }
}

private data class GroupDialog(val id: Long?, val name: String, val deleting: Boolean = false)

private fun shareContacts(context: android.content.Context, vm: ExplorerViewModel, contacts: List<Contact>, fields: Set<String>) {
    if (contacts.isEmpty()) return
    val vcard = ContactTransfer.toVcf(contacts.map { contact -> contact.copy(
        phone = contact.phone.takeIf { "phone" in fields }.orEmpty(),
        email = contact.email.takeIf { "email" in fields }.orEmpty(),
        company = contact.company.takeIf { "company" in fields }.orEmpty(),
        address = contact.address.takeIf { "address" in fields }.orEmpty(),
        note = contact.note.takeIf { "notes" in fields }.orEmpty(),
        phones = if ("phone" in fields) contact.phones else emptyList(),
        emails = if ("email" in fields) contact.emails else emptyList(),
    ) })
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/x-vcard"
        putExtra(Intent.EXTRA_TEXT, vcard)
        putExtra(Intent.EXTRA_TITLE, "${contacts.size} contacts")
    }
    vm.recordShare(contacts.map { it.id })
    runCatching { context.startActivity(Intent.createChooser(intent, "Share contacts")) }
        .onFailure { Toast.makeText(context, "No app can share contacts.", Toast.LENGTH_SHORT).show() }
}

private fun toggleSelected(selected: MutableList<String>, id: String) {
    if (id in selected) selected.remove(id) else selected.add(id)
}

private fun launchDirectCall(context: android.content.Context, number: String) {
    val clean = number.trim()
    if (clean.isBlank()) return
    val uri = Uri.fromParts("tel", clean, null)
    runCatching {
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }.onFailure {
        launchDialAction(context, clean)
    }
}

private fun launchDialAction(context: android.content.Context, number: String) {
    val clean = number.trim()
    if (clean.isBlank()) return
    val uri = Uri.fromParts("tel", clean, null)
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Could not start phone app.", Toast.LENGTH_SHORT).show()
    }
}

private fun launchPhoneAction(context: android.content.Context, number: String) {
    launchDirectCall(context, number)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: com.example.contactexplorer.data.ContactGroup,
    count: Int,
    previewNames: List<String>,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                val subtitle = if (previewNames.isNotEmpty()) {
                    "$count contacts • ${previewNames.joinToString(", ")}"
                } else {
                    "$count contacts"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete group",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UngroupedCard(
    count: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ungrouped Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$count contacts not assigned to any group",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .rotate(180f)
                    .size(20.dp)
            )
        }
    }
}

private fun launchSmsAction(context: android.content.Context, number: String) {
    val uri = Uri.parse("smsto:$number")
    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
        .onFailure { Toast.makeText(context, "Could not start messaging app.", Toast.LENGTH_SHORT).show() }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text(label) }
    else OutlinedButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text(label) }
}

@Composable
private fun EmptyState(title: String, message: String, action: (() -> Unit)? = null, actionLabel: String? = null) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Contacts, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && actionLabel != null) { Spacer(Modifier.height(16.dp)); Button(onClick = action) { Text(actionLabel) } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRow(contact: Contact, selected: Boolean, favorite: Boolean, onOpen: () -> Unit, onLongPress: () -> Unit, onFavorite: () -> Unit, onCall: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(0xFF4C8DF6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Avatar(contact.name)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(contact.company, contact.phone).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { contact.email.ifBlank { "No phone or email" } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!selected) {
                if (contact.phone.isNotBlank()) IconButton(onClick = onCall) { Icon(Icons.Default.Call, "Call ${contact.name}") }
                IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (favorite) "Remove favorite" else "Add favorite", tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactTile(contact: Contact, selected: Boolean, favorite: Boolean, onOpen: () -> Unit, onLongPress: () -> Unit, onFavorite: () -> Unit, onCall: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onLongPress),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!selected) {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Toggle favorite", tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }
            }
            if (selected) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color(0xFF4C8DF6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                Avatar(contact.name, 52.dp)
            }
            Spacer(Modifier.height(12.dp))
            Text(contact.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(contact.company.ifBlank { contact.phone.ifBlank { contact.email.ifBlank { "Contact" } } }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!selected && contact.phone.isNotBlank()) TextButton(onClick = onCall) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(6.dp)); Text("Call") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectionDockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Avatar(name: String, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Surface(Modifier.size(size), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("").ifBlank { "?" }, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsDialog(
    contact: Contact, favorite: Boolean, groups: List<com.example.contactexplorer.data.ContactGroup>,
    canWrite: Boolean,
    onCall: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit, onFavorite: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onMoveGroup: () -> Unit,
) {
    val context = LocalContext.current
    val phones = if (contact.phones.isNotEmpty()) contact.phones else if (contact.phone.isNotBlank()) listOf(LabeledValue(contact.phone)) else emptyList()
    val emails = if (contact.emails.isNotEmpty()) contact.emails else if (contact.email.isNotBlank()) listOf(LabeledValue(contact.email, "Home", 1)) else emptyList()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Avatar(contact.name, 48.dp); Spacer(Modifier.width(12.dp)); Text(contact.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge) }

            if (phones.isNotEmpty()) {
                Text("Phone Numbers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                phones.forEach { p ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.value, fontWeight = FontWeight.Medium)
                            Text(p.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { launchPhoneAction(context, p.value) }) { Icon(Icons.Default.Call, "Call ${p.value}") }
                        IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(p.value)}"))) }) { Icon(Icons.Default.Message, "Message ${p.value}") }
                    }
                }
            }

            if (emails.isNotEmpty()) {
                Text("Email Addresses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                emails.forEach { e ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.value, fontWeight = FontWeight.Medium)
                            Text(e.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(e.value)}"))) }) { Icon(Icons.Default.Email, "Email ${e.value}") }
                    }
                }
            }

            DetailLine("Company", contact.company)
            DetailLine("Address", contact.address)
            DetailLine("Notes", contact.note)
            DetailLine("Groups", groups.joinToString { it.name }.ifBlank { "Ungrouped" })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share selected contact fields") }
            }
            OutlinedButton(onClick = onMoveGroup, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Folder, null); Spacer(Modifier.width(8.dp)); Text("Move to group") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onFavorite) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Text(if (favorite) "Favorited" else "Favorite") }
                if (canWrite) {
                    TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null); Text("Edit") }
                    TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null); Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun ShareContactDialog(contacts: List<Contact>, onDismiss: () -> Unit, onShare: (Set<String>) -> Unit) {
    val fields = listOf("phone" to "Phone", "email" to "Email", "company" to "Company", "address" to "Address", "notes" to "Notes")
    var selected by remember { mutableStateOf(fields.map { it.first }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contacts.size == 1) "Share ${contacts.firstOrNull()?.name.orEmpty()}" else "Share ${contacts.size} contacts") },
        text = {
            Column {
                Text("Choose which fields to include. Names are always shared.", style = MaterialTheme.typography.bodySmall)
                fields.forEach { (key, label) ->
                    val available = contacts.any { contact -> when (key) {
                        "phone" -> contact.phone.isNotBlank(); "email" -> contact.email.isNotBlank()
                        "company" -> contact.company.isNotBlank(); "address" -> contact.address.isNotBlank()
                        else -> contact.note.isNotBlank()
                    } }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = key in selected, enabled = available, onCheckedChange = { checked -> selected = if (checked) selected + key else selected - key })
                        Text(label, color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onShare(selected) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isNotBlank()) Row {
        Text("$label: ", fontWeight = FontWeight.SemiBold)
        Text(value)
    }
}

@Composable
private fun ContactFormDialog(contact: Contact?, onDismiss: () -> Unit, onSave: (ContactDraft) -> Unit) {
    var name by remember(contact) { mutableStateOf(contact?.name.orEmpty()) }
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
    var company by remember(contact) { mutableStateOf(contact?.company.orEmpty()) }
    var address by remember(contact) { mutableStateOf(contact?.address.orEmpty()) }
    var note by remember(contact) { mutableStateOf(contact?.note.orEmpty()) }
    var error by remember(contact) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.92f),
        title = { Text(if (contact == null) "New contact" else "Edit contact") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(480.dp)) {
                item { OutlinedTextField(name, { name = it; error = false }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = error) }

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
                                Icon(Icons.Default.Close, "Remove phone")
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { initialPhones.add(LabeledValue("")) }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add phone number")
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
                                Icon(Icons.Default.Close, "Remove email")
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { initialEmails.add(LabeledValue("", "Home", 1)) }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add email address")
                    }
                }

                item { OutlinedTextField(company, { company = it }, label = { Text("Company") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(note, { note = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()) }
                if (error) item { Text("Name is required.", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    error = true
                } else {
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

@Composable
private fun GroupManagerDialog(
    groups: List<com.example.contactexplorer.data.ContactGroup>,
    groupCounts: Map<Long, Int>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onRename: (com.example.contactexplorer.data.ContactGroup) -> Unit,
    onDelete: (com.example.contactexplorer.data.ContactGroup) -> Unit,
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
                        Icon(Icons.Default.CallMerge, null, modifier = Modifier.size(16.dp))
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
                            Text("${groupCounts[group.id] ?: 0} contacts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun MergeGroupsDialog(
    groups: List<com.example.contactexplorer.data.ContactGroup>,
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

    var targetGroupId by remember { mutableStateOf(groups.first().id) }
    val selectedSourceGroupIds = remember { mutableStateListOf<Long>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

                Text("Target Group (Keep this group):", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                groups.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = targetGroupId == group.id,
                            onClick = {
                                targetGroupId = group.id
                                selectedSourceGroupIds.remove(group.id)
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.name, fontWeight = if (targetGroupId == group.id) FontWeight.Bold else FontWeight.Normal)
                            Text("${groupCounts[group.id] ?: 0} contacts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("Source Groups to Merge & Delete:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                val eligibleSources = groups.filter { it.id != targetGroupId }
                eligibleSources.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = group.id in selectedSourceGroupIds,
                            onCheckedChange = { checked ->
                                if (checked) selectedSourceGroupIds.add(group.id)
                                else selectedSourceGroupIds.remove(group.id)
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.name)
                            Text("${groupCounts[group.id] ?: 0} contacts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                onClick = { onMerge(targetGroupId, selectedSourceGroupIds.toList()) }
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
private fun KeywordCategorizerDialog(
    contacts: List<Contact>,
    groups: List<com.example.contactexplorer.data.ContactGroup>,
    onDismiss: () -> Unit,
    onAssign: (String, List<String>) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Auto-detect clusters
    val clusters = remember(contacts) { OfflineIntelligence.detectKeywordClusters(contacts) }

    // Custom keyword state
    var customKeyword by remember { mutableStateOf("") }
    var customGroupName by remember { mutableStateOf("") }
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
                TabRow(selectedTabIndex = selectedTab) {
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
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No keyword clusters detected yet.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Contacts with similar roles (e.g. 'classmate', 'advocate', 'doctor', 'gym') will appear here automatically. You can also use the Custom Keyword tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(360.dp),
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
                                            Text(cluster.suggestedCategory, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
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
                                            "${cluster.contacts.size} contacts found: " + cluster.contacts.take(4).joinToString(", ") { it.name } + if (cluster.contacts.size > 4) "..." else "",
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
                                            onClick = { onAssign(clusterGroupName.ifBlank { cluster.suggestedCategory }, cluster.contacts.map { it.id }) },
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
                                    customGroupName = it.trim().replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
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
                            Text("Or pick existing group:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groups.take(3).forEach { g ->
                                    FilterPill(g.name, customGroupName.equals(g.name, ignoreCase = true)) {
                                        customGroupName = g.name
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
                                Text("${customMatches.size} match(es)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                if (customMatches.isNotEmpty()) {
                                    TextButton(onClick = {
                                        if (selectedCustomContactIds.size == customMatches.size) selectedCustomContactIds.clear()
                                        else {
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
                                        Text(contact.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        val sub = contact.phone.ifBlank { contact.company.ifBlank { contact.note } }
                                        if (sub.isNotBlank()) {
                                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            if (customMatches.isEmpty()) {
                                Text("No contacts matching '$customKeyword'.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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

@Composable
private fun GroupPickerDialog(title: String, groups: List<com.example.contactexplorer.data.ContactGroup>, selectedGroupId: Long?, mixedSelection: Boolean, onDismiss: () -> Unit, onSelect: (Long?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) { Text("Ungrouped", Modifier.weight(1f)); if (!mixedSelection && selectedGroupId == null) Icon(Icons.Default.Check, null) }
                groups.forEach { group -> TextButton(onClick = { onSelect(group.id) }, modifier = Modifier.fillMaxWidth()) { Text(group.name, Modifier.weight(1f)); if (!mixedSelection && selectedGroupId == group.id) Icon(Icons.Default.Check, null) } }
                if (groups.isEmpty()) Text("Create a group from the Groups tab first.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TagPickerDialog(tags: List<com.example.contactexplorer.data.ContactTag>, onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    var selected by remember { mutableStateOf<Long?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add tag") },
        text = {
            if (tags.isEmpty()) Text("Create a tag from More features → Tags first.")
            else Column {
                tags.forEach { tag -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected == tag.id, onClick = { selected = tag.id })
                    Text(tag.name)
                } }
            }
        },
        confirmButton = { TextButton(enabled = selected != null, onClick = { selected?.let(onSelect) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ThemeModeDialog(mode: ThemeMode, onDismiss: () -> Unit, onSelect: (ThemeMode) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance & Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeMode.entries.forEach { option ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == option, onClick = { onSelect(option) })
                        Spacer(Modifier.width(8.dp))
                        Text(when (option) {
                            ThemeMode.DARK -> "Icon Neon Dark (Obsidian & Amber)"
                            ThemeMode.SYSTEM -> "System Default"
                            ThemeMode.LIGHT -> "Clean Light"
                        }, Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun TextEntryDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text("Group name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onSave(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ConfirmDialog(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
