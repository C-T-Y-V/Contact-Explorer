package com.example.contactexplorer

enum class ThemeMode(
    val title: String,
    val subtitle: String,
    val accentHex: Long,
    val isDark: Boolean
) {
    OBSIDIAN_NEON(
        title = "Obsidian Neon",
        subtitle = "CTYV Signature dark obsidian with vibrant neon cyan & amber accents",
        accentHex = 0xFF00D2FF,
        isDark = true
    ),
    MIDNIGHT_EMERALD(
        title = "Midnight Emerald",
        subtitle = "Deep forest obsidian with cyber emerald & mint highlights",
        accentHex = 0xFF10B981,
        isDark = true
    ),
    SOLAR_AMBER(
        title = "Solar Amber",
        subtitle = "Dark carbon with warm golden amber & sunset honey glow",
        accentHex = 0xFFF59E0B,
        isDark = true
    ),
    CYBER_VIOLET(
        title = "Cyber Violet",
        subtitle = "Deep space violet with electric amethyst & neon magenta aura",
        accentHex = 0xFFA855F7,
        isDark = true
    ),
    CLEAN_LIGHT(
        title = "Clean Light",
        subtitle = "Crisp daylight aesthetic with high-contrast ocean slate",
        accentHex = 0xFF007A99,
        isDark = false
    );

    companion object {
        val DARK: ThemeMode = OBSIDIAN_NEON
        val LIGHT: ThemeMode = CLEAN_LIGHT
        val SYSTEM: ThemeMode = OBSIDIAN_NEON

        fun fromString(name: String?): ThemeMode {
            return when (name) {
                "DARK" -> OBSIDIAN_NEON
                "LIGHT" -> CLEAN_LIGHT
                "SYSTEM" -> OBSIDIAN_NEON
                else -> entries.firstOrNull { it.name == name } ?: OBSIDIAN_NEON
            }
        }
    }

    fun toThemePreset(): ThemePreset = ThemePreset.fromThemeMode(this)
}

enum class ThemePreset {
    OBSIDIAN,
    EMERALD,
    AMBER,
    VIOLET,
    CLEAN_LIGHT;

    val title: String get() = toThemeMode().title
    val subtitle: String get() = toThemeMode().subtitle
    val accentHex: Long get() = toThemeMode().accentHex
    val isDark: Boolean get() = toThemeMode().isDark

    fun toThemeMode(): ThemeMode = when (this) {
        OBSIDIAN -> ThemeMode.OBSIDIAN_NEON
        EMERALD -> ThemeMode.MIDNIGHT_EMERALD
        AMBER -> ThemeMode.SOLAR_AMBER
        VIOLET -> ThemeMode.CYBER_VIOLET
        CLEAN_LIGHT -> ThemeMode.CLEAN_LIGHT
    }

    companion object {
        val LIGHT: ThemePreset = CLEAN_LIGHT

        fun fromThemeMode(mode: ThemeMode): ThemePreset = when (mode) {
            ThemeMode.OBSIDIAN_NEON -> OBSIDIAN
            ThemeMode.MIDNIGHT_EMERALD -> EMERALD
            ThemeMode.SOLAR_AMBER -> AMBER
            ThemeMode.CYBER_VIOLET -> VIOLET
            ThemeMode.CLEAN_LIGHT -> CLEAN_LIGHT
        }
    }
}

enum class FilterMode(val displayName: String) {
    ALL("All Contacts"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
    GROUPS("Groups")
}
