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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.nuvio.tv.domain.model.CatalogDescriptor
import com.nuvio.tv.domain.model.ContentType
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
                        CatalogRow(
                            catalog = catalog,
                            dependencyNames = dependencyNames,
                            installedUrls = state.installedManifestUrls,
                            installing = state.installingId == catalog.id,
                            contents = state.catalogContents[catalog.id].orEmpty(),
                            isLoadingContents = catalog.id in state.loadingCatalogContents,
                            onInstall = { selected -> pendingCatalog = selected },
                            onLoadContents = viewModel::loadCatalogContents,
                            onRemove = viewModel::removeCatalog
                        )
                    }
                }
                section == NuvioTrSection.Collections -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredCollections, key = { it.id }) { collection ->
                        CollectionRow(
                            collection = collection,
                            installed = collection.id in state.installedCollectionIds,
                            installing = state.installingId == collection.id,
                            contents = state.collectionContents[collection.id].orEmpty(),
                            isLoadingContents = collection.id in state.loadingCollectionContents,
                            onInstall = viewModel::installCollection,
                            onLoadContents = viewModel::loadCollectionContents,
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
                            isLoadingProviders = plugin.id in state.loadingPluginProviders,
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
    isLoadingProviders: Boolean,
    onInstall: (String, String) -> Unit,
    onLoadProviders: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }
    var showRemovalConfirmation by remember { mutableStateOf(false) }
    var showContents by remember { mutableStateOf(false) }
    val contentsFocusRequester = remember { FocusRequester() }
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
        onClick = {},
        enabled = !installing,
        leadingIcon = Icons.Default.Extension,
        leadingImageUrl = plugin.iconUrl,
        trailingIcon = if (installed) Icons.Default.Delete else Icons.Default.Download,
        subtitleSuccessBadges = badges,
        subtitleErrorBadges = plugin.origin?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty(),
        inlineActions = { actionsVisible ->
            LaunchedEffect(actionsVisible) {
                if (actionsVisible) runCatching { contentsFocusRequester.requestFocus() }
            }
            NuvioTrRepositoryInlineActions(
                installLabel = stringResource(if (installed) R.string.nuviotr_remove else R.string.nuviotr_install),
                initialFocusRequester = contentsFocusRequester,
                onContents = {
                    onLoadProviders(plugin.id, plugin.manifestUrl)
                    showContents = true
                },
                onInstallOrRemove = {
                    if (installed) showRemovalConfirmation = true else showConfirmation = true
                }
            )
        }
    )
    if (showContents) {
        NuvioTrRepositoryContentsDialog(
            contents = providers,
            isLoading = isLoadingProviders,
            onDismiss = { showContents = false }
        )
    }
    if (showConfirmation) {
        NuvioTrInstallDialog(
            name = plugin.name,
            origin = plugin.origin,
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
    contents: List<NuvioTrCollectionContent>,
    isLoadingContents: Boolean,
    onInstall: (String, String) -> Unit,
    onLoadContents: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }
    var showRemovalConfirmation by remember { mutableStateOf(false) }
    var showContents by remember { mutableStateOf(false) }
    val contentsFocusRequester = remember { FocusRequester() }
    NuvioTrRow(
        name = collection.name,
        authors = collection.authors,
        description = collection.description,
        iconUrl = collection.iconUrl,
        installed = installed,
        installing = installing,
        onClick = {},
        inlineActions = { actionsVisible ->
            LaunchedEffect(actionsVisible) {
                if (actionsVisible) runCatching { contentsFocusRequester.requestFocus() }
            }
            NuvioTrRepositoryInlineActions(
                installLabel = stringResource(if (installed) R.string.nuviotr_remove else R.string.nuviotr_install),
                initialFocusRequester = contentsFocusRequester,
                onContents = {
                    onLoadContents(collection.id, collection.collectionUrl)
                    showContents = true
                },
                onInstallOrRemove = {
                    if (installed) showRemovalConfirmation = true else showConfirmation = true
                }
            )
        }
    )
    if (showContents) {
        NuvioTrCollectionContentsDialog(
            contents = contents,
            isLoading = isLoadingContents,
            onDismiss = { showContents = false }
        )
    }
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
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val message = if (origin.isNullOrBlank()) {
        stringResource(R.string.nuviotr_install_dialog_message, name)
    } else {
        stringResource(R.string.nuviotr_origin_dialog_message, origin, name)
    }
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(if (origin.isNullOrBlank()) R.string.nuviotr_install_dialog_title else R.string.nuviotr_origin_dialog_title),
        subtitle = message
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDismiss) { Text(stringResource(R.string.nuviotr_cancel)) }
            Button(onClick = onConfirm) { Text(stringResource(R.string.nuviotr_confirm)) }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrRepositoryContentsDialog(
    contents: List<String>,
    isLoading: Boolean = false,
    onDismiss: () -> Unit
) {
    val distinctContents = contents.distinct()
    val firstContentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isLoading, distinctContents) {
        if (!isLoading && distinctContents.isNotEmpty()) {
            runCatching { firstContentFocusRequester.requestFocus() }
        }
    }
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.nuviotr_repository_contents)
    ) {
        if (isLoading) {
            Text(
                text = stringResource(R.string.nuviotr_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
            ) {
                items(distinctContents) { content ->
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (content == distinctContents.firstOrNull()) {
                                    Modifier.focusRequester(firstContentFocusRequester)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            text = content,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrCatalogContentsDialog(
    contents: List<CatalogDescriptor>,
    isLoading: Boolean = false,
    onDismiss: () -> Unit
) {
    val firstContentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isLoading, contents) {
        if (!isLoading && contents.isNotEmpty()) {
            runCatching { firstContentFocusRequester.requestFocus() }
        }
    }
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.nuviotr_repository_contents)
    ) {
        if (isLoading) {
            Text(
                text = stringResource(R.string.nuviotr_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
            ) {
                itemsIndexed(
                    items = contents,
                    key = { index, catalog -> "${catalog.type}:${catalog.id}:$index" }
                ) { _, catalog ->
                    val categories = catalog.extra
                        .flatMap { it.options.orEmpty() }
                        .filter { it.isNotBlank() }
                        .distinct()
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (catalog == contents.firstOrNull()) {
                                    Modifier.focusRequester(firstContentFocusRequester)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xxs)
                        ) {
                            Text(text = catalog.name, maxLines = 1)
                            Text(
                                text = stringResource(
                                    if (catalog.type == ContentType.MOVIE) R.string.type_movie else R.string.type_series
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = NuvioTheme.colors.TextSecondary,
                                maxLines = 1
                            )
                            if (categories.isNotEmpty()) {
                                Text(
                                    text = categories.joinToString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NuvioTheme.colors.TextTertiary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrCollectionContentsDialog(
    contents: List<NuvioTrCollectionContent>,
    isLoading: Boolean = false,
    onDismiss: () -> Unit
) {
    val firstContentFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isLoading, contents) {
        if (!isLoading && contents.isNotEmpty()) {
            runCatching { firstContentFocusRequester.requestFocus() }
        }
    }
    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.nuviotr_repository_contents)
    ) {
        if (isLoading) {
            Text(
                text = stringResource(R.string.nuviotr_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioTheme.colors.TextSecondary
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xs)
            ) {
                items(contents, key = { "${it.name}:${it.type}:${it.categories}" }) { content ->
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (content == contents.firstOrNull()) {
                                    Modifier.focusRequester(firstContentFocusRequester)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.xxs)
                        ) {
                            Text(text = content.name, maxLines = 1)
                            Text(
                                text = stringResource(
                                    if (content.type == ContentType.MOVIE) R.string.type_movie else R.string.type_series
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = NuvioTheme.colors.TextSecondary,
                                maxLines = 1
                            )
                            if (content.categories.isNotEmpty()) {
                                Text(
                                    text = content.categories.joinToString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NuvioTheme.colors.TextTertiary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrRow(
    name: String,
    authors: List<String>,
    description: String,
    iconUrl: String,
    installed: Boolean,
    installing: Boolean,
    onClick: () -> Unit,
    inlineActions: @Composable androidx.compose.foundation.layout.ColumnScope.(Boolean) -> Unit = {}
) {
    SettingsActionRow(
        title = name,
        subtitle = listOf(authors.joinToString(), description).filter { it.isNotBlank() }.joinToString("\n"),
        value = null,
        onClick = onClick,
        enabled = !installing,
        leadingIcon = Icons.Default.Extension,
        leadingImageUrl = iconUrl,
        trailingIcon = if (installed) Icons.Default.Delete else Icons.Default.Download,
        inlineActions = inlineActions
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NuvioTrRepositoryInlineActions(
    installLabel: String,
    initialFocusRequester: FocusRequester,
    onContents: () -> Unit,
    onInstallOrRemove: () -> Unit
) {
    Row(
        modifier = Modifier.padding(top = NuvioTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
    ) {
        Button(
            onClick = onContents,
            modifier = Modifier.focusRequester(initialFocusRequester)
        ) {
            Text(stringResource(R.string.nuviotr_repository_contents))
        }
        Button(onClick = onInstallOrRemove) {
            Text(installLabel)
        }
    }
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
    contents: List<CatalogDescriptor>,
    isLoadingContents: Boolean,
    onInstall: (NuvioTrCatalog) -> Unit,
    onLoadContents: (String, String) -> Unit,
    onRemove: (String) -> Unit
) {
    val installed = catalog.manifestUrl.removeSuffix("/manifest.json").trimEnd('/') in installedUrls
    var showRemovalConfirmation by remember { mutableStateOf(false) }
    var showContents by remember { mutableStateOf(false) }
    val contentsFocusRequester = remember { FocusRequester() }
    SettingsActionRow(
        title = catalog.name,
        subtitle = listOf(catalog.authors.joinToString(), catalog.description)
            .filter { it.isNotBlank() }
            .joinToString("\n"),
        value = null,
        onClick = {},
        enabled = !installing,
        leadingIcon = Icons.Default.ViewList,
        leadingImageUrl = catalog.iconUrl,
        trailingIcon = if (installed) Icons.Default.Delete else Icons.Default.Download,
        subtitleBadges = catalog.dependencies.map { dependencyNames[it] ?: it },
        inlineActions = { actionsVisible ->
            LaunchedEffect(actionsVisible) {
                if (actionsVisible) runCatching { contentsFocusRequester.requestFocus() }
            }
            NuvioTrRepositoryInlineActions(
                installLabel = stringResource(if (installed) R.string.nuviotr_remove else R.string.nuviotr_install),
                initialFocusRequester = contentsFocusRequester,
                onContents = {
                    onLoadContents(catalog.id, catalog.manifestUrl)
                    showContents = true
                },
                onInstallOrRemove = {
                    if (installed) showRemovalConfirmation = true else onInstall(catalog)
                }
            )
        }
    )
    if (showContents) {
        NuvioTrCatalogContentsDialog(
            contents = contents,
            isLoading = isLoadingContents,
            onDismiss = { showContents = false }
        )
    }
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
