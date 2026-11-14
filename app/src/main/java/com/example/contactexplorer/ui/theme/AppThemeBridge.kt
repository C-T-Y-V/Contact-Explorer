/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.example.contactexplorer.ThemeMode
import com.example.contactexplorer.ThemePreset
import com.example.contactexplorer.feature.appearance.ContactExplorerTheme as FeatureTheme
import com.example.contactexplorer.feature.appearance.ThemeEngine

/**
 * Clean architecture bridge connecting the 5 CTYV Theme Presets in :feature:appearance to :app.
 * Presets:
 * 1. CTYV Obsidian (Obsidian Neon)
 * 2. CTYV Midnight Emerald
 * 3. CTYV Royal Amethyst (Cyber Violet)
 * 4. CTYV Titanium Slate (Clean Light)
 * 5. CTYV Solar Amber
 */
object AppThemeBridge {
    fun colorSchemeFor(preset: ThemePreset): ColorScheme = ThemeEngine.colorSchemeFor(preset.toThemeMode())
    fun colorSchemeFor(mode: ThemeMode): ColorScheme = ThemeEngine.colorSchemeFor(mode)
}

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.OBSIDIAN_NEON,
    content: @Composable () -> Unit
) {
    FeatureTheme(themeMode = themeMode, content = content)
}
