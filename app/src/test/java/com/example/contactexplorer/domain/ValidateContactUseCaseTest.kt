/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.domain

import com.example.contactexplorer.data.ContactDraft
import com.example.contactexplorer.data.LabeledValue
import com.example.contactexplorer.domain.usecase.ValidateContactUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateContactUseCaseTest {

    private lateinit var useCase: ValidateContactUseCase

    @Before
    fun setUp() {
        useCase = ValidateContactUseCase()
    }

    @Test
    fun validate_returnsInvalid_whenNameBlank() {
        val draft = ContactDraft(name = "   ")
        val result = useCase(draft)
        assertTrue(result is ValidateContactUseCase.ValidationResult.Invalid)
        assertEquals("Contact display name cannot be blank.", (result as ValidateContactUseCase.ValidationResult.Invalid).reason)
    }

    @Test
    fun validate_returnsInvalid_whenNoPhoneOrEmail() {
        val draft = ContactDraft(name = "John Doe", phones = emptyList(), emails = emptyList())
        val result = useCase(draft)
        assertTrue(result is ValidateContactUseCase.ValidationResult.Invalid)
    }

    @Test
    fun validate_returnsInvalid_whenPhoneTooShort() {
        val draft = ContactDraft(
            name = "John Doe",
            phones = listOf(LabeledValue(value = "12", label = "Mobile"))
        )
        val result = useCase(draft)
        assertTrue(result is ValidateContactUseCase.ValidationResult.Invalid)
    }

    @Test
    fun validate_returnsInvalid_whenEmailMalformed() {
        val draft = ContactDraft(
            name = "John Doe",
            phones = listOf(LabeledValue(value = "+1-555-1234", label = "Mobile")),
            emails = listOf(LabeledValue(value = "invalid-email", label = "Work"))
        )
        val result = useCase(draft)
        assertTrue(result is ValidateContactUseCase.ValidationResult.Invalid)
    }

    @Test
    fun validate_returnsValid_whenFieldsAreCorrect() {
        val draft = ContactDraft(
            name = "John Doe",
            phones = listOf(LabeledValue(value = "+1-555-1234", label = "Mobile")),
            emails = listOf(LabeledValue(value = "john.doe@company.com", label = "Work"))
        )
        val result = useCase(draft)
        assertEquals(ValidateContactUseCase.ValidationResult.Valid, result)
    }
}
