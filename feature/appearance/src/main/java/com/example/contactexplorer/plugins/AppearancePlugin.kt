package com.example.contactexplorer.plugins

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import com.example.contactexplorer.ThemeMode

class AppearancePlugin(private val currentMode: ThemeMode, private val selectMode: () -> Unit) : ExplorerPlugin {
    override val id = "appearance"
    private val feature = FeatureDescriptor(id, "Appearance", "Choose light, dark, or device theme.", FeatureCategory.APPEARANCE)
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) {
            toolbarAction(ToolbarActionContribution(
                id = "$id.toggle", featureId = id,
                label = if (currentMode == ThemeMode.DARK) "Light theme" else "Dark theme",
                icon = if (currentMode == ThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                invoke = selectMode,
            ))
        }
    }
}
