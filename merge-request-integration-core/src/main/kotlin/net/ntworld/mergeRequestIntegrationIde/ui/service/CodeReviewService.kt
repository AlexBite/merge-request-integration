package net.ntworld.mergeRequestIntegrationIde.ui.service

import com.intellij.notification.NotificationType
import com.intellij.openapi.wm.ToolWindowManager
import net.ntworld.mergeRequest.Commit
import net.ntworld.mergeRequest.MergeRequest
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.CHANGES_TOOL_WINDOW_ID
import net.ntworld.mergeRequestIntegrationIde.service.ProjectService
import kotlin.Exception
import com.intellij.openapi.project.Project as IdeaProject

object CodeReviewService {
    var checkedOut = false

    fun start(ideaProject: IdeaProject, providerData: ProviderData, mergeRequest: MergeRequest, commits: List<Commit>) {
        checkedOut = false
        val projectService = ProjectService.getInstance(ideaProject)
        projectService.setCodeReviewCommits(providerData, mergeRequest, commits)
        projectService.dispatcher.multicaster.startCodeReview(providerData, mergeRequest)
        val toolWindow = ToolWindowManager.getInstance(ideaProject).getToolWindow(CHANGES_TOOL_WINDOW_ID)
        if (null !== toolWindow) {
            toolWindow.show(null)
        }
        checkout(ideaProject, providerData, mergeRequest, commits)
    }

    fun stop(ideaProject: IdeaProject, providerData: ProviderData, mergeRequest: MergeRequest) {
        ProjectService.getInstance(ideaProject).dispatcher.multicaster.stopCodeReview(providerData, mergeRequest)
        val toolWindow = ToolWindowManager.getInstance(ideaProject).getToolWindow(CHANGES_TOOL_WINDOW_ID)
        if (null !== toolWindow) {
            toolWindow.hide(null)
        }
        if (checkedOut) {
            CheckoutService.stop(ideaProject, providerData, mergeRequest)
            EditorStateService.stop(ideaProject, providerData, mergeRequest)
            DisplayChangesService.stop(ideaProject, providerData, mergeRequest)
        }
    }

    private fun checkout(
        ideaProject: IdeaProject,
        providerData: ProviderData,
        mergeRequest: MergeRequest,
        commits: List<Commit>
    ) {
        CheckoutService.start(ideaProject, providerData, mergeRequest, object : CheckoutService.Listener {
            override fun onError(exception: Exception) {
                ProjectService.getInstance(ideaProject).notify(
                    "Cannot checkout branch ${mergeRequest.sourceBranch}\n\nPlease do git checkout manually before click Code Review",
                    NotificationType.ERROR
                )
                this@CodeReviewService.stop(ideaProject, providerData, mergeRequest)
            }

            override fun onSuccess() {
                checkedOut = true
                EditorStateService.start(ideaProject, providerData, mergeRequest)
                DisplayChangesService.start(ideaProject, providerData, mergeRequest, commits)
            }
        })
    }
}