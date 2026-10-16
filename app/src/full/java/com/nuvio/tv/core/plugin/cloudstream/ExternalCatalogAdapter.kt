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
import kotlin.coroutines.Continuation

/** A CloudStream provider plus one of its declared main-page requests. */
data class ExternalCatalogRequest(
    val source: PluginSourceRef,
    /** CloudStream's provider name, distinct from the containing .cs3 extension name. */
    val providerName: String,
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
): List<ExternalCatalogRequest> {
    // `hasMainPage` is an optional Cloudstream UI hint. Older providers can
    // implement getMainPage and expose requests without setting that flag.
    // MainAPI itself supplies one blank placeholder, which is not a catalog.
    val requests = mainPage
        .filter { page -> page.name.isNotBlank() || page.data.isNotBlank() }
        .distinctBy { it.name to it.data }
        .map { page -> MainPageRequest(page.name, page.data, page.horizontalImages) }
        .ifEmpty {
            // This preserves Cloudstream's default request for providers that
            // implement getMainPage without declaring page metadata.
            if (overridesGetMainPage()) listOf(MainPageRequest("", "", false)) else emptyList()
        }

    return requests.map { request ->
        ExternalCatalogRequest(
            source = PluginSourceRef(RepositoryType.EXTERNAL_DEX, repositoryId, scraperId, providerKey),
            providerName = name,
            name = request.name,
            request = request
        )
    }
}

private fun MainAPI.overridesGetMainPage(): Boolean = runCatching {
    javaClass.getMethod(
        "getMainPage",
        Int::class.javaPrimitiveType!!,
        MainPageRequest::class.java,
        Continuation::class.java
    ).declaringClass != MainAPI::class.java
}.getOrDefault(false)

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
