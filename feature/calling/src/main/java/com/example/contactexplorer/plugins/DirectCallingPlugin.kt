package com.example.contactexplorer.plugins

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import com.example.contactexplorer.data.Contact

class DirectCallingPlugin(private val requestCall: (Contact) -> Unit) : ExplorerPlugin {
    override val id = "direct-calling"
    private val feature = FeatureDescriptor(id, "Calling", "Call contacts from the contact list.", FeatureCategory.CONTACTS)
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) {
            contactAction(ContactActionContribution(
                id = "$id.call", featureId = id, label = "Call", icon = Icons.Default.Call,
                visible = { it.phone.isNotBlank() }, invoke = requestCall,
            ))
        }
    }
}
