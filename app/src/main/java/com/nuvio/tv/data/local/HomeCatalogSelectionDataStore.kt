package com.nuvio.tv.data.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Stores the Home catalog source independently for each profile. */
@Singleton
class HomeCatalogSelectionDataStore @Inject constructor(
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    private companion object {
        const val FEATURE = "home_catalog_selection"
        val selectedSourceId = stringPreferencesKey("selected_source_id")
    }

    private fun store() = factory.get(profileManager.activeProfileId.value, FEATURE)

    suspend fun getSelectedSourceId(): String? =
        store().data.first()[selectedSourceId]?.trim()?.takeIf { it.isNotEmpty() }

    suspend fun setSelectedSourceId(sourceId: String) {
        val normalized = sourceId.trim()
        if (normalized.isEmpty()) return
        store().edit { preferences ->
            preferences[selectedSourceId] = normalized
        }
    }
}
