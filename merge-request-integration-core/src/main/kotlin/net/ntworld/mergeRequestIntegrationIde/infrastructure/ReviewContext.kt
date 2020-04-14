package net.ntworld.mergeRequestIntegrationIde.infrastructure

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.util.messages.MessageBusConnection
import git4idea.repo.GitRepository
import net.ntworld.mergeRequest.*

interface ReviewContext {
    val project: Project

    val providerData: ProviderData

    val mergeRequestInfo: MergeRequestInfo

    val messageBusConnection: MessageBusConnection

    val diffReference: DiffReference?

    val repository: GitRepository?

    val commits: List<Commit>

    val comments: List<Comment>

    fun getCommentsByPath(path: String): List<Comment>

    fun openChange(change: Change)

    fun hasAnyChangeOpened(): Boolean

    fun closeAllChanges()
}
