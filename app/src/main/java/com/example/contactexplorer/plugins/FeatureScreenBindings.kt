/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.plugins

import com.example.contactexplorer.plugins.ExplorerPluginRegistry

/**
 * Connects ExplorerPluginRegistry contributions to UI slots and navigation destinations.
 */
object FeatureScreenBindings {
    fun getRegisteredScreens(registry: ExplorerPluginRegistry) = registry.screens
}
