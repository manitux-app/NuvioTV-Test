package com.nuvio.tv.di

import com.nuvio.tv.core.auth.AuthManager
import com.nuvio.tv.core.plugin.PluginManager
import com.nuvio.tv.core.plugin.PluginRuntime
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.plugin.cloudstream.ExternalExtensionLoader
import com.nuvio.tv.core.plugin.cloudstream.ExternalExtensionRunner
import com.nuvio.tv.core.plugin.cloudstream.ExternalRepoParser
import com.nuvio.tv.core.sync.PluginSyncService
import com.nuvio.tv.data.local.PluginDataStore
import com.nuvio.tv.data.local.AddonPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PluginModule {

    @Provides
    @Singleton
    fun providePluginRuntime(): PluginRuntime {
        return PluginRuntime()
    }

    @Provides
    @Singleton
    fun providePluginManager(
        dataStore: PluginDataStore,
        addonPreferences: AddonPreferences,
        profileManager: ProfileManager,
        runtime: PluginRuntime,
        pluginSyncService: PluginSyncService,
        authManager: AuthManager,
        externalRepoParser: ExternalRepoParser,
        externalExtensionLoader: ExternalExtensionLoader,
        externalExtensionRunner: ExternalExtensionRunner
    ): PluginManager {
        return PluginManager(
            dataStore, addonPreferences, profileManager, runtime, pluginSyncService, authManager,
            externalRepoParser, externalExtensionLoader, externalExtensionRunner
        )
    }
}
