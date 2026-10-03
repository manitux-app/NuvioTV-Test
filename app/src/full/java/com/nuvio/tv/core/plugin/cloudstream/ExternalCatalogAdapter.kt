package com.nuvio.tv.core.plugin.cloudstream

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.AnimeSearchResponse
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.TvSeriesSearchResponse
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.PluginCatalogItem
import com.nuvio.tv.domain.model.PluginContentRef
import com.nuvio.tv.domain.model.PluginSourceRef
import com.nuvio.tv.domain.model.RepositoryType

/** A CloudStream provider plus one of its declared main-page requests. */
data class ExternalCatalogRequest(
    val source: PluginSourceRef,
    val name: String,
    val request: MainPageRequest
)

data class ExternalCatalogRow(
    val name: String,
    val type: ContentType,
    val items: List<PluginCatalogItem>
)

/** One getMainPage response; page advancement belongs to the request, not an individual shelf. */
data class ExternalCatalogPage(
    val rows: List<ExternalCatalogRow>,
    val hasNext: Boolean
)

internal fun MainAPI.catalogRequests(
    repositoryId: String,
    scraperId: String,
    providerKey: String
): List<ExternalCatalogRequest> = mainPage
    .distinctBy { it.name to it.data }
    .map { page ->
        ExternalCatalogRequest(
            source = PluginSourceRef(RepositoryType.EXTERNAL_DEX, repositoryId, scraperId, providerKey),
            name = page.name,
            request = MainPageRequest(page.name, page.data, page.horizontalImages)
        )
    }

internal fun HomePageList.toCatalogRow(source: PluginSourceRef): ExternalCatalogRow? {
    val items = list.mapNotNull { it.toCatalogItem(source) }
    val type = items.firstOrNull()?.content?.type ?: return null
    return ExternalCatalogRow(name = name, type = type, items = items.filter { it.content.type == type })
}

internal fun SearchResponse.toCatalogItem(source: PluginSourceRef): PluginCatalogItem? {
    val type = type?.toNuvioType()?.let(ContentType::fromString)
        ?.takeIf { it == ContentType.MOVIE || it == ContentType.SERIES }
        ?: return null
    val contentUrl = url.takeIf { it.isNotBlank() } ?: return null
    val year = when (this) {
        is MovieSearchResponse -> year
        is TvSeriesSearchResponse -> year
        is AnimeSearchResponse -> year
        else -> null
    }
    return PluginCatalogItem(
        content = PluginContentRef(source, contentUrl, type, url = contentUrl),
        name = name.takeIf { it.isNotBlank() } ?: return null,
        year = year,
        poster = posterUrl
    )
}
