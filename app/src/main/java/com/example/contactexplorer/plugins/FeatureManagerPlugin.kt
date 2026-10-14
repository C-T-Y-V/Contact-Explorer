package com.example.contactexplorer.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class FeatureManagerPlugin(
    private val registeredFeatures: List<FeatureDescriptor>,
    private val isEnabled: (String) -> Boolean,
    private val onToggle: (String, Boolean) -> Unit,
) : ExplorerPlugin {
    override val id = "feature-manager"
    private val feature = FeatureDescriptor(id, "Feature Manager", "Manage the built-in Contact Explorer features.", FeatureCategory.CORE, required = true)
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) {
            screen(ScreenContribution(id, id, "Feature Manager", Icons.Default.Extension) {
                FeatureManagerScreen(registeredFeatures + feature, isEnabled, onToggle)
            })
        }
    }
}

@Composable
private fun FeatureManagerScreen(
    features: List<FeatureDescriptor>,
    isEnabled: (String) -> Boolean,
    onToggle: (String, Boolean) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Feature Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Built-in features are registered through the feature API. Core contact access and storage stay available.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(features.sortedWith(compareBy({ it.category.ordinal }, { it.name.lowercase() })), key = FeatureDescriptor::id) { feature ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(feature.name, fontWeight = FontWeight.SemiBold)
                            Text(feature.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(feature.category.name.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            if (!feature.available) Text("Planned; not installed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Checkbox(
                            checked = feature.available && (isEnabled(feature.id) || feature.required),
                            enabled = feature.available && !feature.required,
                            onCheckedChange = { onToggle(feature.id, it) },
                        )
                    }
                }
            }
        }
    }
}
