package com.example.contactexplorer.plugins

import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.data.SearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KotlinSearchEngineTest {

    private lateinit var searchEngine: KotlinSearchEngine

    private val sampleContacts = listOf(
        Contact(
            id = "1",
            lookupKey = "lk1",
            name = "Priyanshu Sharma",
            phone = "+91 98765 43210",
            email = "priyanshu@ctyv.org",
            company = "CTYV Enterprise",
            address = "Tech Hub, Silicon City",
            note = "Lead Architect and Founder"
        ),
        Contact(
            id = "2",
            lookupKey = "lk2",
            name = "Alice Johnson",
            phone = "123-456-7890",
            email = "alice@example.com",
            company = "Tech Corp",
            address = "123 Main St, New York",
            note = "Project Manager"
        ),
        Contact(
            id = "3",
            lookupKey = "lk3",
            name = "Bob Smith",
            phone = "555-0199",
            email = "bob@smith.io",
            company = "Design Labs",
            address = "456 Market St, San Francisco",
            note = "UI Designer"
        ),
        Contact(
            id = "4",
            lookupKey = "lk4",
            name = "Charlie Brown",
            phone = "555-0200",
            email = "charlie@peanuts.com",
            company = "Acme Inc",
            address = "789 Pine St, Boston",
            phones = listOf(LabeledValue("999-888-7777", "Work", 1)),
            note = "DevOps Specialist"
        )
    )

    @Before
    fun setUp() {
        searchEngine = KotlinSearchEngine()
    }

    private fun doSearch(query: String): List<Contact> {
        return searchEngine.search(SearchRequest(query = query, contacts = sampleContacts))
    }

    @Test
    fun testEmptyQueryReturnsAllContacts() {
        val result = doSearch("")
        assertEquals(sampleContacts.size, result.size)
    }

    @Test
    fun testSearchByNameExactAndPrefix() {
        val result = doSearch("Priyanshu")
        assertEquals(1, result.size)
        assertEquals("Priyanshu Sharma", result.first().name)

        val prefixResult = doSearch("pri")
        assertTrue(prefixResult.any { it.name == "Priyanshu Sharma" })
    }

    @Test
    fun testSearchByPhoneDigits() {
        val result = doSearch("9876543210")
        assertEquals(1, result.size)
        assertEquals("Priyanshu Sharma", result.first().name)

        val partialPhone = doSearch("43210")
        assertTrue(partialPhone.any { it.name == "Priyanshu Sharma" })
    }

    @Test
    fun testSearchByCompany() {
        val result = doSearch("CTYV")
        assertEquals(1, result.size)
        assertEquals("Priyanshu Sharma", result.first().name)
    }

    @Test
    fun testSearchByEmail() {
        val result = doSearch("smith.io")
        assertEquals(1, result.size)
        assertEquals("Bob Smith", result.first().name)
    }

    @Test
    fun testSearchCaseInsensitivity() {
        val upper = doSearch("ALICE")
        val lower = doSearch("alice")
        assertEquals(upper.size, lower.size)
        assertEquals("Alice Johnson", upper.first().name)
    }

    @Test
    fun testMultiTokenSearch() {
        val result = doSearch("Charlie Acme")
        assertEquals(1, result.size)
        assertEquals("Charlie Brown", result.first().name)
    }

    @Test
    fun testAlphanumericPhoneDigitExtraction() {
        // Query containing text + phone digits "12345" matches Alice Johnson ("123-456-7890")
        val result = doSearch("Call12345")
        assertEquals(1, result.size)
        assertEquals("Alice Johnson", result.first().name)
    }

    @Test
    fun testNonExistentSearchReturnsEmpty() {
        val result = doSearch("zzzyyyxxxwww")
        assertTrue(result.isEmpty())
    }
}
