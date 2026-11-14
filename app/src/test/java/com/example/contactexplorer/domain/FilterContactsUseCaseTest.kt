/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.domain

import com.example.contactexplorer.FilterMode
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactGroupMembership
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.domain.usecase.FilterContactsUseCase
import com.example.contactexplorer.domain.usecase.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FilterContactsUseCaseTest {

    private lateinit var useCase: FilterContactsUseCase
    private lateinit var sampleContacts: List<Contact>

    @Before
    fun setUp() {
        useCase = FilterContactsUseCase()
        sampleContacts = listOf(
            Contact(
                id = "1",
                lookupKey = "key1",
                name = "Alice Smith",
                phones = listOf(LabeledValue(value = "+1-555-0101", label = "Mobile")),
                emails = listOf(LabeledValue(value = "alice@enterprise.com", label = "Work")),
                company = "Acme Corp",
                timesContacted = 10,
                lastModified = 1000L
            ),
            Contact(
                id = "2",
                lookupKey = "key2",
                name = "Bob Jones",
                phones = listOf(LabeledValue(value = "+1-555-0202", label = "Mobile")),
                emails = listOf(LabeledValue(value = "bob@startup.io", label = "Work")),
                company = "Beta Inc",
                timesContacted = 2,
                lastModified = 2000L
            ),
            Contact(
                id = "3",
                lookupKey = "key3",
                name = "Charlie Brown",
                phones = listOf(LabeledValue(value = "+1-555-0303", label = "Mobile")),
                emails = emptyList(),
                company = "Acme Corp",
                timesContacted = 50,
                lastModified = 500L
            )
        )
    }

    @Test
    fun filter_returnsAllContacts_whenQueryBlankAndFilterAll() {
        val result = useCase(contacts = sampleContacts, query = "", filterMode = FilterMode.ALL)
        assertEquals(3, result.size)
    }

    @Test
    fun filter_returnsMatchingContacts_whenQueryMatchesName() {
        val result = useCase(contacts = sampleContacts, query = "alice")
        assertEquals(1, result.size)
        assertEquals("Alice Smith", result.first().name)
    }

    @Test
    fun filter_returnsMatchingContacts_whenQueryMatchesCompany() {
        val result = useCase(contacts = sampleContacts, query = "Acme")
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "3" })
    }

    @Test
    fun filter_filtersByFavorites() {
        val favorites = setOf("2")
        val result = useCase(
            contacts = sampleContacts,
            filterMode = FilterMode.FAVORITES,
            favorites = favorites
        )
        assertEquals(1, result.size)
        assertEquals("Bob Jones", result.first().name)
    }

    @Test
    fun filter_filtersByGroupMembership() {
        val memberships = listOf(
            ContactGroupMembership(contactId = "1", groupId = 101L),
            ContactGroupMembership(contactId = "2", groupId = 102L)
        )
        val result = useCase(
            contacts = sampleContacts,
            filterMode = FilterMode.GROUPS,
            memberships = memberships,
            selectedGroupId = 101L
        )
        assertEquals(1, result.size)
        assertEquals("Alice Smith", result.first().name)
    }

    @Test
    fun filter_sortsByMostContacted() {
        val result = useCase(
            contacts = sampleContacts,
            sortBy = SortOption.MOST_CONTACTED
        )
        assertEquals("Charlie Brown", result[0].name)
        assertEquals("Alice Smith", result[1].name)
        assertEquals("Bob Jones", result[2].name)
    }
}
