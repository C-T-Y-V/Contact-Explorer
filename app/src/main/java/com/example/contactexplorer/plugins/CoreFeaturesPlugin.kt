package com.example.contactexplorer.plugins

class CoreServicesPlugin : ExplorerPlugin {
    override val id = "core-services"
    override val features = listOf(
        FeatureDescriptor("core.contacts", "Contacts", "Android Contacts Provider access and contact operations.", FeatureCategory.CORE, required = true),
        FeatureDescriptor("core.database", "Database", "Local Room storage and schema migrations.", FeatureCategory.CORE, required = true),
        FeatureDescriptor("core.permissions", "Permissions", "Runtime access checks for contacts and calling.", FeatureCategory.CORE, required = true),
        FeatureDescriptor("core.api", "Core API", "Shared contracts used by the app and built-in features.", FeatureCategory.CORE, required = true),
    )

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        features.forEach { descriptor -> registry.feature(descriptor) {} }
    }
}

class NavigationFeaturesPlugin : ExplorerPlugin {
    override val id = "navigation-features"
    override val features = listOf(
        FeatureDescriptor("favorites", "Favorites", "Keep selected contacts easy to reach.", FeatureCategory.ORGANIZATION),
        FeatureDescriptor("recent-contacts", "Recent", "Revisit contacts you opened recently.", FeatureCategory.ORGANIZATION),
        FeatureDescriptor("contact-groups", "Contact Groups", "Browse and manage contact groups.", FeatureCategory.ORGANIZATION),
    )

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        features.forEach { descriptor -> registry.feature(descriptor) {} }
    }
}
