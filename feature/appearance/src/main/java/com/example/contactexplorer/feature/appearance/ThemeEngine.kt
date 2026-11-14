package com.example.contactexplorer.feature.appearance

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.contactexplorer.ThemeMode

// ============================================================================
// Contact Explorer Multi-Preset Theme Sets Engine
// A CTYV Product | Priyanshu Sharma (CTYV)
// ============================================================================

val ObsidianNeonColorScheme = darkColorScheme(
    primary = Color(0xFF00D2FF), // Neon cyan rim glow
    onPrimary = Color(0xFF030A12),
    primaryContainer = Color(0xFF0C2B4D), // Translucent electric blue
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFFFFB020), // Warm amber tab divider
    onSecondary = Color(0xFF2E1700),
    secondaryContainer = Color(0xFF4A2A04),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF38E1FF), // Ice highlight
    onTertiary = Color(0xFF001F29),
    tertiaryContainer = Color(0xFF083344),
    onTertiaryContainer = Color(0xFFB5EFFF),
    background = Color(0xFF070B12), // Deep obsidian black
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0E1626), // Midnight navy surface
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF152238),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF111C2E),
    surfaceContainerHigh = Color(0xFF17253D),
    surfaceContainerHighest = Color(0xFF1E2F4D),
    outline = Color(0xFF1E385C),
    outlineVariant = Color(0xFF122035)
)

val MidnightEmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF10B981), // Cyber emerald green
    onPrimary = Color(0xFF02170D),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF34D399), // Mint ice accent
    onSecondary = Color(0xFF022013),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF012014),
    tertiaryContainer = Color(0xFF044331),
    onTertiaryContainer = Color(0xFFE6FFFA),
    background = Color(0xFF05100B), // Deep forest obsidian
    onBackground = Color(0xFFF0FDF4),
    surface = Color(0xFF091A13),
    onSurface = Color(0xFFE6F4EA),
    surfaceVariant = Color(0xFF0F261C),
    onSurfaceVariant = Color(0xFF86EFAC),
    surfaceContainer = Color(0xFF0E241B),
    surfaceContainerHigh = Color(0xFF143024),
    surfaceContainerHighest = Color(0xFF1B3D2F),
    outline = Color(0xFF1E513D),
    outlineVariant = Color(0xFF0F3224)
)

val SolarAmberColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B), // Radiant solar amber
    onPrimary = Color(0xFF261500),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = Color(0xFFFB923C), // Sunset orange
    onSecondary = Color(0xFF2D1000),
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = Color(0xFFFFEDD5),
    tertiary = Color(0xFFFCD34D), // Honey gold
    onTertiary = Color(0xFF2E1700),
    tertiaryContainer = Color(0xFF451A03),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF100B06), // Dark roasted carbon
    onBackground = Color(0xFFFFFBEB),
    surface = Color(0xFF191209),
    onSurface = Color(0xFFF5ECE0),
    surfaceVariant = Color(0xFF231A0F),
    onSurfaceVariant = Color(0xFFFCD34D),
    surfaceContainer = Color(0xFF21190D),
    surfaceContainerHigh = Color(0xFF2B2112),
    surfaceContainerHighest = Color(0xFF372B18),
    outline = Color(0xFF5A4423),
    outlineVariant = Color(0xFF382914)
)

val CyberVioletColorScheme = darkColorScheme(
    primary = Color(0xFFA855F7), // Electric amethyst
    onPrimary = Color(0xFF1C032F),
    primaryContainer = Color(0xFF3B0764),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = Color(0xFFEC4899), // Neon magenta
    onSecondary = Color(0xFF2E0217),
    secondaryContainer = Color(0xFF700D3E),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = Color(0xFFC084FC),
    onTertiary = Color(0xFF1A002E),
    tertiaryContainer = Color(0xFF4A0E4E),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = Color(0xFF0C0816), // Deep void purple
    onBackground = Color(0xFFFAF5FF),
    surface = Color(0xFF130E22),
    onSurface = Color(0xFFF1EAFA),
    surfaceVariant = Color(0xFF1E1736),
    onSurfaceVariant = Color(0xFFD8B4FE),
    surfaceContainer = Color(0xFF1B1430),
    surfaceContainerHigh = Color(0xFF241C3E),
    surfaceContainerHighest = Color(0xFF2F2450),
    outline = Color(0xFF4C3672),
    outlineVariant = Color(0xFF2E1F47)
)

val CleanLightColorScheme = lightColorScheme(
    primary = Color(0xFF007A99), // Clean ocean slate
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0F0FD),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFFB45309), // Amber tab accent
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    background = Color(0xFFF8FAFC), // Pure daylight
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

object ThemeEngine {
    fun colorSchemeFor(mode: ThemeMode): ColorScheme = when (mode) {
        ThemeMode.OBSIDIAN_NEON -> ObsidianNeonColorScheme
        ThemeMode.MIDNIGHT_EMERALD -> MidnightEmeraldColorScheme
        ThemeMode.SOLAR_AMBER -> SolarAmberColorScheme
        ThemeMode.CYBER_VIOLET -> CyberVioletColorScheme
        ThemeMode.CLEAN_LIGHT -> CleanLightColorScheme
    }
}

@Composable
fun ContactExplorerTheme(
    themeMode: ThemeMode = ThemeMode.OBSIDIAN_NEON,
    content: @Composable () -> Unit
) {
    val colors = ThemeEngine.colorSchemeFor(themeMode)
    MaterialTheme(colorScheme = colors, content = content)
}
