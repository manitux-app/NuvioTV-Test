package com.nuvio.tv.domain.model

import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps catalog provenance available while navigating from Home to Detail.
 * Persistent storage for process recreation is added with the playback flow.
 */
object PluginContentRegistry {
    private const val MAX_ITEMS = 512
    private val entries = ConcurrentHashMap<String, PluginCatalogItem>()
    private val streamSelections = ConcurrentHashMap<String, HomeStreamSourceSelection>()
    @Volatile private var activeStreamSelection: HomeStreamSourceSelection? = null

    fun put(items: Iterable<PluginCatalogItem>) {
        items.forEach { item -> entries[item.content.stableId()] = item }
        if (entries.size > MAX_ITEMS) {
            entries.keys.take(entries.size - MAX_ITEMS).forEach(entries::remove)
        }
    }

    fun get(stableId: String): PluginContentRef? = entries[stableId]?.content

    /** Keeps display metadata available for TMDB resolution after Home navigation. */
    fun getItem(stableId: String): PluginCatalogItem? = entries[stableId]

    /** Captures the Home source choice at navigation time, not catalog-load time. */
    fun selectStreamSource(stableId: String, selection: HomeStreamSourceSelection) {
        activeStreamSelection = selection
        streamSelections[stableId] = selection
    }

    /** Falls back to the current Home selection if this content was evicted before navigation. */
    fun selectedStreamSource(stableId: String): PluginSourceRef? =
        selectedStreamSelection(stableId)?.source

    fun selectedStreamSelection(stableId: String): HomeStreamSourceSelection? =
        streamSelections[stableId] ?: activeStreamSelection
}

/**
 * The source restriction chosen on Home. Server-based catalog items have no plugin
 * provenance, so this carries the independent source-menu choice into playback.
 */
data class HomeStreamSourceSelection(
    val source: PluginSourceRef? = null,
    val repositoryId: String? = null,
    val restrictAddonSources: Boolean = false
)
