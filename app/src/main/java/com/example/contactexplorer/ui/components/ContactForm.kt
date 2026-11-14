/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.components

import androidx.compose.runtime.Composable
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactDraft

/**
 * Defensive contact creation and edit form dialog with field validation.
 */
@Composable
fun ContactForm(
    contact: Contact?,
    onDismiss: () -> Unit,
    onSave: (ContactDraft) -> Unit
) {
    ContactFormDialog(
        contact = contact,
        onDismiss = onDismiss,
        onSave = onSave
    )
}
