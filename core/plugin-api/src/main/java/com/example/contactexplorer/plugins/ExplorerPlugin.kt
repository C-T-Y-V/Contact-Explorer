package com.example.contactexplorer.plugins

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.contactexplorer.data.Contact
import kotlin.reflect.KClass

enum class FeatureCategory { CORE, CONTACTS, ORGANIZATION, SEARCH, INSIGHTS, GRAPH, DATA, APPEARANCE }

data class FeatureDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val category: FeatureCategory,
    val enabledByDefault: Boolean = true,
    val required: Boolean = false,
    val available: Boolean = true,
)

data class ContactActionContribution(
    val id: String,
    val featureId: String,
    val label: String,
    val icon: ImageVector,
    val visible: (Contact) -> Boolean = { true },
    val invoke: (Contact) -> Unit,
)

data class SelectionActionContribution(
    val id: String,
    val featureId: String,
    val label: String,
    val icon: ImageVector,
    val invoke: (List<String>) -> Unit,
)

data class ToolbarActionContribution(
    val id: String,
    val featureId: String,
    val label: String,
    val icon: ImageVector,
    val invoke: () -> Unit,
)

data class ScreenContribution(
    val id: String,
    val featureId: String,
    val label: String,
    val icon: ImageVector,
    val content: @Composable () -> Unit,
)

data class ServiceContribution(
    val id: String,
    val featureId: String,
    val type: KClass<*>,
    val service: Any,
)

