package com.nuvio.tv.ui.screens.nuviotr

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Surface
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.NuvioTrCatalog
import com.nuvio.tv.domain.model.NuvioTrCollection
import com.nuvio.tv.domain.model.NuvioTrPlugin
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.screens.settings.SettingsDetailHeader
import com.nuvio.tv.ui.screens.settings.SettingsGroupCard
import com.nuvio.tv.ui.screens.settings.SettingsRailButton
import com.nuvio.tv.ui.screens.settings.SettingsActionRow
import com.nuvio.tv.ui.screens.settings.SettingsWorkspaceSurface
import com.nuvio.tv.ui.screens.collection.NuvioTextField
import com.nuvio.tv.ui.components.NuvioDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class NuvioTrSection { Catalogs, Collections, Plugins }

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NuvioTrScreen(
    viewModel: NuvioTrViewModel = hiltViewModel(),
    onBackPress: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dependencyNames = (state.repositories.catalogs.associate { it.id to it.name } +
        state.repositories.plugins.associate { it.id to it.name })
    val pluginDependencies = state.repositories.plugins.associateBy { it.id }
    var section by rememberSaveable { mutableStateOf(NuvioTrSection.Catalogs) }
    var pendingCatalog by remember { mutableStateOf<NuvioTrCatalog?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var appliedSearchQuery by rememberSaveable { mutableStateOf("") }
    val repositoryNameQuery = appliedSearchQuery.trim()
    val filteredCatalogs = remember(state.repositories.catalogs, repositoryNameQuery) {
        state.repositories.catalogs.filter { catalog ->
            catalog.name.contains(repositoryNameQuery, ignoreCase = true)
        }
    }
    val filteredCollections = remember(state.repositories.collections, repositoryNameQuery) {
        state.repositories.collections.filter { collection ->
            collection.name.contains(repositoryNameQuery, ignoreCase = true)
        }
    }
    val filteredPlugins = remember(state.repositories.plugins, repositoryNameQuery) {
        state.repositories.plugins.filter { plugin ->
            plugin.name.contains(repositoryNameQuery, ignoreCase = true)
        }
    }
    LaunchedEffect(state.notification) {
        state.notification?.let {
            delay(if (state.notificationIsError) 5_000 else 3_000)
            viewModel.clearNotification()
        }
    }
    BackHandler(onBack = onBackPress)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioTheme.colors.Background)
            .padding(
                start = NuvioTheme.spacing.xxl,
                end = NuvioTheme.spacing.xxl,
                top = NuvioTheme.spacing.xl,
                bottom = NuvioTheme.spacing.xl
            )
    ) {
    SettingsWorkspaceSurface(modifier = Modifier.fillMaxSize()) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.width(260.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.nuviotr_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            NuvioTrSection.entries.forEach { item ->
                val label = stringResource(when (item) {
                        NuvioTrSection.Catalogs -> R.string.nuviotr_catalogs
                        NuvioTrSection.Collections -> R.string.nuviotr_collections
                        NuvioTrSection.Plugins -> R.string.nuviotr_plugins
                    })
                SettingsRailButton(
                    title = label,
                    isSelected = section == item,
                    onClick = { section = item },
                    icon = when (item) {
                        NuvioTrSection.Catalogs -> Icons.Default.ViewList
                        NuvioTrSection.Collections -> Icons.Default.Folder
                        NuvioTrSection.Plugins -> Icons.Default.Extension
                    }
                )
            }
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
            ) {
                SettingsDetailHeader(
                    title = stringResource(when (section) {
                        NuvioTrSection.Catalogs -> R.string.nuviotr_catalogs
                        NuvioTrSection.Collections -> R.string.nuviotr_collections
                        NuvioTrSection.Plugins -> R.string.nuviotr_plugins
                    }),
                    subtitle = stringResource(when (section) {
                        NuvioTrSection.Catalogs -> R.string.nuviotr_catalogs_subtitle
                        NuvioTrSection.Collections -> R.string.nuviotr_collections_subtitle
                        NuvioTrSection.Plugins -> R.string.nuviotr_plugins_subtitle
                    }),
                    modifier = Modifier.weight(1f)
                )
                NuvioTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.width(180.dp),
                    placeholder = stringResource(R.string.nuviotr_search_placeholder)
                )
                Button(onClick = { appliedSearchQuery = searchQuery }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.nuviotr_search)
                    )
                }
                Button(
                    onClick = viewModel::refresh,
                    enabled = !state.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.nuviotr_refresh)
                    )
                }
            }
            SettingsGroupCard(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Text(stringResource(R.string.nuviotr_loading))
                state.error != null -> Text(state.error ?: "")
                section == NuvioTrSection.Catalogs -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredCatalogs, key = { it.id }) { catalog ->
                        CatalogRow(catalog, dependencyNames, state.installedManifestUrls, state.installingId == catalog.id, { selected ->
                            pendingCatalog = selected
                        }, viewModel::removeCatalog)
                    }
                }
                section == NuvioTrSection.Collections -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredCollections, key = { it.id }) { collection ->
                        CollectionRow(
                            collection = collection,
                            installed = collection.id in state.installedCollectionIds,
                            installing = state.installingId == collection.id,
                            onInstall = viewModel::installCollection,
                            onRemove = viewModel::removeCollection
                        )
                    }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredPlugins, key = { it.id }) { plugin ->
                        PluginRowWithBadges(
                            plugin = plugin,
                            installed = plugin.manifestUrl.trimEnd('/') in state.installedPluginUrls,
                            installing = state.installingId == plugin.id,
                            providers = state.pluginProviders[plugin.id].orEmpty(),
                            onInstall = viewModel::installPlugin,
                            onLoadProviders = viewModel::loadPluginProviders,
                            onRemove = viewModel::removePlugin
                        )
                    }
                }
            }
            }
        }
    }
    }
    }
    pendingCatalog?.let { catalog ->
        val dependencies = catalog.dependencies.mapNotNull { id ->
            pluginDependencies[id]?.let { id to it.manifestUrl }
        }
        NuvioDialog(
            onDismiss = { pendingCatalog = null },
            title = stringResource(if (dependencies.isEmpty()) R.string.nuviotr_install_dialog_title else R.string.nuviotr_dependency_dialog_title),
            subtitle = if (dependencies.isEmpty()) {
                stringResource(R.string.nuviotr_install_dialog_message, catalog.name)
            } else {
                stringResource(R.string.nuviotr_dependency_dialog_message, catalog.name, dependencies.joinToString { dependencyNames[it.first] ?: it.first })
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { pendingCatalog = null }) { Text(stringResource(R.string.nuviotr_cancel)) }
                Button(onClick = { viewModel.installCatalog(catalog.id, catalog.manifestUrl, dependencies); pendingCatalog = null }) { Text(stringResource(R.string.nuviotr_confirm)) }
            }
        }
    }
    NuvioTrMessageOverlay(
        successMessage = state.notification.takeUnless { state.notificationIsError },
        errorMessage = state.notification.takeIf { state.notificationIsError }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrMessageOverlay(successMessage: String?, errorMessage: String?) {
    Box(
        modifier = Modifier.fillMaxSize().padding(NuvioTheme.spacing.xl),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = successMessage != null || errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val isSuccess = successMessage != null
            Surface(
                onClick = {},
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSuccess) Color(0xFF2E7D32).copy(alpha = 0.9f) else Color(0xFFC62828).copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = NuvioTheme.spacing.xl, vertical = NuvioTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
                ) {
                    Icon(if (isSuccess) Icons.Default.Check else Icons.Default.Close, null, tint = Color.White)
                    Text(successMessage ?: errorMessage.orEmpty(), color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PluginRowWithBadges(
    plugin: NuvioTrPlugin,
    installed: Boolean,
    installing: Boolean,
    providers: List<String>,
    onInstall: (String, String) -> Unit,
    onLoadProviders: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }
    var showRemovalConfirmation by remember { mutableStateOf(false) }
    val badges = listOfNotNull(
        if (plugin.supports.catalog) stringResource(R.string.nuviotr_support_catalog) else null,
        if (plugin.supports.stream) stringResource(R.string.nuviotr_support_stream) else null,
        if (plugin.supports.search) stringResource(R.string.nuviotr_support_search) else null,
        if (plugin.supports.comments) stringResource(R.string.nuviotr_support_comments) else null
    )
    SettingsActionRow(
        title = plugin.name,
        subtitle = listOf(plugin.authors.joinToString(), plugin.description).filter { it.isNotBlank() }.joinToString("\n"),
        value = null,
        onClick = {
            if (installed) {
                showRemovalConfirmation = true
            } else {
                onLoadProviders(plugin.id, plugin.manifestUrl)
                showConfirmation = true
            }
        },
        enabled = !installing,
        leadingIcon = Icons.Default.Extension,
        leadingImageUrl = plugin.iconUrl,
        trailingIcon = if (installed) Icons.Default.Delete else Icons.Default.Download,
        subtitleSuccessBadges = badges,
        subtitleErrorBadges = plugin.origin?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
    )
    if (showConfirmation) {
        NuvioTrInstallDialog(
            name = plugin.name,
            origin = plugin.origin,
            providers = providers,
            onDismiss = { showConfirmation = false },
            onConfirm = { onInstall(plugin.id, plugin.manifestUrl); showConfirmation = false }
        )
    }
    if (showRemovalConfirmation) {
        NuvioTrRemoveDialog(
            name = plugin.name,
            onDismiss = { showRemovalConfirmation = false },
            onConfirm = { onRemove(plugin.manifestUrl); showRemovalConfirmation = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CollectionRow(
    collection: NuvioTrCollection,
    installed: Boolean,
    installing: Boolean,
    onInstall: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }
    var showRemovalConfirmation by remember { mutableStateOf(false) }
    NuvioTrRow(
        name = collection.name,
        authors = collection.authors,
        description = collection.description,
        iconUrl = collection.iconUrl,
        installed = installed,
        installing = installing,
        onInstall = { showConfirmation = true },
        onRemove = { showRemovalConfirmation = true }
    )
    if (showConfirmation) {
        NuvioTrInstallDialog(
            name = collection.name,
            onDismiss = { showConfirmation = false },
            onConfirm = { onInstall(collection.id, collection.collectionUrl); showConfirmation = false }
        )
    }
    if (showRemovalConfirmation) {
        NuvioTrRemoveDialog(
            name = collection.name,
            onDismiss = { showRemovalConfirmation = false },
            onConfirm = { onRemove(collection.id); showRemovalConfirmation = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrInstallDialog(
    name: String,
    origin: String? = null,
    providers: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val message = if (origin.isNullOrBlank()) {
        stringResource(R.string.nuviotr_install_dialog_message, name)
    } else {
        stringResource(R.string.nuviotr_origin_dialog_message, origin, name)
    }
    val providerText = providers.distinct().joinToString(separator = " · ")
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(if (origin.isNullOrBlank()) R.string.nuviotr_install_dialog_title else R.string.nuviotr_origin_dialog_title),
        subtitle = message
    ) {
        if (providers.isNotEmpty()) {
            Text(
                text = stringResource(R.string.nuviotr_plugin_providers),
                style = MaterialTheme.typography.titleSmall,
                color = NuvioTheme.colors.TextPrimary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = NuvioTheme.colors.Surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(NuvioTheme.radii.md)
                    )
            ) {
                Text(
                    text = providerText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NuvioTheme.colors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(NuvioTheme.spacing.md)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDismiss) { Text(stringResource(R.string.nuviotr_cancel)) }
            Button(onClick = onConfirm) { Text(stringResource(R.string.nuviotr_confirm)) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PluginRow(plugin: NuvioTrPlugin, installing: Boolean, onInstall: (String, String) -> Unit, onRemove: (String) -> Unit) {
    val supports = listOfNotNull(if (plugin.supports.catalog) "Catalog" else null, if (plugin.supports.search) "Search" else null, if (plugin.supports.comments) "Comments" else null).joinToString(" · ")
    NuvioTrRow(plugin.name, plugin.authors, listOf(plugin.description, supports).filter { it.isNotBlank() }.joinToString("\n"), plugin.iconUrl, false, installing, { onInstall(plugin.id, plugin.manifestUrl) }, { onRemove(plugin.manifestUrl) })
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrRow(name: String, authors: List<String>, description: String, iconUrl: String, installed: Boolean, installing: Boolean, onInstall: () -> Unit, onRemove: () -> Unit) {
    SettingsActionRow(
        title = name,
        subtitle = listOf(authors.joinToString(), description).filter { it.isNotBlank() }.joinToString("\n"),
        value = null,
        onClick = { if (installed) onRemove() else onInstall() },
        enabled = !installing,
        leadingIcon = Icons.Default.Extension,
        leadingImageUrl = iconUrl,
        trailingIcon = if (installed) Icons.Default.Delete else Icons.Default.Download
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrRemoveDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.nuviotr_remove_dialog_title),
        subtitle = stringResource(R.string.nuviotr_remove_dialog_message, name)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDismiss) { Text(stringResource(R.string.nuviotr_cancel)) }
            Button(onClick = onConfirm) { Text(stringResource(R.string.nuviotr_confirm)) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CatalogRow(
    catalog: NuvioTrCatalog,
    dependencyNames: Map<String, String>,
    installedUrls: Set<String>,
    installing: Boolean,
    onInstall: (NuvioTrCatalog) -> Unit,
    onRemove: (String) -> Unit
) {
    val installed = catalog.manifestUrl.removeSuffix("/manifest.json").trimEnd('/') in installedUrls
    var showRemovalConfirmation by remember { mutableStateOf(false) }
    SettingsActionRow(
        title = catalog.name,
        subtitle = listOf(catalog.authors.joinToString(), catalog.description)
            .filter { it.isNotBlank() }
            .joinToString("\n"),
        value = null,
        onClick = { if (installed) showRemovalConfirmation = true else onInstall(catalog) },
        enabled = !installing,
        leadingIcon = Icons.Default.ViewList,
        leadingImageUrl = catalog.iconUrl,
        trailingIcon = if (installed) Icons.Default.Delete else Icons.Default.Download,
        subtitleBadges = catalog.dependencies.map { dependencyNames[it] ?: it }
    )
    if (showRemovalConfirmation) {
        NuvioDialog(
            onDismiss = { showRemovalConfirmation = false },
            title = stringResource(R.string.nuviotr_remove_dialog_title),
            subtitle = stringResource(R.string.nuviotr_remove_dialog_message, catalog.name)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showRemovalConfirmation = false }) { Text(stringResource(R.string.nuviotr_cancel)) }
                Button(onClick = { onRemove(catalog.manifestUrl); showRemovalConfirmation = false }) { Text(stringResource(R.string.nuviotr_confirm)) }
            }
        }
    }
}
