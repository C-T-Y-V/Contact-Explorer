/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.theme

import androidx.compose.runtime.Composable
import com.example.contactexplorer.ThemeMode
import com.example.contactexplorer.feature.appearance.ContactExplorerTheme as EngineTheme

@Composable
fun ContactExplorerTheme(
    themeMode: ThemeMode = ThemeMode.OBSIDIAN_NEON,
    content: @Composable () -> Unit
) {
    EngineTheme(themeMode = themeMode, content = content)
}
