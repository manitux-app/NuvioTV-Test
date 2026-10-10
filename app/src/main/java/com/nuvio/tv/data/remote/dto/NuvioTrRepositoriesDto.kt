package com.nuvio.tv.data.remote.dto

data class NuvioTrRepositoriesDto(
    val catalogs: List<NuvioTrCatalogDto> = emptyList(),
    val collections: List<NuvioTrCollectionDto> = emptyList(),
    val plugins: List<NuvioTrPluginDto> = emptyList()
)

data class NuvioTrCatalogDto(
    val id: String,
    val name: String,
    val authors: List<String> = emptyList(),
    val description: String,
    val repoUrl: String,
    val iconUrl: String = "",
    val manifestUrl: String,
    val dependencies: List<String> = emptyList()
)

data class NuvioTrCollectionDto(
    val id: String,
    val name: String,
    val authors: List<String> = emptyList(),
    val description: String,
    val repoUrl: String,
    val iconUrl: String = "",
    val collectionUrl: String
)

data class NuvioTrPluginDto(
    val id: String,
    val name: String,
    val authors: List<String> = emptyList(),
    val description: String,
    val repoUrl: String,
    val iconUrl: String = "",
    val manifestUrl: String,
    val origin: String? = null,
    val supports: NuvioTrPluginSupportsDto = NuvioTrPluginSupportsDto()
)

data class NuvioTrPluginSupportsDto(
    val catalog: Boolean = false,
    val stream: Boolean = false,
    val search: Boolean = false,
    val comments: Boolean = false
)
