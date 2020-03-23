package net.ntworld.mergeRequestIntegrationIde.diff

import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService
import net.ntworld.mergeRequestIntegrationIde.service.CodeReviewManager
import net.ntworld.mergeRequestIntegrationIde.service.ProjectService

object DiffFactory {
    fun makeDiffPresenter(
        applicationService: ApplicationService, projectService: ProjectService, model: DiffModel, view: DiffView<*>
    ): DiffPresenter {
        return DiffPresenterImpl(applicationService, projectService, model, view)
    }

    fun makeView(
        applicationService: ApplicationService,
        viewer: DiffViewerBase,
        change: Change
    ): DiffView<*>? {
        return when (viewer) {
            is SimpleOnesideDiffViewer -> makeSimpleOneSideDiffViewer(applicationService, viewer, change)
            is TwosideTextDiffViewer -> makeTwoSideTextDiffViewer(applicationService, viewer, change)
            is UnifiedDiffViewer -> makeUnifiedDiffView(applicationService, viewer, change)
            else -> null
        }
    }

    private fun makeSimpleOneSideDiffViewer(
        applicationService: ApplicationService, viewer: SimpleOnesideDiffViewer, change: Change
    ): DiffView<SimpleOnesideDiffViewer>? {
        return SimpleOneSideDiffView(applicationService, viewer, change, when(change.type) {
            Change.Type.NEW -> DiffView.ContentType.AFTER
            Change.Type.DELETED -> DiffView.ContentType.BEFORE
            else -> throw Exception("Invalid change type")
        })
    }

    private fun makeTwoSideTextDiffViewer(
        applicationService: ApplicationService, viewer: TwosideTextDiffViewer, change: Change
    ): DiffView<TwosideTextDiffViewer>? {
        return TwoSideTextDiffView(applicationService, viewer, change)
    }

    private fun makeUnifiedDiffView(
        applicationService: ApplicationService, viewer: UnifiedDiffViewer, change: Change
    ): DiffView<UnifiedDiffViewer>? {
        return UnifiedDiffView(applicationService, viewer, change)
    }

    fun makeDiffModel(applicationService: ApplicationService, project: Project, change: Change): DiffModel? {
        val codeReviewManager = applicationService.getProjectService(project).codeReviewManager
        if (null === codeReviewManager) {
            return null
        }
        return DiffModelImpl(codeReviewManager, change)
    }
}