/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.domain.usecase

import com.example.contactexplorer.data.ContactDraft

/**
 * Pure Kotlin Use Case validating contact draft entries before persistence.
 * Prevents empty or malformed contact records from entering the database or ContentProvider.
 */
class ValidateContactUseCase {

    sealed interface ValidationResult {
        object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    operator fun invoke(draft: ContactDraft): ValidationResult {
        if (draft.name.isBlank()) {
            return ValidationResult.Invalid("Contact display name cannot be blank.")
        }

        val phones = draft.effectivePhones
        val emails = draft.effectiveEmails

        if (phones.isEmpty() && emails.isEmpty()) {
            return ValidationResult.Invalid("Contact must have at least one phone number or email address.")
        }

        // Validate phone numbers: must contain at least 3 digits
        val invalidPhones = phones.filter { phone ->
            val digits = phone.value.filter { it.isDigit() }
            digits.length < 3
        }
        if (invalidPhones.isNotEmpty()) {
            return ValidationResult.Invalid("Phone number '${invalidPhones.first().value}' is too short.")
        }

        // Validate emails: basic RFC structure if present
        val invalidEmails = emails.filter { email ->
            val value = email.value.trim()
            value.isNotEmpty() && (!value.contains("@") || !value.contains("."))
        }
        if (invalidEmails.isNotEmpty()) {
            return ValidationResult.Invalid("Email address '${invalidEmails.first().value}' is invalid.")
        }

        return ValidationResult.Valid
    }
}
