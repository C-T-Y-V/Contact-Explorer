package com.example.contactexplorer.plugins

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.DuplicateGroup
import com.example.contactexplorer.data.KeywordCluster
import com.example.contactexplorer.data.OfflineIntelligence

class AiFeaturePlugin(
    private val onAutoGroupTrigger: (() -> Unit)? = null
) : ExplorerPlugin {
    override val id = "ai"
    private val feature = FeatureDescriptor(
        id = id,
        name = "Offline Contact Intelligence",
        description = "On-device duplicate detection, phone clustering, and heuristic auto-grouping.",
        category = FeatureCategory.INSIGHTS,
        enabledByDefault = true,
        available = true,
    )
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) {
            onAutoGroupTrigger?.let { trigger ->
                toolbarAction(
                    ToolbarActionContribution(
                        id = "$id.autogroup",
                        featureId = id,
                        label = "Auto Group (AI)",
                        icon = Icons.Default.AutoAwesome,
                        invoke = trigger,
                    )
                )
            }
        }
    }

    fun findDuplicates(contacts: List<Contact>): List<List<Contact>> {
        return OfflineIntelligence.duplicateGroups(contacts)
    }

    fun findDuplicateGroups(contacts: List<Contact>): List<DuplicateGroup> {
        return OfflineIntelligence.findDuplicateGroups(contacts)
    }

    fun clusterContactsByKeyword(contacts: List<Contact>): List<KeywordCluster> {
        return OfflineIntelligence.detectKeywordClusters(contacts)
    }
}
