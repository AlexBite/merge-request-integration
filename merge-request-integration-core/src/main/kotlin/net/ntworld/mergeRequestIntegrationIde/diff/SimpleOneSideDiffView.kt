package net.ntworld.mergeRequestIntegrationIde.diff

import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.vcs.changes.Change
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequest.MergeRequest
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.AddGutterIconRenderer
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.CommentsGutterIconRenderer
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService

class SimpleOneSideDiffView(
    private val applicationService: ApplicationService,
    override val viewer: SimpleOnesideDiffViewer,
    private val change: Change,
    private val contentType: DiffView.ContentType
) : AbstractDiffView<SimpleOnesideDiffViewer>(applicationService, viewer) {
    override fun displayAddGutterIcons() {
        for (logicalLine in 0 until viewer.editor.document.lineCount) {
            val lineHighlighter = viewer.editor.markupModel.addLineHighlighter(logicalLine, HighlighterLayer.LAST, null)

            lineHighlighter.gutterIconRenderer = AddGutterIconRenderer(
                viewer.editor,
                applicationService.settings.showAddCommentIconsInDiffViewGutter && !hasCommentsGutter(
                    logicalLine,
                    contentType
                ),
                logicalLine + 1,
                logicalLine,
                this::onAddGutterIconClicked
            )
        }
    }

    private fun onAddGutterIconClicked(renderer: AddGutterIconRenderer, changeType: DiffView.ChangeType) {
        val position = if (contentType == DiffView.ContentType.BEFORE) {
            AddCommentRequestedPosition(
                editorType = DiffView.EditorType.SINGLE_SIDE,
                changeType = changeType,
                oldLine = renderer.visibleLine,
                oldPath = change.beforeRevision!!.file.toString(),
                newLine = null,
                newPath = null,
                baseHash = change.beforeRevision!!.revisionNumber.asString()
            )
        } else {
            AddCommentRequestedPosition(
                editorType = DiffView.EditorType.SINGLE_SIDE,
                changeType = changeType,
                newLine = renderer.visibleLine,
                newPath = change.afterRevision!!.file.toString(),
                oldLine = null,
                oldPath = null,
                headHash = change.afterRevision!!.revisionNumber.asString()
            )
        }
        dispatcher.multicaster.onAddGutterIconClicked(renderer, position)
    }

    override fun displayCommentsGutterIcon(
        visibleLine: Int,
        contentType: DiffView.ContentType,
        comments: List<Comment>
    ) {
        val logicalLine = visibleLine - 1
        if (!hasCommentsGutter(logicalLine, contentType)) {
            val lineHighlighter = viewer.editor.markupModel.addLineHighlighter(logicalLine, HighlighterLayer.LAST, null)
            lineHighlighter.gutterIconRenderer = CommentsGutterIconRenderer(
                visibleLine, logicalLine, contentType, dispatcher.multicaster::onCommentsGutterIconClicked
            )
        }
        registerCommentsGutter(logicalLine, contentType, comments)
    }

    override fun displayCommentsOnLine(
        providerData: ProviderData,
        mergeRequest: MergeRequest,
        visibleLine: Int,
        contentType: DiffView.ContentType,
        comments: List<Comment>
    ) {
        displayCommentsOnLine(providerData, mergeRequest, viewer.editor, visibleLine - 1, comments)
    }
}