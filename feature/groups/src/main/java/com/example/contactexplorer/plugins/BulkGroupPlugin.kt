package com.example.contactexplorer.plugins

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder

class BulkGroupPlugin(private val openGroupPicker: (List<String>) -> Unit) : ExplorerPlugin {
    override val id = "groups"
    private val feature = FeatureDescriptor(id, "Move to Group", "Organize selected contacts into groups.", FeatureCategory.ORGANIZATION)
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) {
            selectionAction(SelectionActionContribution(
                id = "$id.move", featureId = id, label = "Move to group", icon = Icons.Default.Folder,
                invoke = openGroupPicker,
            ))
        }
    }
}
