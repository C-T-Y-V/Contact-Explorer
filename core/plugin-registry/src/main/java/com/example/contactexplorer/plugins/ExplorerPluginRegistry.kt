package com.example.contactexplorer.plugins

import kotlin.reflect.KClass

interface ExplorerPlugin {
    val id: String
    val features: List<FeatureDescriptor>
    fun register(registry: ExplorerPluginRegistry.Builder)
}

class ExplorerPluginRegistry private constructor(
    val features: List<FeatureDescriptor>,
    val contactActions: List<ContactActionContribution>,
    val selectionActions: List<SelectionActionContribution>,
    val toolbarActions: List<ToolbarActionContribution>,
    val screens: List<ScreenContribution>,
    private val services: List<ServiceContribution>,
) {
    fun enabled(featureIds: Set<String>): ExplorerPluginRegistry {
        val active = features.filter { it.required || it.id in featureIds }.mapTo(mutableSetOf()) { it.id }
        return ExplorerPluginRegistry(
            features,
            contactActions.filter { it.featureId in active },
            selectionActions.filter { it.featureId in active },
            toolbarActions.filter { it.featureId in active },
            screens.filter { it.featureId in active },
            services.filter { it.featureId in active },
        )
    }

    fun <T : Any> service(id: String, type: KClass<T>): T? {
        val entry = services.singleOrNull { it.id == id } ?: return null
        require(type == entry.type) { "Service '$id' was registered as ${entry.type.qualifiedName}." }
        return type.java.cast(entry.service)
    }

    class Builder {
        private val features = mutableListOf<FeatureDescriptor>()
        private val contactActions = mutableListOf<ContactActionContribution>()
        private val selectionActions = mutableListOf<SelectionActionContribution>()
        private val toolbarActions = mutableListOf<ToolbarActionContribution>()
        private val screens = mutableListOf<ScreenContribution>()
        private val services = mutableListOf<ServiceContribution>()
        private val featureIds = mutableSetOf<String>()

        fun feature(descriptor: FeatureDescriptor, contributions: FeatureBuilder.() -> Unit) {
            require(featureIds.add(descriptor.id)) { "Feature IDs must be unique: ${descriptor.id}" }
            require(descriptor.id.isNotBlank()) { "Feature ID cannot be blank." }
            features += descriptor
            FeatureBuilder(descriptor.id).contributions()
        }

        inner class FeatureBuilder internal constructor(private val featureId: String) {
            fun contactAction(action: ContactActionContribution) {
                require(action.featureId == featureId) { "Action ${action.id} belongs to ${action.featureId}, not $featureId." }
                contactActions += action
            }

            fun selectionAction(action: SelectionActionContribution) {
                require(action.featureId == featureId) { "Action ${action.id} belongs to ${action.featureId}, not $featureId." }
                selectionActions += action
            }

            fun toolbarAction(action: ToolbarActionContribution) {
                require(action.featureId == featureId) { "Action ${action.id} belongs to ${action.featureId}, not $featureId." }
                toolbarActions += action
            }

            fun screen(screen: ScreenContribution) {
                require(screen.featureId == featureId) { "Screen ${screen.id} belongs to ${screen.featureId}, not $featureId." }
                screens += screen
            }

            fun <T : Any> service(id: String, type: KClass<T>, service: T) {
                services += ServiceContribution(id, featureId, type, service)
            }
        }

        internal fun build(): ExplorerPluginRegistry {
            fun <T> uniqueById(items: List<T>, id: (T) -> String, kind: String): List<T> {
                val duplicates = items.groupBy(id).filterValues { it.size > 1 }.keys
                require(duplicates.isEmpty()) { "Duplicate $kind IDs: ${duplicates.joinToString()}" }
                return items
            }
            val knownFeatureIds = features.mapTo(mutableSetOf()) { it.id }
            require((contactActions.map { it.featureId } + selectionActions.map { it.featureId } +
                toolbarActions.map { it.featureId } + screens.map { it.featureId } + services.map { it.featureId })
                .all { it in knownFeatureIds }) { "Every contribution must belong to a registered feature." }
            return ExplorerPluginRegistry(
                features.toList(),
                uniqueById(contactActions, ContactActionContribution::id, "contact action"),
                uniqueById(selectionActions, SelectionActionContribution::id, "selection action"),
                uniqueById(toolbarActions, ToolbarActionContribution::id, "toolbar action"),
                uniqueById(screens, ScreenContribution::id, "screen"),
                uniqueById(services, ServiceContribution::id, "service"),
            )
        }
    }

    companion object {
        fun install(plugins: List<ExplorerPlugin>): ExplorerPluginRegistry {
            require(plugins.map { it.id }.distinct().size == plugins.size) { "Plugin IDs must be unique." }
            require(plugins.all { it.id.isNotBlank() }) { "Plugin ID cannot be blank." }
            val declaredFeatures = plugins.flatMap { it.features }
            require(declaredFeatures.map { it.id }.distinct().size == declaredFeatures.size) { "Feature IDs must be unique." }
            val builder = Builder()
            plugins.forEach { it.register(builder) }
            val registry = builder.build()
            require(registry.features.map { it.id }.toSet() == declaredFeatures.map { it.id }.toSet()) {
                "Every declared feature must be registered exactly once."
            }
            return registry
        }
    }
}
