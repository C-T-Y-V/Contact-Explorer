/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.domain.usecase

import com.example.contactexplorer.FilterMode
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactGroupMembership

/**
 * Pure Kotlin Use Case encapsulating contact search, section filtering, and sorting logic.
 * Adheres strictly to Clean Architecture: 100% testable on local JVM with zero Android SDK dependencies.
 */
class FilterContactsUseCase {

    operator fun invoke(
        contacts: List<Contact>,
        query: String = "",
        filterMode: FilterMode = FilterMode.ALL,
        favorites: Set<String> = emptySet(),
        recentIds: List<String> = emptyList(),
        memberships: List<ContactGroupMembership> = emptyList(),
        selectedGroupId: Long? = null,
        sortBy: SortOption = SortOption.NAME_ASC
    ): List<Contact> {
        // Step 1: Filter by Section / FilterMode
        val sectionFiltered = when (filterMode) {
            FilterMode.ALL -> contacts
            FilterMode.FAVORITES -> contacts.filter { it.id in favorites }
            FilterMode.RECENT -> {
                val recentSet = recentIds.toSet()
                contacts.filter { it.id in recentSet }
            }
            FilterMode.GROUPS -> when (selectedGroupId) {
                null -> contacts
                -1L -> contacts.filter { c -> memberships.none { it.contactId == c.id } } // Unassigned
                else -> contacts.filter { contact ->
                    memberships.any { it.contactId == contact.id && it.groupId == selectedGroupId }
                }
            }
        }

        // Step 2: Search Query Matching (Name, Phone, Email, Company, Notes)
        val searchFiltered = if (query.isBlank()) {
            sectionFiltered
        } else {
            val terms = query.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            sectionFiltered.filter { contact ->
                matchesSearch(contact, terms)
            }
        }

        // Step 3: Sort Order
        return when (sortBy) {
            SortOption.NAME_ASC -> searchFiltered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOption.NAME_DESC -> searchFiltered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOption.LAST_UPDATED -> searchFiltered.sortedByDescending { it.lastModified }
            SortOption.MOST_CONTACTED -> searchFiltered.sortedByDescending { it.timesContacted }
        }
    }

    private fun matchesSearch(contact: Contact, terms: List<String>): Boolean {
        val searchableText = buildString {
            append(contact.name).append(" ")
            append(contact.phone).append(" ")
            append(contact.email).append(" ")
            append(contact.company).append(" ")
            append(contact.address).append(" ")
            append(contact.note).append(" ")
            contact.phones.forEach { append(it.value).append(" ") }
            contact.emails.forEach { append(it.value).append(" ") }
        }.lowercase()

        return terms.all { term -> searchableText.contains(term) }
    }
}

enum class SortOption(val displayName: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    LAST_UPDATED("Recently Updated"),
    MOST_CONTACTED("Frequently Contacted")
}
