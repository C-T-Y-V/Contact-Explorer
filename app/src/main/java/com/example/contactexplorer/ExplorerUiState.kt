/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer

import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactEvent
import com.example.contactexplorer.data.ContactGroup
import com.example.contactexplorer.data.ContactGroupMembership
import com.example.contactexplorer.data.ContactTag
import com.example.contactexplorer.data.ContactTagAssignment
import com.example.contactexplorer.data.DeletedContact
import com.example.contactexplorer.data.SmartGroupRule

/**
 * Single atomic UI state for Contact Explorer Enterprise.
 * Replaces loose mutable state variables with a unified, unidirectional data flow contract.
 *
 * @param contacts List of active contacts loaded into memory.
 * @param groups List of user contact groups.
 * @param favorites Set of contact IDs designated as favorites.
 * @param selectedIds Set of contact IDs actively selected in multi-select mode.
 * @param themePreset Active theme styling preset (defaults to [ThemePreset.OBSIDIAN]).
 * @param activeFilter Active filter/section mode (defaults to [FilterMode.ALL]).
 * @param isLoading Indicates whether a background loading or sync operation is in flight.
 * @param errorMessage Transient error or user-facing notification message, if any.
 */
data class ExplorerUiState(
    val contacts: List<Contact> = emptyList(),
    val groups: List<ContactGroup> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val selectedIds: Set<String> = emptySet(),
    val themePreset: ThemePreset = ThemePreset.OBSIDIAN,
    val activeFilter: FilterMode = FilterMode.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val memberships: List<ContactGroupMembership> = emptyList(),
    val recentIds: List<String> = emptyList(),
    val tags: List<ContactTag> = emptyList(),
    val tagAssignments: List<ContactTagAssignment> = emptyList(),
    val deletedContacts: List<DeletedContact> = emptyList(),
    val events: List<ContactEvent> = emptyList(),
    val smartGroups: List<SmartGroupRule> = emptyList(),
    val hasContactsPermission: Boolean = false,
    val canWriteContacts: Boolean = false,
    val canCallPhone: Boolean = false,
    val enabledFeatureIds: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.OBSIDIAN_NEON
) {
    val isSelectionActive: Boolean get() = selectedIds.isNotEmpty()
    val selectedCount: Int get() = selectedIds.size
}
