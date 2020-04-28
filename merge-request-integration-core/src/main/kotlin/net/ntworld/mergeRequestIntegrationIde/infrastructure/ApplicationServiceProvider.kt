package net.ntworld.mergeRequestIntegrationIde.infrastructure

import com.intellij.openapi.project.Project
import net.ntworld.foundation.Infrastructure
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequest.ProviderInfo
import net.ntworld.mergeRequest.api.ApiCredentials
import net.ntworld.mergeRequestIntegrationIde.compatibility.IntellijIdeApi
import net.ntworld.mergeRequestIntegrationIde.infrastructure.setting.ApplicationSettingsManager
import net.ntworld.mergeRequestIntegrationIde.watcher.WatcherManager

interface ApplicationServiceProvider {

    val infrastructure: Infrastructure

    val intellijIdeApi: IntellijIdeApi

    val settingsManager: ApplicationSettingsManager

    val watcherManager: WatcherManager

    fun findProjectServiceProvider(project: Project): ProjectServiceProvider

    fun getChangesToolWindowId(): String

    fun addProviderConfiguration(id: String, info: ProviderInfo, credentials: ApiCredentials)

    fun removeAllProviderConfigurations()

    fun getProviderConfigurations(): List<ProviderSettings>

    fun isLegal(providerData: ProviderData): Boolean

}