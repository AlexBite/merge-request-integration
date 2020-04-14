package net.ntworld.mergeRequestIntegrationIde.diff

import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.vcs.changes.Change
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequest.MergeRequestInfo
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.DataChangedSource
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.*
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService

class SimpleOneSideDiffView(
    private val applicationService: ApplicationService,
    override val viewer: SimpleOnesideDiffViewer,
    private val change: Change,
    private val contentType: DiffView.ContentType
) : AbstractDiffView<SimpleOnesideDiffViewer>(applicationService, viewer) {

    override fun convertVisibleLineToLogicalLine(visibleLine: Int, contentType: DiffView.ContentType): Int {
        return visibleLine - 1
    }

    override fun createGutterIcons() {
        for (logicalLine in 0 until viewer.editor.document.lineCount) {
            registerGutterIconRenderer(GutterIconRendererFactory.makeGutterIconRenderer(
                viewer.editor.markupModel.addLineHighlighter(logicalLine, HighlighterLayer.LAST, null),
                applicationService.settings.showAddCommentIconsInDiffViewGutter,
                logicalLine,
                visibleLineLeft = if (contentType == DiffView.ContentType.BEFORE) logicalLine + 1 else null,
                visibleLineRight = if (contentType == DiffView.ContentType.AFTER) logicalLine + 1 else null,
                contentType = contentType,
                action = ::dispatchOnGutterActionPerformed
            ))
        }
    }

    override fun changeGutterIconsByComments(
        visibleLine: Int,
        contentType: DiffView.ContentType,
        comments: List<Comment>
    ) {
        updateGutterIcon(findGutterIconRenderer(visibleLine - 1, contentType), comments)
    }

    override fun updateComments(
        providerData: ProviderData,
        mergeRequestInfo: MergeRequestInfo,
        visibleLine: Int,
        contentType: DiffView.ContentType,
        comments: List<Comment>,
        requestSource: DataChangedSource
    ) {
        updateComments(
            providerData,
            mergeRequestInfo,
            viewer.editor,
            calcPosition(visibleLine - 1),
            findGutterIconRenderer(visibleLine - 1, contentType),
            comments
        )
    }

    override fun displayEditorOnLine(
        providerData: ProviderData,
        mergeRequestInfo: MergeRequestInfo,
        logicalLine: Int,
        contentType: DiffView.ContentType,
        comments: List<Comment>
    ) {
        displayCommentsAndEditorOnLine(
            providerData, mergeRequestInfo,
            viewer.editor,
            calcPosition(logicalLine),
            logicalLine, contentType,
            comments
        )
    }

    override fun changeCommentsVisibilityOnLine(
        providerData: ProviderData,
        mergeRequestInfo: MergeRequestInfo,
        logicalLine: Int,
        contentType: DiffView.ContentType,
        comments: List<Comment>,
        mode: DiffView.DisplayCommentMode
    ) {
        toggleCommentsOnLine(
            providerData, mergeRequestInfo,
            viewer.editor,
            calcPosition(logicalLine),
            logicalLine, contentType,
            comments,
            mode
        )
    }

    private fun calcPosition(logicalLine: Int): GutterPosition {
        return if (contentType == DiffView.ContentType.BEFORE) {
            GutterPosition(
                editorType = DiffView.EditorType.SINGLE_SIDE,
                changeType = findChangeType(viewer.editor, logicalLine),
                oldLine = logicalLine + 1,
                oldPath = change.beforeRevision!!.file.toString(),
                newLine = null,
                newPath = null,
                baseHash = change.beforeRevision!!.revisionNumber.asString()
            )
        } else {
            GutterPosition(
                editorType = DiffView.EditorType.SINGLE_SIDE,
                changeType = findChangeType(viewer.editor, logicalLine),
                newLine = logicalLine + 1,
                newPath = change.afterRevision!!.file.toString(),
                oldLine = null,
                oldPath = null,
                headHash = change.afterRevision!!.revisionNumber.asString()
            )
        }
    }
}