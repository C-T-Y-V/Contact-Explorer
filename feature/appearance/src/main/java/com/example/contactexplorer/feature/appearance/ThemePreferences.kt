package com.example.contactexplorer.feature.appearance

import android.content.Context
import android.content.SharedPreferences
import com.example.contactexplorer.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.OBSIDIAN_NEON.name)
        return ThemeMode.fromString(raw)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun cycleNextTheme(): ThemeMode {
        val presets = ThemeMode.entries
        val currentIndex = presets.indexOf(_themeMode.value)
        val nextIndex = (currentIndex + 1) % presets.size
        val nextMode = presets[nextIndex]
        setThemeMode(nextMode)
        return nextMode
    }

    companion object {
        private const val PREFS_NAME = "contact_explorer_appearance_prefs"
        private const val KEY_THEME_MODE = "active_theme_mode_preset"
    }
}
