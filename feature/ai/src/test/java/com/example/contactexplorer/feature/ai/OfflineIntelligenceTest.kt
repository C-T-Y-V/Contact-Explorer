/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.feature.ai

import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.data.OfflineIntelligence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineIntelligenceTest {

    @Test
    fun normalizePhone_stripsNonDigitsAndKeeps10Digits() {
        assertEquals("9876543210", OfflineIntelligence.normalizePhone("+1 (987) 654-3210"))
        assertEquals("12345", OfflineIntelligence.normalizePhone("12-345"))
    }

    @Test
    fun findDuplicateGroups_detectsSamePhoneNumber() {
        val contacts = listOf(
            Contact(
                id = "1",
                lookupKey = "k1",
                name = "Alice Smith",
                phone = "555-123-4567"
            ),
            Contact(
                id = "2",
                lookupKey = "k2",
                name = "Alice S.",
                phone = "(555) 123-4567"
            ),
            Contact(
                id = "3",
                lookupKey = "k3",
                name = "Bob Jones",
                phone = "555-999-8888"
            )
        )

        val duplicates = OfflineIntelligence.findDuplicateGroups(contacts)
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates.first().contacts.size)
        assertTrue(duplicates.first().reason.contains("phone", ignoreCase = true))
    }

    @Test
    fun findDuplicateGroups_detectsSameEmail() {
        val contacts = listOf(
            Contact(
                id = "1",
                lookupKey = "k1",
                name = "Charlie Brown",
                email = "charlie@enterprise.com"
            ),
            Contact(
                id = "2",
                lookupKey = "k2",
                name = "C. Brown",
                email = "CHARLIE@enterprise.com"
            )
        )

        val duplicates = OfflineIntelligence.findDuplicateGroups(contacts)
        assertEquals(1, duplicates.size)
        assertTrue(duplicates.first().reason.contains("email", ignoreCase = true))
    }
}
