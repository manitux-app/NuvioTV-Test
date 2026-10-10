package com.nuvio.tv.data.repository

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.NuvioTrApi
import com.nuvio.tv.data.remote.dto.NuvioTrRepositoriesDto
import com.nuvio.tv.domain.model.NuvioTrCatalog
import com.nuvio.tv.domain.model.NuvioTrCollection
import com.nuvio.tv.domain.model.NuvioTrPlugin
import com.nuvio.tv.domain.model.NuvioTrPluginSupports
import com.nuvio.tv.domain.model.NuvioTrRepositories
import com.nuvio.tv.domain.repository.NuvioTrRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NuvioTrRepositoryImpl @Inject constructor(
    private val api: NuvioTrApi
) : NuvioTrRepository {
    override suspend fun fetchRepositories(): Result<NuvioTrRepositories> = runCatching {
        val url = BuildConfig.NUVIOTR_REPOS_URL.trim()
        require(url.isNotBlank()) { "NUVIOTR_REPOS_URL is not configured" }
        api.getRepositories(url).toDomain()
    }
}

private fun NuvioTrRepositoriesDto.toDomain() = NuvioTrRepositories(
    catalogs = catalogs.map { item ->
        NuvioTrCatalog(item.id, item.name, item.authors, item.description, item.repoUrl, item.iconUrl, item.manifestUrl, item.dependencies)
    },
    collections = collections.map { item ->
        NuvioTrCollection(item.id, item.name, item.authors, item.description, item.repoUrl, item.iconUrl, item.collectionUrl)
    },
    plugins = plugins.map { item ->
        NuvioTrPlugin(
            item.id, item.name, item.authors, item.description, item.repoUrl, item.iconUrl, item.manifestUrl, item.origin,
            NuvioTrPluginSupports(item.supports.catalog, item.supports.stream, item.supports.search, item.supports.comments)
        )
    }
)
