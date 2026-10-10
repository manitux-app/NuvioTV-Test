package com.nuvio.tv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class NuvioTrRepositories(
    val catalogs: List<NuvioTrCatalog> = emptyList(),
    val collections: List<NuvioTrCollection> = emptyList(),
    val plugins: List<NuvioTrPlugin> = emptyList()
)

@Immutable
data class NuvioTrCatalog(
    val id: String,
    val name: String,
    val authors: List<String>,
    val description: String,
    val repoUrl: String,
    val iconUrl: String,
    val manifestUrl: String,
    val dependencies: List<String>
)

@Immutable
data class NuvioTrCollection(
    val id: String,
    val name: String,
    val authors: List<String>,
    val description: String,
    val repoUrl: String,
    val iconUrl: String,
    val collectionUrl: String
)

@Immutable
data class NuvioTrPlugin(
    val id: String,
    val name: String,
    val authors: List<String>,
    val description: String,
    val repoUrl: String,
    val iconUrl: String,
    val manifestUrl: String,
    val origin: String? = null,
    val supports: NuvioTrPluginSupports
)

@Immutable
data class NuvioTrPluginSupports(
    val catalog: Boolean,
    val stream: Boolean,
    val search: Boolean,
    val comments: Boolean
)
