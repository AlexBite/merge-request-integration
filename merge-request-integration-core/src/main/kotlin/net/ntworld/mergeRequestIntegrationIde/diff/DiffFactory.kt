package net.ntworld.mergeRequestIntegrationIde.diff

import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.Side
import com.intellij.openapi.vcs.changes.Change
import net.ntworld.mergeRequestIntegrationIde.infrastructure.ReviewContext
import net.ntworld.mergeRequestIntegrationIde.infrastructure.ApplicationService
import net.ntworld.mergeRequestIntegrationIde.infrastructure.ProjectService

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
            Change.Type.DELETED -> Side.LEFT
            Change.Type.NEW -> Side.RIGHT
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

    fun makeDiffModel(projectService: ProjectService, reviewContext: ReviewContext, change: Change): DiffModel? {
        return DiffModelImpl(projectService, reviewContext, change, false)
    }
}