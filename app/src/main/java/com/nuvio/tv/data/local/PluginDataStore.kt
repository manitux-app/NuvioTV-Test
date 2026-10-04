package com.nuvio.tv.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.domain.model.PluginRepository
import com.nuvio.tv.domain.model.ScraperInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class PluginDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val factory: ProfileDataStoreFactory,
    private val profileManager: ProfileManager
) {
    companion object {
        internal const val FEATURE = "plugin_settings"
        internal const val GROUP_STREAMS_BY_REPOSITORY = "group_streams_by_repository"
    }

    private fun effectiveProfileId(): Int {
        val active = profileManager.activeProfile
        return if (active != null && active.usesPrimaryPlugins) 1 else profileManager.activeProfileId.value
    }

    private fun store(profileId: Int = effectiveProfileId()) = factory.get(profileId, FEATURE)

    private val effectiveProfileIdFlow: Flow<Int> = combine(
        profileManager.activeProfileId,
        profileManager.profiles
    ) { activeProfileId, profiles ->
        val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
        if (activeProfile?.usesPrimaryPlugins == true) 1 else activeProfileId
    }.distinctUntilChanged()

    private val repositoriesKey = stringPreferencesKey("repositories")
    private val defaultRepositoriesInitializedKey = booleanPreferencesKey("default_repositories_initialized")
    private val scrapersKey = stringPreferencesKey("scrapers")
    private val pluginsEnabledKey = booleanPreferencesKey("plugins_enabled")
    private val groupStreamsByRepositoryKey = booleanPreferencesKey(GROUP_STREAMS_BY_REPOSITORY)
    private val scraperSettingsKey = stringPreferencesKey("scraper_settings")

    private val repoListType = Types.newParameterizedType(List::class.java, PluginRepository::class.java)
    private val scraperListType = Types.newParameterizedType(List::class.java, ScraperInfo::class.java)
    private val settingsMapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    // Plugin code directory - per-profile
    private fun codeDir(profileId: Int): File {
        val dirName = if (profileId == 1) "plugin_code" else "plugin_code_p${profileId}"
        return File(context.filesDir, dirName)
    }

    val codeDir: File get() = codeDir(effectiveProfileId())

    private suspend fun ensureCodeDir(): File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        codeDir.also { it.mkdirs() }
    }

    // Repositories
    val repositories: Flow<List<PluginRepository>> = effectiveProfileIdFlow.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            prefs[repositoriesKey]?.let(::parseRepositories).orEmpty()
        }
    }

    private fun parseRepositories(json: String): List<PluginRepository> =
        try {
            moshi.adapter<List<PluginRepository>>(repoListType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

    /** Returns the current persisted list without relying on an asynchronously collected flow. */
    suspend fun getRepositories(profileId: Int = effectiveProfileId()): List<PluginRepository> =
        store(profileId).data.first()[repositoriesKey]?.let(::parseRepositories).orEmpty()

    /** Whether all default repositories have completed their initial setup for this profile. */
    suspend fun hasInitializedDefaultRepositories(profileId: Int = effectiveProfileId()): Boolean =
        store(profileId).data.first()[defaultRepositoriesInitializedKey] ?: false

    suspend fun markDefaultRepositoriesInitialized(profileId: Int = effectiveProfileId()) {
        store(profileId).edit { prefs ->
            prefs[defaultRepositoriesInitializedKey] = true
        }
    }

    suspend fun saveRepositories(
        repos: List<PluginRepository>,
        profileId: Int = effectiveProfileId(),
        bypassProfileWriteProtection: Boolean = false
    ) {
            val active = profileManager.activeProfile
            if (!bypassProfileWriteProtection && active != null && !active.isPrimary && active.usesPrimaryPlugins) return
        val json = moshi.adapter<List<PluginRepository>>(repoListType).toJson(repos)
        store(profileId).edit { prefs ->
            prefs[repositoriesKey] = json
        }
    }

    suspend fun addRepository(
        repo: PluginRepository,
        profileId: Int = effectiveProfileId(),
        bypassProfileWriteProtection: Boolean = false
    ) {
            val active = profileManager.activeProfile
            if (!bypassProfileWriteProtection && active != null && !active.isPrimary && active.usesPrimaryPlugins) return
        store(profileId).edit { prefs ->
            val current = prefs[repositoriesKey]
                ?.let(::parseRepositories)
                .orEmpty()
                .toMutableList()
            current.removeAll { it.id == repo.id }
            current.add(repo)
            prefs[repositoriesKey] = moshi.adapter<List<PluginRepository>>(repoListType).toJson(current)
        }
    }

    suspend fun removeRepository(repoId: String) {
            val active = profileManager.activeProfile
            if (active != null && !active.isPrimary && active.usesPrimaryPlugins) return
        store().edit { prefs ->
            val current = prefs[repositoriesKey]
                ?.let(::parseRepositories)
                .orEmpty()
                .filterNot { it.id == repoId }
            prefs[repositoriesKey] = moshi.adapter<List<PluginRepository>>(repoListType).toJson(current)
        }
    }

    suspend fun updateRepository(repo: PluginRepository) {
        store().edit { prefs ->
            val current = prefs[repositoriesKey]
                ?.let(::parseRepositories)
                .orEmpty()
                .toMutableList()
            val index = current.indexOfFirst { it.id == repo.id }
            if (index >= 0) {
                current[index] = repo
                val json = moshi.adapter<List<PluginRepository>>(repoListType).toJson(current)
                prefs[repositoriesKey] = json
            }
        }
    }

    // Scrapers
    val scrapers: Flow<List<ScraperInfo>> = effectiveProfileIdFlow.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            prefs[scrapersKey]?.let { json ->
                try {
                    moshi.adapter<List<ScraperInfo>>(scraperListType).fromJson(json) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    suspend fun getScrapers(profileId: Int = effectiveProfileId()): List<ScraperInfo> =
        store(profileId).data.first()[scrapersKey]?.let { json ->
            try {
                moshi.adapter<List<ScraperInfo>>(scraperListType).fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

    suspend fun saveScrapers(
        scrapers: List<ScraperInfo>,
        profileId: Int = effectiveProfileId(),
        bypassProfileWriteProtection: Boolean = false
    ) {
            val active = profileManager.activeProfile
            if (!bypassProfileWriteProtection && active != null && !active.isPrimary && active.usesPrimaryPlugins) return
        val json = moshi.adapter<List<ScraperInfo>>(scraperListType).toJson(scrapers)
        store(profileId).edit { prefs ->
            prefs[scrapersKey] = json
        }
    }

    suspend fun setScraperEnabled(scraperId: String, enabled: Boolean) {
        val current = scrapers.first().toMutableList()
        val index = current.indexOfFirst { it.id == scraperId }
        if (index >= 0) {
            val scraper = current[index]
            // Only enable if manifest allows
            if (enabled && !scraper.manifestEnabled) return
            current[index] = scraper.copy(enabled = enabled)
            saveScrapers(current)
        }
    }

    // Plugins enabled global toggle
    val pluginsEnabled: Flow<Boolean> = effectiveProfileIdFlow.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            prefs[pluginsEnabledKey] ?: true
        }
    }

    suspend fun setPluginsEnabled(enabled: Boolean) {
            val active = profileManager.activeProfile
            if (active != null && !active.isPrimary && active.usesPrimaryPlugins) return
        store().edit { prefs ->
            prefs[pluginsEnabledKey] = enabled
        }
    }

    val groupStreamsByRepository: Flow<Boolean> = effectiveProfileIdFlow.flatMapLatest { pid ->
        factory.get(pid, FEATURE).data.map { prefs ->
            prefs[groupStreamsByRepositoryKey] ?: true
        }
    }

    suspend fun setGroupStreamsByRepository(enabled: Boolean) {
            val active = profileManager.activeProfile
            if (active != null && !active.isPrimary && active.usesPrimaryPlugins) return
        store().edit { prefs ->
            prefs[groupStreamsByRepositoryKey] = enabled
        }
    }

    // Scraper code storage
    fun getScraperCodeFile(scraperId: String, profileId: Int = effectiveProfileId()): File =
        File(codeDir(profileId), "$scraperId.js")

    suspend fun saveScraperCode(scraperId: String, code: String, profileId: Int = effectiveProfileId()) {
        val dir = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            codeDir(profileId).also { it.mkdirs() }
        }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            File(dir, "$scraperId.js").writeText(code)
        }
    }

    suspend fun getScraperCode(scraperId: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val file = File(codeDir, "$scraperId.js")
            if (file.exists()) file.readText() else null
        }
    }

    suspend fun deleteScraperCode(scraperId: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            File(codeDir, "$scraperId.js").delete()
        }
    }

    suspend fun clearAllScraperCode() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            codeDir.listFiles()?.forEach { it.delete() }
        }
    }

    // Per-scraper settings
    suspend fun getScraperSettings(scraperId: String): Map<String, Any> {
        val prefs = store().data.first()
        val allSettings = prefs[scraperSettingsKey]?.let { json ->
            try {
                @Suppress("UNCHECKED_CAST")
                moshi.adapter<Map<String, Map<String, Any>>>(settingsMapType).fromJson(json) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        return allSettings[scraperId] as? Map<String, Any> ?: emptyMap()
    }

    suspend fun setScraperSettings(scraperId: String, settings: Map<String, Any>) {
        val prefs = store().data.first()
        val allSettings = prefs[scraperSettingsKey]?.let { json ->
            try {
                @Suppress("UNCHECKED_CAST")
                moshi.adapter<Map<String, Map<String, Any>>>(settingsMapType).fromJson(json)?.toMutableMap()
                    ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
        } ?: mutableMapOf()

        allSettings[scraperId] = settings

        val json = moshi.adapter<Map<String, Map<String, Any>>>(settingsMapType).toJson(allSettings)
        store().edit { p ->
            p[scraperSettingsKey] = json
        }
    }
}
