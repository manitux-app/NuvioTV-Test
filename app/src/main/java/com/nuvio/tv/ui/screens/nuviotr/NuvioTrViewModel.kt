package com.nuvio.tv.ui.screens.nuviotr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.nuvio.tv.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.domain.model.NuvioTrRepositories
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.domain.repository.NuvioTrRepository
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.data.local.CollectionsDataStore
import com.nuvio.tv.data.remote.api.NuvioTrApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NuvioTrUiState(
    val isLoading: Boolean = true,
    val repositories: NuvioTrRepositories = NuvioTrRepositories(),
    val error: String? = null,
    val notification: String? = null,
    val notificationIsError: Boolean = false,
    val installingId: String? = null,
    val installedManifestUrls: Set<String> = emptySet(),
    val installedCollectionIds: Set<String> = emptySet(),
    val installedPluginUrls: Set<String> = emptySet(),
    val pluginProviders: Map<String, List<String>> = emptyMap(),
    val loadingPluginProviders: Set<String> = emptySet(),
    val catalogContents: Map<String, List<String>> = emptyMap(),
    val collectionContents: Map<String, List<String>> = emptyMap(),
    val loadingCatalogContents: Set<String> = emptySet(),
    val loadingCollectionContents: Set<String> = emptySet()
)

@HiltViewModel
class NuvioTrViewModel @Inject constructor(
    private val nuvioTrRepository: NuvioTrRepository,
    private val addonRepository: AddonRepository,
    private val collectionsDataStore: CollectionsDataStore,
    private val pluginManager: PluginManager,
    private val api: NuvioTrApi,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(NuvioTrUiState())
    val uiState: StateFlow<NuvioTrUiState> = _uiState.asStateFlow()

    init {
        observeInstalledAddons()
        observeInstalledCollections()
        observeInstalledPlugins()
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        nuvioTrRepository.fetchRepositories().fold(
            onSuccess = { repositories -> _uiState.update { it.copy(isLoading = false, repositories = repositories) } },
            onFailure = { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        )
    }

    fun installCatalog(id: String, manifestUrl: String, dependencies: List<Pair<String, String>> = emptyList()) = viewModelScope.launch {
        val baseUrl = manifestUrl.removeSuffix("/manifest.json").trimEnd('/')
        _uiState.update { it.copy(installingId = id, error = null, notification = null) }
        val dependencyError = dependencies.firstNotNullOfOrNull { (_, repoUrl) ->
            pluginManager.addRepository(repoUrl).exceptionOrNull()
        }
        if (dependencyError != null) {
            _uiState.update { it.copy(installingId = null, notification = context.getString(R.string.nuviotr_install_failed), notificationIsError = true) }
            return@launch
        }
        when (val result = addonRepository.fetchAddon(baseUrl)) {
            is NetworkResult.Success -> {
                addonRepository.addAddon(baseUrl)
                _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_install_success), notificationIsError = false) }
            }
            is NetworkResult.Error -> _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_install_failed), notificationIsError = true) }
            NetworkResult.Loading -> Unit
        }
        _uiState.update { it.copy(installingId = null) }
    }

    fun removeCatalog(manifestUrl: String) = viewModelScope.launch {
        addonRepository.removeAddon(manifestUrl.removeSuffix("/manifest.json").trimEnd('/'))
        _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_remove_success), notificationIsError = false) }
    }

    fun installCollection(id: String, collectionUrl: String) = viewModelScope.launch {
        _uiState.update { it.copy(installingId = id, error = null) }
        runCatching {
            val incoming = collectionsDataStore.importFromJson(api.getCollection(collectionUrl).string())
            val current = collectionsDataStore.getCurrentCollections()
            collectionsDataStore.setCollections((current.filterNot { saved -> incoming.any { it.id == saved.id } } + incoming))
            collectionsDataStore.saveNuvioTrCollectionSource(id, incoming.mapTo(mutableSetOf()) { it.id })
        }.onSuccess { _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_install_success), notificationIsError = false) } }
            .onFailure { _ -> _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_install_failed), notificationIsError = true) } }
        _uiState.update { it.copy(installingId = null) }
    }

    fun removeCollection(id: String) = viewModelScope.launch {
        collectionsDataStore.removeNuvioTrCollectionSource(id).forEach { collectionId ->
            collectionsDataStore.removeCollection(collectionId)
        }
        _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_remove_success), notificationIsError = false) }
    }

    fun installPlugin(id: String, manifestUrl: String) = viewModelScope.launch {
        _uiState.update { it.copy(installingId = id, error = null) }
        pluginManager.addRepository(manifestUrl).onSuccess {
            _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_install_success), notificationIsError = false) }
        }.onFailure { _ -> _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_install_failed), notificationIsError = true) } }
        _uiState.update { it.copy(installingId = null) }
    }

    fun loadPluginProviders(id: String, manifestUrl: String) = viewModelScope.launch {
        if (id in _uiState.value.pluginProviders || id in _uiState.value.loadingPluginProviders) return@launch
        _uiState.update { it.copy(loadingPluginProviders = it.loadingPluginProviders + id) }
        val providers = runCatching { pluginManager.getRepositoryProviderNames(manifestUrl) }
            .getOrDefault(emptyList())
        _uiState.update { state ->
            state.copy(
                pluginProviders = state.pluginProviders + (id to providers),
                loadingPluginProviders = state.loadingPluginProviders - id
            )
        }
    }

    fun loadCatalogContents(id: String, manifestUrl: String) = viewModelScope.launch {
        if (id in _uiState.value.catalogContents || id in _uiState.value.loadingCatalogContents) return@launch
        _uiState.update { it.copy(loadingCatalogContents = it.loadingCatalogContents + id) }
        val baseUrl = manifestUrl.removeSuffix("/manifest.json").trimEnd('/')
        val contents = runCatching {
            when (val result = addonRepository.fetchAddon(baseUrl)) {
                is NetworkResult.Success -> result.data.catalogs
                    .map { it.name }
                    .filter { it.isNotBlank() }
                    .distinct()
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
        _uiState.update { state ->
            state.copy(
                catalogContents = state.catalogContents + (id to contents),
                loadingCatalogContents = state.loadingCatalogContents - id
            )
        }
    }

    fun loadCollectionContents(id: String, collectionUrl: String) = viewModelScope.launch {
        if (id in _uiState.value.collectionContents || id in _uiState.value.loadingCollectionContents) return@launch
        _uiState.update { it.copy(loadingCollectionContents = it.loadingCollectionContents + id) }
        val contents = runCatching {
            collectionsDataStore.importFromJson(api.getCollection(collectionUrl).string())
                .map { it.title }
                .filter { it.isNotBlank() }
                .distinct()
        }.getOrDefault(emptyList())
        _uiState.update { state ->
            state.copy(
                collectionContents = state.collectionContents + (id to contents),
                loadingCollectionContents = state.loadingCollectionContents - id
            )
        }
    }

    fun removePlugin(repoUrl: String) = viewModelScope.launch {
        pluginManager.repositories.first().firstOrNull { it.url.trimEnd('/') == repoUrl.trimEnd('/') }?.let { pluginManager.removeRepository(it.id) }
        _uiState.update { it.copy(notification = context.getString(R.string.nuviotr_remove_success), notificationIsError = false) }
    }

    fun clearNotification() = _uiState.update { it.copy(notification = null, notificationIsError = false) }

    private fun observeInstalledAddons() = viewModelScope.launch {
        addonRepository.getInstalledAddons().collect { addons ->
            _uiState.update { state ->
                state.copy(installedManifestUrls = addons.map { it.baseUrl.trimEnd('/') }.toSet())
            }
        }
    }

    private fun observeInstalledCollections() = viewModelScope.launch {
        collectionsDataStore.nuvioTrCollectionSources.collect { sourceIds ->
            _uiState.update { state ->
                state.copy(installedCollectionIds = sourceIds)
            }
        }
    }

    private fun observeInstalledPlugins() = viewModelScope.launch {
        pluginManager.repositories.collect { repositories ->
            _uiState.update { state ->
                state.copy(installedPluginUrls = repositories.mapTo(mutableSetOf()) { it.url.trimEnd('/') })
            }
        }
    }
}
