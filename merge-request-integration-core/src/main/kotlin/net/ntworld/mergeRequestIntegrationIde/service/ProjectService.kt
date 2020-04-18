package net.ntworld.mergeRequestIntegrationIde.service

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.vcs.changes.Change
import com.intellij.util.EventDispatcher
import com.intellij.util.messages.MessageBus
import net.ntworld.mergeRequest.*
import net.ntworld.mergeRequest.api.ApiCredentials
import net.ntworld.mergeRequest.api.MergeRequestOrdering
import net.ntworld.mergeRequest.query.GetMergeRequestFilter
import net.ntworld.mergeRequestIntegrationIde.infrastructure.ReviewContext
import net.ntworld.mergeRequestIntegrationIde.infrastructure.service.RepositoryFileService
import com.intellij.openapi.project.Project as IdeaProject

interface ProjectService {
    val project: IdeaProject

    val messageBus: MessageBus

    val dispatcher: EventDispatcher<ProjectEventListener>

    val notification: NotificationGroup

    val codeReviewManager: CodeReviewManager?

    val registeredProviders: List<ProviderData>

    val repositoryFile: RepositoryFileService

    fun getApplicationService(): ApplicationService

    fun findFiltersByProviderId(id: String): Pair<GetMergeRequestFilter, MergeRequestOrdering>

    fun saveFiltersOfProvider(id: String, filters: GetMergeRequestFilter, ordering: MergeRequestOrdering)

    fun supported(): List<ProviderInfo>

    fun addProviderConfiguration(id: String, info: ProviderInfo, credentials: ApiCredentials, repository: String)

    fun removeProviderConfiguration(id: String)

    fun getProviderConfigurations(): List<ProviderSettings>

    fun clear()

    fun register(settings: ProviderSettings)

    fun isDoingCodeReview(): Boolean

    fun findReviewContextWhichDoingCodeReview(): ReviewContext?

    fun isReviewing(providerData: ProviderData, mergeRequest: MergeRequest): Boolean

    fun setCodeReviewComments(
        providerData: ProviderData,
        mergeRequest: MergeRequest,
        comments: Collection<Comment>
    )

    fun getCodeReviewComments(): Collection<Comment>

    fun setCodeReviewCommits(
        providerData: ProviderData,
        mergeRequest: MergeRequest,
        commits: Collection<Commit>
    )

    fun getCodeReviewCommits(): Collection<Commit>

    fun setCodeReviewChanges(
        providerData: ProviderData,
        mergeRequest: MergeRequest,
        changes: Collection<Change>
    )

    fun getCodeReviewChanges(): Collection<Change>

    fun notify(message: String)

    fun notify(message: String, type: NotificationType)
}
