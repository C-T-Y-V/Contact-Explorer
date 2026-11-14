/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.core.contacts

import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactTransfer
import com.example.contactexplorer.data.LabeledValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactTransferTest {

    @Test
    fun toCsv_generatesValidCsvHeaderAndRows() {
        val contacts = listOf(
            Contact(
                id = "1",
                lookupKey = "k1",
                name = "Alice Smith",
                phone = "+1-555-0101",
                email = "alice@example.com",
                company = "Acme Corp"
            )
        )

        val csv = ContactTransfer.toCsv(contacts)
        assertTrue(csv.startsWith("name,phone,email,company,address,notes"))
        assertTrue(csv.contains("Alice Smith"))
        assertTrue(csv.contains("+1-555-0101"))
        assertTrue(csv.contains("alice@example.com"))
    }

    @Test
    fun fromCsv_parsesValidCsvRows() {
        val csv = """
            name,phone,email,company,address,notes
            Bob Jones,+1-555-0202,bob@example.com,Beta Inc,123 Main St,Important client
        """.trimIndent()

        val parsed = ContactTransfer.fromCsv(csv)
        assertEquals(1, parsed.size)
        val bob = parsed.first()
        assertEquals("Bob Jones", bob.name)
        assertEquals("+1-555-0202", bob.phone)
        assertEquals("bob@example.com", bob.email)
        assertEquals("Beta Inc", bob.company)
    }

    @Test
    fun fromVcf_parsesStandardVCardV3() {
        val vcf = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Charlie Brown
            TEL;TYPE=CELL:+1-555-0303
            EMAIL;TYPE=WORK:charlie@corp.com
            ORG:Acme Corp
            NOTE:Key stakeholder
            END:VCARD
        """.trimIndent()

        val parsed = ContactTransfer.fromVcf(vcf)
        assertEquals(1, parsed.size)
        val charlie = parsed.first()
        assertEquals("Charlie Brown", charlie.name)
        assertEquals("+1-555-0303", charlie.phone)
        assertEquals("charlie@corp.com", charlie.email)
        assertEquals("Acme Corp", charlie.company)
    }
}
