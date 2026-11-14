/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactDraft
import com.example.contactexplorer.data.ContactEvent
import com.example.contactexplorer.data.ContactGroup
import com.example.contactexplorer.data.ContactGroupMembership
import com.example.contactexplorer.data.ContactTag
import com.example.contactexplorer.data.ContactTagAssignment
import com.example.contactexplorer.data.ContactTransfer
import com.example.contactexplorer.data.ContactsRepository
import com.example.contactexplorer.data.DeletedContact
import com.example.contactexplorer.data.ExplorerDatabase
import com.example.contactexplorer.data.FavoriteContact
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.data.OfflineIntelligence
import com.example.contactexplorer.data.RecentContact
import com.example.contactexplorer.data.SmartGroupRule
import com.example.contactexplorer.data.VaultData
import com.example.contactexplorer.feature.appearance.ThemePreferences
import com.example.contactexplorer.feature.calling.CallingDispatcher
import com.example.contactexplorer.permissions.ContactPermissions
import com.example.contactexplorer.plugins.FeatureDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactsRepository(application)
    private val dao = ExplorerDatabase.get(application).explorerDao()
    private val contactPermissions = ContactPermissions(application)

    private val themePreferences by lazy { ThemePreferences(getApplication()) }
    val themeModeFlow: StateFlow<ThemeMode> get() = themePreferences.themeMode

    private val featurePreferences by lazy {
        getApplication<Application>().getSharedPreferences("explorer_features", Context.MODE_PRIVATE)
    }

    // --- Ephemeral Session / Interaction State ---
    private data class ExplorerSessionState(
        val contacts: List<Contact> = emptyList(),
        val selectedIds: Set<String> = emptySet(),
        val activeFilter: FilterMode = FilterMode.ALL,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val hasContactsPermission: Boolean = false,
        val canWriteContacts: Boolean = false,
        val canCallPhone: Boolean = false,
        val enabledFeatureIds: Set<String> = emptySet()
    )

    private val sessionState = MutableStateFlow(
        ExplorerSessionState(
            hasContactsPermission = contactPermissions.currentState().canRead,
            canWriteContacts = contactPermissions.currentState().canWrite,
            canCallPhone = contactPermissions.currentState().canCall
        )
    )

    // --- Room Local Database Snapshot Model ---
    private data class RoomVaultData(
        val groups: List<ContactGroup> = emptyList(),
        val memberships: List<ContactGroupMembership> = emptyList(),
        val favorites: Set<String> = emptySet(),
        val recentIds: List<String> = emptyList(),
        val tags: List<ContactTag> = emptyList(),
        val tagAssignments: List<ContactTagAssignment> = emptyList(),
        val deletedContacts: List<DeletedContact> = emptyList(),
        val events: List<ContactEvent> = emptyList(),
        val smartGroups: List<SmartGroupRule> = emptyList()
    )

    private data class ActivitySnapshot(
        val favorites: Set<String>,
        val recentIds: List<String>,
        val deleted: List<DeletedContact>,
        val events: List<ContactEvent>
    )

    // Sub-group 1: Groups, memberships, and smart group rules
    private val groupsFlow = combine(
        dao.observeGroups(),
        dao.observeMemberships(),
        dao.observeSmartGroups()
    ) { groups, memberships, smartGroups ->
        Triple(groups, memberships, smartGroups)
    }

    // Sub-group 2: Tags and tag assignments
    private val tagsFlow = combine(
        dao.observeTags(),
        dao.observeTagAssignments()
    ) { tags, assignments ->
        Pair(tags, assignments)
    }

    // Sub-group 3: Favorites, recent contacts, trash, and activity audit log
    private val activityFlow = combine(
        dao.observeFavorites(),
        dao.observeRecentIds(),
        dao.observeDeletedContacts(),
        dao.observeEvents()
    ) { favorites, recentIds, deleted, events ->
        ActivitySnapshot(favorites.toSet(), recentIds, deleted, events)
    }

    // Combined Room Vault Flow
    private val roomVaultFlow: Flow<RoomVaultData> = combine(
        groupsFlow,
        tagsFlow,
        activityFlow
    ) { (groups, memberships, smartGroups), (tags, assignments), activity ->
        RoomVaultData(
            groups = groups,
            memberships = memberships,
            favorites = activity.favorites,
            recentIds = activity.recentIds,
            tags = tags,
            tagAssignments = assignments,
            deletedContacts = activity.deleted,
            events = activity.events,
            smartGroups = smartGroups
        )
    }

    // --- Single Atomic StateFlow via combine() & stateIn(WhileSubscribed(5000)) ---
    val uiState: StateFlow<ExplorerUiState> = combine(
        roomVaultFlow,
        themePreferences.themeMode,
        sessionState
    ) { vault, themeMode, session ->
        ExplorerUiState(
            contacts = session.contacts,
            groups = vault.groups,
            favorites = vault.favorites,
            selectedIds = session.selectedIds,
            themePreset = ThemePreset.fromThemeMode(themeMode),
            activeFilter = session.activeFilter,
            isLoading = session.isLoading,
            errorMessage = session.errorMessage,
            memberships = vault.memberships,
            recentIds = vault.recentIds,
            tags = vault.tags,
            tagAssignments = vault.tagAssignments,
            deletedContacts = vault.deletedContacts,
            events = vault.events,
            smartGroups = vault.smartGroups,
            hasContactsPermission = session.hasContactsPermission,
            canWriteContacts = session.canWriteContacts,
            canCallPhone = session.canCallPhone,
            enabledFeatureIds = session.enabledFeatureIds,
            themeMode = themeMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExplorerUiState(
            themeMode = themePreferences.themeMode.value,
            themePreset = ThemePreset.fromThemeMode(themePreferences.themeMode.value),
            hasContactsPermission = contactPermissions.currentState().canRead,
            canWriteContacts = contactPermissions.currentState().canWrite,
            canCallPhone = contactPermissions.currentState().canCall
        )
    )

    // Immediate Compose State snapshot for legacy property access
    var stateSnapshot by mutableStateOf(uiState.value)
        private set

    // --- Delegated Backward-Compatible Getters ---
    val contacts: List<Contact> get() = stateSnapshot.contacts
    val groups: List<ContactGroup> get() = stateSnapshot.groups
    val memberships: List<ContactGroupMembership> get() = stateSnapshot.memberships
    val favorites: Set<String> get() = stateSnapshot.favorites
    val recentIds: List<String> get() = stateSnapshot.recentIds
    val tags: List<ContactTag> get() = stateSnapshot.tags
    val tagAssignments: List<ContactTagAssignment> get() = stateSnapshot.tagAssignments
    val deletedContacts: List<DeletedContact> get() = stateSnapshot.deletedContacts
    val events: List<ContactEvent> get() = stateSnapshot.events
    val smartGroups: List<SmartGroupRule> get() = stateSnapshot.smartGroups
    val hasContactsPermission: Boolean get() = stateSnapshot.hasContactsPermission
    val canWriteContacts: Boolean get() = stateSnapshot.canWriteContacts
    val canCallPhone: Boolean get() = stateSnapshot.canCallPhone

    val themeMode: ThemeMode get() = stateSnapshot.themeMode
    val themePreset: ThemePreset get() = stateSnapshot.themePreset
    val activeFilter: FilterMode get() = stateSnapshot.activeFilter
    val selectedIds: Set<String> get() = stateSnapshot.selectedIds
    val loading: Boolean get() = stateSnapshot.isLoading
    val isLoading: Boolean get() = stateSnapshot.isLoading
    val message: String? get() = stateSnapshot.errorMessage
    val errorMessage: String? get() = stateSnapshot.errorMessage
    val enabledFeatureIds: Set<String> get() = stateSnapshot.enabledFeatureIds

    @Deprecated(
        message = "The revision counter hack has been deprecated and eliminated in favor of reactive Room DAO combine() and StateFlow recomposition.",
        replaceWith = ReplaceWith("uiState"),
        level = DeprecationLevel.WARNING
    )
    val revision: Int get() = 0

    init {
        // Synchronize StateFlow updates into Compose state snapshot
        viewModelScope.launch {
            uiState.collect { snapshot ->
                stateSnapshot = snapshot
            }
        }

        // Auto-purge old trash contacts (30-day retention)
        viewModelScope.launch {
            runCatching { dao.purgeOldDeletedContacts(System.currentTimeMillis() - TRASH_RETENTION_MS) }
        }

        // Live real-time address book observer from ContentResolver
        viewModelScope.launch {
            runCatching {
                repository.observeContactsChanges().collectLatest {
                    if (contactPermissions.currentState().canRead) {
                        runCatching { repository.loadContacts() }
                            .onSuccess { loaded -> sessionState.update { it.copy(contacts = loaded) } }
                    }
                }
            }
        }
    }

    fun refresh() {
        val state = contactPermissions.currentState()
        sessionState.update {
            it.copy(
                hasContactsPermission = state.canRead,
                canWriteContacts = state.canWrite,
                canCallPhone = state.canCall
            )
        }
        if (!state.canRead) {
            sessionState.update { it.copy(contacts = emptyList(), isLoading = false) }
            return
        }
        viewModelScope.launch {
            sessionState.update { it.copy(isLoading = true) }
            runCatching { repository.loadContacts() }
                .onSuccess { loaded ->
                    sessionState.update { it.copy(contacts = loaded, isLoading = false) }
                }
                .onFailure { error ->
                    sessionState.update {
                        it.copy(
                            errorMessage = error.message ?: "Could not read contacts.",
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        sessionState.update { it.copy(errorMessage = null) }
    }

    fun clearErrorMessage() = clearMessage()

    fun updateThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun setThemePreset(preset: ThemePreset) {
        updateThemeMode(preset.toThemeMode())
    }

    fun cycleThemeMode(): ThemeMode {
        return themePreferences.cycleNextTheme()
    }

    // --- Selection Management ---
    fun toggleSelection(contactId: String) {
        sessionState.update { state ->
            val updated = if (contactId in state.selectedIds) state.selectedIds - contactId else state.selectedIds + contactId
            state.copy(selectedIds = updated)
        }
    }

    fun selectAll(ids: Collection<String>) {
        sessionState.update { it.copy(selectedIds = ids.toSet()) }
    }

    fun clearSelection() {
        sessionState.update { it.copy(selectedIds = emptySet()) }
    }

    // --- Filter Management ---
    fun setActiveFilter(filter: FilterMode) {
        sessionState.update { it.copy(activeFilter = filter) }
    }

    fun initFeatures(registeredFeatures: List<FeatureDescriptor>) {
        val defaultEnabled = registeredFeatures.filter { it.enabledByDefault }.mapTo(mutableSetOf()) { it.id }
        val saved = featurePreferences.getStringSet("enabled_ids", null)?.toSet()
        val known = featurePreferences.getStringSet("known_ids", null)?.toSet().orEmpty()
        val registeredIds = registeredFeatures.mapTo(mutableSetOf()) { it.id }

        val active = (saved ?: defaultEnabled).toMutableSet().apply {
            if (saved != null) {
                registeredFeatures.filter { it.enabledByDefault && it.id !in known }.forEach { add(it.id) }
            }
        }
        sessionState.update { it.copy(enabledFeatureIds = active) }
        featurePreferences.edit()
            .putStringSet("enabled_ids", active)
            .putStringSet("known_ids", registeredIds)
            .apply()
    }

    fun toggleFeature(featureId: String, enabled: Boolean) {
        val updated = if (enabled) enabledFeatureIds + featureId else enabledFeatureIds - featureId
        sessionState.update { it.copy(enabledFeatureIds = updated) }
        featurePreferences.edit().putStringSet("enabled_ids", updated).apply()
    }

    fun launchCall(number: String) {
        recordCallByNumber(number)
        val app = getApplication<Application>()
        if (canCallPhone) {
            CallingDispatcher.launchDirectCall(app, number)
        } else {
            CallingDispatcher.launchDialAction(app, number)
        }
    }

    fun launchSms(number: String) {
        CallingDispatcher.launchSmsAction(getApplication(), number)
    }

    fun launchEmail(email: String) {
        CallingDispatcher.launchEmailAction(getApplication(), email)
    }

    private fun recordCallByNumber(number: String) {
        val contact = contacts.find { c ->
            c.phone == number || c.phones.any { it.value == number }
        }
        if (contact != null) recordCall(contact)
    }

    fun shareText(title: String, text: String) {
        val app = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, title).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        app.startActivity(chooser)
    }

    fun shareFile(filename: String, content: String, mimeType: String) {
        runCatching {
            val app = getApplication<Application>()
            val exportDir = java.io.File(app.cacheDir, "exports").apply { mkdirs() }
            val file = java.io.File(exportDir, filename).apply { writeText(content) }
            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, filename)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share contacts via...").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            app.startActivity(chooser)
        }.onFailure {
            sessionState.update { state -> state.copy(errorMessage = "Could not share file: ${it.message}") }
        }
    }

    fun writeExportToUri(uri: Uri, content: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(content)
                } ?: error("Could not open file output stream.")
            }.onSuccess {
                viewModelScope.launch(Dispatchers.Main) { onSuccess() }
            }.onFailure { e ->
                viewModelScope.launch(Dispatchers.Main) { onError(e.message ?: "Failed to write file.") }
            }
        }
    }

    fun importContactsFromUri(uri: Uri, format: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val text = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    it.readText()
                } ?: error("Could not open file input stream.")

                val trimmed = text.trimStart()
                if (format == "vault" || (trimmed.startsWith("{") && trimmed.contains("\"contact_explorer_vault\""))) {
                    val vault = ContactTransfer.fromVaultJson(text)
                    if (vault != null) {
                        importVaultData(vault) { count ->
                            viewModelScope.launch(Dispatchers.Main) {
                                onComplete("Restored $count contacts with groups & tags from Vault")
                            }
                        }
                    } else {
                        error("Invalid or corrupted Vault file.")
                    }
                } else {
                    val drafts = if (format == "csv") ContactTransfer.fromCsv(text) else ContactTransfer.fromVcf(text)
                    drafts.forEach { importContact(it) }
                    viewModelScope.launch(Dispatchers.Main) {
                        onComplete("Importing ${drafts.size} contacts")
                    }
                }
            }.onFailure { e ->
                viewModelScope.launch(Dispatchers.Main) {
                    onComplete(e.message ?: "Could not import file.")
                }
            }
        }
    }

    fun save(contactId: String?, draft: ContactDraft) {
        viewModelScope.launch {
            runCatching { repository.saveContact(contactId, draft) }
                .onSuccess { savedId ->
                    recordEvent(savedId, draft.name, if (contactId == null) "CREATED" else "UPDATED", "Contact details saved.")
                    sessionState.update { it.copy(errorMessage = "Contact saved.") }
                    refresh()
                }
                .onFailure { error ->
                    sessionState.update { it.copy(errorMessage = error.message ?: "Could not save contact.") }
                }
        }
    }

    fun delete(contactId: String) {
        deleteContacts(listOf(contactId))
    }

    fun deleteContacts(contactIds: List<String>) {
        if (contactIds.isEmpty()) return
        val toDeleteSet = contactIds.toSet()
        sessionState.update {
            it.copy(
                contacts = it.contacts.filterNot { c -> c.id in toDeleteSet },
                selectedIds = it.selectedIds - toDeleteSet
            )
        }
        viewModelScope.launch {
            var deletedCount = 0
            var failedCount = 0
            for (contactId in contactIds) {
                val contact = contacts.firstOrNull { it.id == contactId }
                if (contact == null) { failedCount++; continue }
                val savedGroups = memberships.filter { it.contactId == contactId }.map { it.groupId }
                val savedTags = tagAssignments.filter { it.contactId == contactId }.map { it.tagId }
                val snapshot = DeletedContact(
                    contactId = contact.id, lookupKey = contact.lookupKey, name = contact.name,
                    phone = contact.phone, email = contact.email, company = contact.company,
                    address = contact.address, note = contact.note, lastModified = contact.lastModified,
                    timesContacted = contact.timesContacted, deletedAt = System.currentTimeMillis(),
                    groupIds = savedGroups.joinToString(","), tagIds = savedTags.joinToString(","),
                    wasFavorite = contactId in favorites,
                    vcard = ContactTransfer.toSingleVcf(contact),
                )
                var providerDeleted = false
                runCatching {
                    dao.saveDeletedContact(snapshot)
                    check(repository.deleteContact(contactId) > 0) { "The contact could not be deleted." }
                    providerDeleted = true
                    dao.clearFavorite(contactId); dao.clearRecent(contactId); dao.clearGroupsForContact(contactId); dao.clearTagsForContact(contactId)
                    recordEvent(contactId, contact.name, "DELETED", "Moved to Recycle Bin.")
                    deletedCount++
                }.onFailure {
                    if (!providerDeleted) dao.removeDeletedContact(contactId)
                    sessionState.update { state -> state.copy(errorMessage = it.message ?: "Could not delete contact.") }
                    failedCount++
                }
            }
            sessionState.update { state ->
                state.copy(
                    errorMessage = when {
                        failedCount == 0 && deletedCount == 1 -> "Contact moved to Recycle Bin."
                        failedCount == 0 -> "$deletedCount contacts moved to Recycle Bin."
                        deletedCount == 0 -> "Could not move selected contacts to Recycle Bin."
                        else -> "$deletedCount contacts moved; $failedCount failed."
                    }
                )
            }
            refresh()
        }
    }

    fun restoreDeleted(contact: DeletedContact) {
        viewModelScope.launch {
            runCatching {
                val draft = if (contact.vcard.isNotBlank()) {
                    ContactTransfer.fromVcf(contact.vcard).firstOrNull() ?: ContactDraft(
                        contact.name, contact.phone, contact.email, contact.company, contact.address, contact.note
                    )
                } else {
                    ContactDraft(contact.name, contact.phone, contact.email, contact.company, contact.address, contact.note)
                }
                val newId = repository.saveContact(null, draft)
                contact.groupIds.split(',').mapNotNull { it.toLongOrNull() }.forEach { dao.addMembership(ContactGroupMembership(newId, it)) }
                if (contact.wasFavorite) dao.setFavorite(FavoriteContact(newId))
                contact.tagIds.split(',').mapNotNull { it.toLongOrNull() }.forEach { dao.assignTag(ContactTagAssignment(newId, it)) }
                dao.removeDeletedContact(contact.contactId)
                recordEvent(newId, contact.name, "RESTORED", "Restored from Recycle Bin.")
            }.onSuccess {
                sessionState.update { it.copy(errorMessage = "Contact restored.") }
                refresh()
            }.onFailure { error ->
                sessionState.update { it.copy(errorMessage = error.message ?: "Could not restore contact.") }
            }
        }
    }

    fun permanentlyDelete(contact: DeletedContact) {
        viewModelScope.launch {
            dao.removeDeletedContact(contact.contactId)
            recordEvent(contact.contactId, contact.name, "PURGED", "Permanently deleted from Recycle Bin.")
        }
    }

    fun mergeContacts(target: Contact, sources: List<Contact>) {
        viewModelScope.launch {
            runCatching {
                val allSources = sources.filter { it.id != target.id }
                if (allSources.isEmpty()) return@launch

                val combinedPhones = (target.phones + allSources.flatMap { it.phones } +
                    (if (target.phone.isNotBlank()) listOf(LabeledValue(target.phone)) else emptyList()) +
                    allSources.mapNotNull { it.phone.takeIf(String::isNotBlank)?.let { p -> LabeledValue(p) } })
                    .filter { it.value.isNotBlank() }
                    .distinctBy { OfflineIntelligence.normalizePhone(it.value) }

                val combinedEmails = (target.emails + allSources.flatMap { it.emails } +
                    (if (target.email.isNotBlank()) listOf(LabeledValue(target.email, "Home", 1)) else emptyList()) +
                    allSources.mapNotNull { it.email.takeIf(String::isNotBlank)?.let { e -> LabeledValue(e, "Home", 1) } })
                    .filter { it.value.isNotBlank() }
                    .distinctBy { it.value.lowercase() }

                val bestCompany = (listOf(target.company) + allSources.map { it.company })
                    .firstOrNull { it.isNotBlank() }.orEmpty()
                val bestAddress = (listOf(target.address) + allSources.map { it.address })
                    .maxByOrNull { it.length }.orEmpty()
                val combinedNotes = (listOf(target.note) + allSources.map { it.note })
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString("\n")

                val mergedDraft = ContactDraft(
                    name = target.name.ifBlank { allSources.firstOrNull()?.name ?: "Merged Contact" },
                    phone = combinedPhones.firstOrNull()?.value.orEmpty(),
                    email = combinedEmails.firstOrNull()?.value.orEmpty(),
                    company = bestCompany,
                    address = bestAddress,
                    note = combinedNotes,
                    phones = combinedPhones,
                    emails = combinedEmails,
                )

                repository.saveContact(target.id, mergedDraft)

                val sourceIds = allSources.map { it.id }
                val existingTargetGroupIds = memberships.filter { it.contactId == target.id }.map { it.groupId }.toSet()
                val sourceMemberships = memberships.filter { it.contactId in sourceIds }
                sourceMemberships.forEach { sm ->
                    if (sm.groupId !in existingTargetGroupIds) {
                        dao.addMembership(ContactGroupMembership(target.id, sm.groupId))
                    }
                }

                val existingTargetTagIds = tagAssignments.filter { it.contactId == target.id }.map { it.tagId }.toSet()
                val sourceTags = tagAssignments.filter { it.contactId in sourceIds }
                sourceTags.forEach { st ->
                    if (st.tagId !in existingTargetTagIds) {
                        dao.assignTag(ContactTagAssignment(target.id, st.tagId))
                    }
                }

                if (sourceIds.any { it in favorites }) {
                    dao.setFavorite(FavoriteContact(target.id))
                }

                sourceIds.forEach { sId ->
                    repository.deleteContact(sId)
                    dao.clearFavorite(sId)
                    dao.clearRecent(sId)
                    dao.clearGroupsForContact(sId)
                    dao.clearTagsForContact(sId)
                }

                recordEvent(target.id, target.name, "MERGED", "Merged ${allSources.size} duplicate contact(s).")
                sessionState.update { it.copy(errorMessage = "Merged ${allSources.size + 1} contacts successfully.") }
                refresh()
            }.onFailure { error ->
                sessionState.update { it.copy(errorMessage = "Could not merge contacts: ${error.message}") }
            }
        }
    }

    fun recordCall(contact: Contact) {
        viewModelScope.launch { recordEvent(contact.id, contact.name, "CALLED", "Call started.") }
    }

    fun recordShare(contactIds: List<String>) {
        viewModelScope.launch { contactIds.forEach { id -> contacts.find { it.id == id }?.let { recordEvent(it.id, it.name, "SHARED", "Contact shared.") } } }
    }

    private suspend fun recordEvent(id: String?, name: String, action: String, details: String) {
        dao.addEvent(ContactEvent(contactId = id, contactName = name, action = action, details = details, occurredAt = System.currentTimeMillis()))
    }

    fun toggleFavorite(contactId: String) {
        val willBeFavorite = contactId !in favorites
        viewModelScope.launch {
            if (willBeFavorite) dao.setFavorite(FavoriteContact(contactId)) else dao.removeFavorite(contactId)
            contacts.find { it.id == contactId }?.let { recordEvent(it.id, it.name, "FAVORITE", "Favorite status changed.") }
        }
    }

    fun markViewed(contactId: String) {
        viewModelScope.launch {
            dao.recordRecent(RecentContact(contactId, System.currentTimeMillis()))
            contacts.find { it.id == contactId }?.let { recordEvent(it.id, it.name, "OPENED", "Contact viewed.") }
        }
    }

    fun createGroup(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            runCatching { dao.createGroup(ContactGroup(name = clean)) }
                .onFailure { error ->
                    sessionState.update { it.copy(errorMessage = "Could not create group: ${error.message}") }
                }
        }
    }

    fun renameGroup(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { dao.renameGroup(id, clean) }
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch { dao.clearGroupMemberships(id); dao.deleteGroup(id) }
    }

    fun mergeGroups(targetGroupId: Long, sourceGroupIds: List<Long>) {
        viewModelScope.launch {
            val target = groups.firstOrNull { it.id == targetGroupId } ?: return@launch
            val sources = groups.filter { it.id in sourceGroupIds && it.id != targetGroupId }
            if (sources.isEmpty()) return@launch

            val existingTargetMembers = memberships.filter { it.groupId == targetGroupId }.map { it.contactId }.toSet()
            var transferredCount = 0

            sources.forEach { source ->
                val sourceMembers = memberships.filter { it.groupId == source.id }
                sourceMembers.forEach { sm ->
                    if (sm.contactId !in existingTargetMembers) {
                        dao.addMembership(ContactGroupMembership(sm.contactId, targetGroupId))
                        transferredCount++
                    }
                }
                dao.clearGroupMemberships(source.id)
                dao.deleteGroup(source.id)
            }

            recordEvent(null, target.name, "MERGED_GROUPS", "Merged ${sources.size} group(s) into '${target.name}'.")
            sessionState.update { it.copy(errorMessage = "Merged ${sources.size} group(s) into '${target.name}'.") }
        }
    }

    fun createGroupAndAssign(groupName: String, contactIds: List<String>) {
        val clean = groupName.trim()
        if (clean.isEmpty() || contactIds.isEmpty()) return
        viewModelScope.launch {
            val existing = groups.firstOrNull { it.name.equals(clean, ignoreCase = true) }
            val groupId = existing?.id ?: dao.createGroup(ContactGroup(name = clean))
            contactIds.forEach { cId ->
                dao.addMembership(ContactGroupMembership(cId, groupId))
            }
            recordEvent(null, clean, "GROUPED", "Categorized ${contactIds.size} contacts into '$clean'.")
            sessionState.update { it.copy(errorMessage = "Assigned ${contactIds.size} contacts to '$clean'.") }
        }
    }

    fun addContactsToGroup(groupId: Long, contactIds: List<String>) {
        viewModelScope.launch {
            val grp = groups.firstOrNull { it.id == groupId }
            contactIds.forEach { cId ->
                dao.addMembership(ContactGroupMembership(cId, groupId))
            }
            recordEvent(null, grp?.name ?: "Group", "GROUPED", "Added ${contactIds.size} contacts to group.")
            sessionState.update { it.copy(errorMessage = "Added ${contactIds.size} contacts to ${grp?.name ?: "group"}.") }
        }
    }

    fun moveToGroup(contactIds: List<String>, groupId: Long?) {
        viewModelScope.launch {
            contactIds.forEach { contactId ->
                dao.clearGroupsForContact(contactId)
                if (groupId != null) dao.addMembership(ContactGroupMembership(contactId, groupId))
            }
        }
    }

    fun saveTag(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || tags.any { it.name.equals(clean, ignoreCase = true) }) return
        viewModelScope.launch { dao.saveTag(ContactTag(name = clean)) }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch { dao.clearTagAssignments(tagId); dao.deleteTag(tagId) }
    }

    fun addTagToContacts(contactIds: List<String>, tagId: Long) {
        viewModelScope.launch { contactIds.forEach { dao.assignTag(ContactTagAssignment(it, tagId)) } }
    }

    fun removeTag(contactId: String, tagId: Long) {
        viewModelScope.launch { dao.clearTagsForContact(contactId); tagAssignments.filter { it.contactId == contactId && it.tagId != tagId }.forEach { dao.assignTag(it) } }
    }

    fun createSmartGroup(name: String, field: String, value: String) {
        if (name.isBlank() || value.isBlank()) return
        viewModelScope.launch { dao.createSmartGroup(SmartGroupRule(name = name.trim(), field = field, value = value.trim())) }
    }

    fun deleteSmartGroup(id: Long) { viewModelScope.launch { dao.deleteSmartGroup(id) } }

    fun autoTagContacts() {
        viewModelScope.launch {
            val tagIds = tags.associate { it.name.lowercase() to it.id }.toMutableMap()
            contacts.forEach { contact ->
                OfflineIntelligence.suggestedTags(contact).forEach { tagName ->
                    val id = tagIds[tagName.lowercase()] ?: dao.saveTag(ContactTag(name = tagName)).also { tagIds[tagName.lowercase()] = it }
                    dao.assignTag(ContactTagAssignment(contact.id, id))
                }
            }
            recordEvent(null, "Contacts", "AUTO_TAGGED", "Offline rules suggested tags for contacts.")
        }
    }

    fun generateVaultBackupJson(): String {
        val groupNamesMap = memberships.groupBy { it.contactId }.mapValues { (_, links) -> links.mapNotNull { link -> groups.find { it.id == link.groupId }?.name } }
        val tagNamesMap = tagAssignments.groupBy { it.contactId }.mapValues { (_, links) -> links.mapNotNull { link -> tags.find { it.id == link.tagId }?.name } }
        return ContactTransfer.toVaultJson(
            contacts = contacts,
            groupMap = groupNamesMap,
            tagMap = tagNamesMap,
            favorites = favorites,
            groupsList = groups.map { it.name },
            tagsList = tags.map { it.name },
            smartRules = smartGroups.map { Triple(it.name, it.field, it.value) },
        )
    }

    fun importVaultData(vault: VaultData, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            var count = 0
            val groupMap = groups.associate { it.name.lowercase() to it.id }.toMutableMap()
            for (gName in vault.groups) {
                if (gName.lowercase() !in groupMap) {
                    val newId = dao.createGroup(ContactGroup(name = gName))
                    groupMap[gName.lowercase()] = newId
                }
            }
            val tagMap = tags.associate { it.name.lowercase() to it.id }.toMutableMap()
            for (tName in vault.tags) {
                if (tName.lowercase() !in tagMap) {
                    val newId = dao.saveTag(ContactTag(name = tName))
                    tagMap[tName.lowercase()] = newId
                }
            }
            for ((name, field, value) in vault.smartRules) {
                if (smartGroups.none { it.name == name && it.field == field && it.value == value }) {
                    dao.createSmartGroup(SmartGroupRule(name = name, field = field, value = value))
                }
            }
            for (draft in vault.contacts) {
                try {
                    val newId = repository.saveContact(null, draft)
                    count++
                    for (gName in draft.categories) {
                        val gId = groupMap[gName.lowercase()] ?: dao.createGroup(ContactGroup(name = gName)).also { groupMap[gName.lowercase()] = it }
                        dao.addMembership(ContactGroupMembership(newId, gId))
                    }
                    for (tName in draft.tags) {
                        val tId = tagMap[tName.lowercase()] ?: dao.saveTag(ContactTag(name = tName)).also { tagMap[tName.lowercase()] = it }
                        dao.assignTag(ContactTagAssignment(newId, tId))
                    }
                    if (draft.isFavorite) {
                        dao.setFavorite(FavoriteContact(newId))
                    }
                } catch (e: Exception) {
                    // Skip failed individual contacts
                }
            }
            recordEvent(null, "Vault", "RESTORED", "Restored $count contacts with full group & tag structure from Vault.")
            sessionState.update { it.copy(errorMessage = "Restored $count contacts from Vault.") }
            refresh()
            onComplete(count)
        }
    }

    fun importContact(draft: ContactDraft) {
        viewModelScope.launch {
            try {
                val id = repository.saveContact(null, draft)
                // Auto-reconstruct categories/groups if present
                for (groupName in draft.categories) {
                    val existingGroup = groups.firstOrNull { it.name.equals(groupName, ignoreCase = true) }
                    val gId = existingGroup?.id ?: dao.createGroup(ContactGroup(name = groupName))
                    dao.addMembership(ContactGroupMembership(id, gId))
                }
                // Auto-reconstruct tags if present
                for (tagName in draft.tags) {
                    val existingTag = tags.firstOrNull { it.name.equals(tagName, ignoreCase = true) }
                    val tId = existingTag?.id ?: dao.saveTag(ContactTag(name = tagName))
                    dao.assignTag(ContactTagAssignment(id, tId))
                }
                if (draft.isFavorite) {
                    dao.setFavorite(FavoriteContact(id))
                }
                recordEvent(id, draft.name, "IMPORTED", "Imported contact.")
                refresh()
            } catch (e: Exception) {
                sessionState.update { it.copy(errorMessage = "Could not import ${draft.name}: ${e.message}") }
            }
        }
    }

    companion object { private const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000 }
}
