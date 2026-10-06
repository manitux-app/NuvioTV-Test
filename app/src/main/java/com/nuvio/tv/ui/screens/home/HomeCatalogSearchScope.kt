package com.nuvio.tv.ui.screens.home

/** Carries the Home catalog selection into the next Search view model exactly once. */
object HomeCatalogSearchScope {
    @Volatile
    private var sourceId: String? = null

    fun open(sourceId: String) {
        this.sourceId = sourceId
    }

    fun consume(): String? = sourceId.also { sourceId = null }
}
