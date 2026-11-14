package com.example.contactexplorer.plugins

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import com.example.contactexplorer.ThemeMode

class AppearancePlugin(
    private val currentMode: ThemeMode,
    private val cycleTheme: () -> Unit
) : ExplorerPlugin {
    override val id = "appearance"
    private val feature = FeatureDescriptor(
        id,
        "Appearance",
        "Multi-Preset Theme Sets: Switch seamlessly between 5 presets in real-time.",
        FeatureCategory.APPEARANCE
    )
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) {
            toolbarAction(
                ToolbarActionContribution(
                    id = "$id.cycle",
                    featureId = id,
                    label = "Theme: ${currentMode.title}",
                    icon = Icons.Default.Palette,
                    invoke = cycleTheme
                )
            )
        }
    }
}
