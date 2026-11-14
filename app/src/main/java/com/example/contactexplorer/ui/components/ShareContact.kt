/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.components

import androidx.compose.runtime.Composable
import com.example.contactexplorer.data.Contact

/**
 * Ephemeral FileProvider URI sharesheet launcher and selective field disclosure dialog.
 */
@Composable
fun ShareContact(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onShare: (Set<String>) -> Unit
) {
    ShareContactDialog(
        contacts = contacts,
        onDismiss = onDismiss,
        onShare = onShare
    )
}
