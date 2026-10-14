package com.example.contactexplorer.plugins

/** Registered as an unavailable extension point. This feature intentionally contributes no behavior. */
class AiFeaturePlugin : ExplorerPlugin {
    override val id = "ai"
    private val feature = FeatureDescriptor(
        id = id,
        name = "AI",
        description = "Reserved extension point. No AI behavior or models are active.",
        category = FeatureCategory.INSIGHTS,
        enabledByDefault = false,
        available = false,
    )
    override val features = listOf(feature)

    override fun register(registry: ExplorerPluginRegistry.Builder) {
        registry.feature(feature) { }
    }
}
