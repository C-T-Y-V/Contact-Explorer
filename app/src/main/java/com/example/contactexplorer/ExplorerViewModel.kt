package com.example.contactexplorer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactDraft
import com.example.contactexplorer.data.ContactGroup
import com.example.contactexplorer.data.ContactGroupMembership
import com.example.contactexplorer.data.ContactEvent
import com.example.contactexplorer.data.ContactTag
import com.example.contactexplorer.data.ContactTagAssignment
import com.example.contactexplorer.data.ContactTransfer
import com.example.contactexplorer.data.ContactsRepository
import com.example.contactexplorer.data.DeletedContact
import com.example.contactexplorer.data.ExplorerDatabase
import com.example.contactexplorer.data.FavoriteContact
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.data.RecentContact
import com.example.contactexplorer.data.SmartGroupRule
import com.example.contactexplorer.data.OfflineIntelligence
import android.content.Context
import com.example.contactexplorer.permissions.ContactPermissions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactsRepository(application)
    private val dao = ExplorerDatabase.get(application).explorerDao()
    private val contactPermissions = ContactPermissions(application)

    var contacts by mutableStateOf<List<Contact>>(emptyList()); private set
    var groups by mutableStateOf<List<ContactGroup>>(emptyList()); private set
    var memberships by mutableStateOf<List<ContactGroupMembership>>(emptyList()); private set
    var favorites by mutableStateOf<Set<String>>(emptySet()); private set
    var recentIds by mutableStateOf<List<String>>(emptyList()); private set
    var tags by mutableStateOf<List<ContactTag>>(emptyList()); private set
    var tagAssignments by mutableStateOf<List<ContactTagAssignment>>(emptyList()); private set
    var deletedContacts by mutableStateOf<List<DeletedContact>>(emptyList()); private set
    var events by mutableStateOf<List<ContactEvent>>(emptyList()); private set
    var smartGroups by mutableStateOf<List<SmartGroupRule>>(emptyList()); private set
    var hasContactsPermission by mutableStateOf(false); private set
    var canWriteContacts by mutableStateOf(false); private set
    var canCallPhone by mutableStateOf(false); private set
    var themeMode by mutableStateOf(ThemeMode.SYSTEM); private set
    var loading by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var revision by mutableIntStateOf(0); private set

    init {
        themeMode = ThemeMode.entries.firstOrNull {
            it.name == getApplication<Application>().getSharedPreferences("explorer", Context.MODE_PRIVATE).getString("theme_mode", ThemeMode.DARK.name)
        } ?: ThemeMode.DARK
        viewModelScope.launch { runCatching { dao.observeGroups().collectLatest { groups = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.observeMemberships().collectLatest { memberships = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.observeFavorites().collectLatest { favorites = it.toSet(); revision++ } } }
        viewModelScope.launch { runCatching { dao.observeRecentIds().collectLatest { recentIds = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.observeTags().collectLatest { tags = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.observeTagAssignments().collectLatest { tagAssignments = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.observeDeletedContacts().collectLatest { deletedContacts = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.observeEvents().collectLatest { events = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.observeSmartGroups().collectLatest { smartGroups = it; revision++ } } }
        viewModelScope.launch { runCatching { dao.purgeOldDeletedContacts(System.currentTimeMillis() - TRASH_RETENTION_MS) } }

        // Live real-time address book observer
        viewModelScope.launch {
            runCatching {
                repository.observeContactsChanges().collectLatest {
                    if (hasContactsPermission) {
                        runCatching { repository.loadContacts() }
                            .onSuccess { contacts = it; revision++ }
                    }
                }
            }
        }
    }

    fun refresh() {
        val state = contactPermissions.currentState()
        hasContactsPermission = state.canRead
        canWriteContacts = state.canWrite
        canCallPhone = state.canCall
        if (!hasContactsPermission) { contacts = emptyList(); revision++; return }
        viewModelScope.launch {
            loading = true; revision++
            runCatching { repository.loadContacts() }
                .onSuccess { contacts = it }
                .onFailure { message = it.message ?: "Could not read contacts." }
            loading = false; revision++
        }
    }

    fun clearMessage() { message = null; revision++ }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        getApplication<Application>().getSharedPreferences("explorer", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode.name).apply()
        revision++
    }

    fun save(contactId: String?, draft: ContactDraft) {
        viewModelScope.launch {
            runCatching { repository.saveContact(contactId, draft) }
                .onSuccess { savedId ->
                    recordEvent(savedId, draft.name, if (contactId == null) "CREATED" else "UPDATED", "Contact details saved.")
                    message = "Contact saved."; refresh()
                }
                .onFailure { message = it.message ?: "Could not save contact."; revision++ }
        }
    }

    fun delete(contactId: String) {
        deleteContacts(listOf(contactId))
    }

    fun deleteContacts(contactIds: List<String>) {
        if (contactIds.isEmpty()) return
        val toDeleteSet = contactIds.toSet()
        val originalContacts = contacts
        contacts = contacts.filterNot { it.id in toDeleteSet }
        favorites = favorites - toDeleteSet
        revision++
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
                    message = it.message ?: "Could not delete contact."; revision++
                    failedCount++
                }
            }
            message = when {
                failedCount == 0 && deletedCount == 1 -> "Contact moved to Recycle Bin."
                failedCount == 0 -> "$deletedCount contacts moved to Recycle Bin."
                deletedCount == 0 -> "Could not move selected contacts to Recycle Bin."
                else -> "$deletedCount contacts moved; $failedCount failed."
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
            }.onSuccess { message = "Contact restored."; revision++; refresh() }
                .onFailure { message = it.message ?: "Could not restore contact."; revision++ }
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
                message = "Merged ${allSources.size + 1} contacts successfully."
                refresh()
            }.onFailure {
                message = "Could not merge contacts: ${it.message}"
                revision++
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
        favorites = if (willBeFavorite) favorites + contactId else favorites - contactId
        revision++
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
                .onFailure { message = "Could not create group: ${it.message}"; revision++ }
        }
    }

    fun renameGroup(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { dao.renameGroup(id, clean) }
    }

    fun deleteGroup(id: Long) {
        groups = groups.filterNot { it.id == id }
        memberships = memberships.filterNot { it.groupId == id }
        revision++
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
            message = "Merged ${sources.size} group(s) into '${target.name}'."
            revision++
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
            message = "Assigned ${contactIds.size} contacts to '$clean'."
            revision++
        }
    }

    fun addContactsToGroup(groupId: Long, contactIds: List<String>) {
        viewModelScope.launch {
            val grp = groups.firstOrNull { it.id == groupId }
            contactIds.forEach { cId ->
                dao.addMembership(ContactGroupMembership(cId, groupId))
            }
            recordEvent(null, grp?.name ?: "Group", "GROUPED", "Added ${contactIds.size} contacts to group.")
            message = "Added ${contactIds.size} contacts to ${grp?.name ?: "group"}."
            revision++
        }
    }

    fun moveToGroup(contactIds: List<String>, groupId: Long?) {
        val toUpdateSet = contactIds.toSet()
        val remaining = memberships.filterNot { it.contactId in toUpdateSet }
        memberships = if (groupId != null) remaining + contactIds.map { ContactGroupMembership(it, groupId) } else remaining
        revision++
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

    fun importContact(draft: ContactDraft) {
        viewModelScope.launch {
            runCatching { repository.saveContact(null, draft) }
                .onSuccess { id -> recordEvent(id, draft.name, "IMPORTED", "Imported contact."); refresh() }
                .onFailure { message = "Could not import ${draft.name}: ${it.message}"; revision++ }
        }
    }

    companion object { private const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000 }
}
