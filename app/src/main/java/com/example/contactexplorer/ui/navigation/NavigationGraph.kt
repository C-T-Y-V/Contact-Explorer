/*
 * Contact Explorer Enterprise Android
 * A CTYV PRODUCT
 * Copyright © 2026 Priyanshu Sharma (CTYV) — A CTYV Product. All rights reserved.
 * Distributed under the MIT License (with Attribution).
 */

package com.example.contactexplorer.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.contactexplorer.ExplorerViewModel
import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.plugins.AiFeaturePlugin
import com.example.contactexplorer.plugins.AppearancePlugin
import com.example.contactexplorer.plugins.BuiltInFeatureScreens
import com.example.contactexplorer.plugins.BulkGroupPlugin
import com.example.contactexplorer.plugins.CoreServicesPlugin
import com.example.contactexplorer.plugins.ExplorerPluginRegistry
import com.example.contactexplorer.plugins.FeatureDescriptor
import com.example.contactexplorer.plugins.FeatureManagerPlugin
import com.example.contactexplorer.plugins.KotlinSearchEngine
import com.example.contactexplorer.plugins.NavigationFeaturesPlugin
import com.example.contactexplorer.plugins.SearchFeaturePlugin
import com.example.contactexplorer.ui.components.ContactFormDialog
import com.example.contactexplorer.ui.components.EmptyState
import com.example.contactexplorer.ui.screens.ContactDetailsScreen
import com.example.contactexplorer.ui.screens.ContactListScreen
import com.example.contactexplorer.ui.screens.ExportVaultScreen
import com.example.contactexplorer.ui.screens.SettingsScreen

object Destinations {
    const val CONTACT_LIST = "contact_list"
    const val CONTACT_DETAILS = "contact_details/{contactId}"
    const val EXPORT_VAULT = "export_vault"
    const val SETTINGS = "settings"
    const val FEATURE = "feature/{featureId}"

    fun contactDetails(contactId: String) = "contact_details/$contactId"
    fun feature(featureId: String) = "feature/$featureId"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactExplorerNavHost(
    vm: ExplorerViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    var editContactId by rememberSaveable { mutableStateOf<String?>(null) }
    var showContactForm by rememberSaveable { mutableStateOf(false) }
    val editContact = editContactId?.let { id -> vm.contacts.find { it.id == id } }

    val builtInPlugins = remember(vm, vm.themeMode) {
        listOf(
            CoreServicesPlugin(),
            NavigationFeaturesPlugin(),
            BulkGroupPlugin { /* handled in ContactListScreen */ },
            AppearancePlugin(vm.themeMode) { vm.cycleThemeMode() },
            SearchFeaturePlugin(KotlinSearchEngine()),
            AiFeaturePlugin(),
            BuiltInFeatureScreens(
                vm = vm,
                onAddTagToContacts = { /* tag management */ },
                onAssignTag = vm::addTagToContacts,
                onImportCsv = { navController.navigate(Destinations.EXPORT_VAULT) },
                onImportVcf = { navController.navigate(Destinations.EXPORT_VAULT) },
                onExportCsv = { navController.navigate(Destinations.EXPORT_VAULT) },
                onExportVcf = { navController.navigate(Destinations.EXPORT_VAULT) },
                onDeleteContacts = { ids -> vm.deleteContacts(ids) },
                onShareContacts = { /* share handled in screen */ },
                onOpenExportDialog = { navController.navigate(Destinations.EXPORT_VAULT) },
                onOpenKeepDataDialog = { navController.navigate(Destinations.EXPORT_VAULT) },
            ),
        )
    }

    val allFeatures = remember(builtInPlugins) {
        builtInPlugins.flatMap { it.features }
    }

    LaunchedEffect(allFeatures) {
        vm.initFeatures(allFeatures)
    }

    val managerPlugin = remember(allFeatures, vm.enabledFeatureIds) {
        FeatureManagerPlugin(
            registeredFeatures = allFeatures,
            isEnabled = { it in vm.enabledFeatureIds },
            onToggle = vm::toggleFeature,
        )
    }

    val featureRegistry = remember(builtInPlugins, managerPlugin, vm.enabledFeatureIds) {
        ExplorerPluginRegistry.install(builtInPlugins + managerPlugin).enabled(vm.enabledFeatureIds)
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.CONTACT_LIST,
        modifier = modifier
    ) {
        composable(Destinations.CONTACT_LIST) {
            ContactListScreen(
                vm = vm,
                activeFeatureIds = vm.enabledFeatureIds,
                registeredFeatures = allFeatures,
                onNavigateToDetails = { contactId ->
                    navController.navigate(Destinations.contactDetails(contactId))
                },
                onNavigateToExport = {
                    navController.navigate(Destinations.EXPORT_VAULT)
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
                onNavigateToFeature = { featureId ->
                    navController.navigate(Destinations.feature(featureId))
                }
            )
        }

        composable(
            route = Destinations.CONTACT_DETAILS,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId").orEmpty()
            ContactDetailsScreen(
                contactId = contactId,
                vm = vm,
                onBack = { navController.popBackStack() },
                onEditContact = { contact ->
                    editContactId = contact.id
                    showContactForm = true
                }
            )
        }

        composable(Destinations.EXPORT_VAULT) {
            ExportVaultScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                vm = vm,
                registeredFeatures = allFeatures,
                activeFeatureIds = vm.enabledFeatureIds,
                onToggleFeature = vm::toggleFeature,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destinations.FEATURE,
            arguments = listOf(navArgument("featureId") { type = NavType.StringType })
        ) { backStackEntry ->
            val featureId = backStackEntry.arguments?.getString("featureId").orEmpty()
            val screen = featureRegistry.screens.firstOrNull { it.id == featureId }
            if (screen != null) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(screen.label, fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        screen.content()
                    }
                }
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Feature", fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        EmptyState(
                            title = "Feature not found",
                            message = "The selected feature is not enabled or not installed.",
                            action = { navController.popBackStack() },
                            actionLabel = "Go back"
                        )
                    }
                }
            }
        }
    }

    if (showContactForm) {
        ContactFormDialog(
            contact = editContact,
            onDismiss = { showContactForm = false; editContactId = null },
            onSave = { draft ->
                vm.save(editContactId, draft)
                showContactForm = false
                editContactId = null
            }
        )
    }
}
