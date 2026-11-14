package com.example.contactexplorer

import com.example.contactexplorer.data.Contact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorerUiStateTest {

    @Test
    fun testDefaultStateValues() {
        val state = ExplorerUiState()

        assertTrue(state.contacts.isEmpty())
        assertTrue(state.groups.isEmpty())
        assertTrue(state.favorites.isEmpty())
        assertTrue(state.selectedIds.isEmpty())
        assertEquals(ThemePreset.OBSIDIAN, state.themePreset)
        assertEquals(FilterMode.ALL, state.activeFilter)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun testStateCopyWithContactsAndLoading() {
        val contacts = listOf(
            Contact(id = "c1", lookupKey = "lookup_c1", name = "Priyanshu Sharma", phone = "9876543210")
        )
        val state = ExplorerUiState().copy(
            contacts = contacts,
            isLoading = true
        )

        assertEquals(1, state.contacts.size)
        assertEquals("Priyanshu Sharma", state.contacts.first().name)
        assertTrue(state.isLoading)
    }

    @Test
    fun testSelectionAndFavoritesToggling() {
        val baseState = ExplorerUiState()

        val selectedState = baseState.copy(selectedIds = setOf("c1", "c2"))
        assertEquals(2, selectedState.selectedIds.size)
        assertTrue(selectedState.selectedIds.contains("c1"))

        val favoritesState = selectedState.copy(favorites = setOf("c1"))
        assertEquals(1, favoritesState.favorites.size)
        assertTrue(favoritesState.favorites.contains("c1"))
    }

    @Test
    fun testThemePresetAndFilterModeUpdate() {
        val state = ExplorerUiState().copy(
            themePreset = ThemePreset.EMERALD,
            activeFilter = FilterMode.FAVORITES
        )

        assertEquals(ThemePreset.EMERALD, state.themePreset)
        assertEquals(FilterMode.FAVORITES, state.activeFilter)
    }

    @Test
    fun testErrorMessagePropagation() {
        val errorState = ExplorerUiState().copy(
            errorMessage = "Failed to synchronize contact vault"
        )

        assertEquals("Failed to synchronize contact vault", errorState.errorMessage)
    }
}
