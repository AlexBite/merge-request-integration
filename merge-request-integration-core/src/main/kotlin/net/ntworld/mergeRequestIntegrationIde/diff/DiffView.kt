package net.ntworld.mergeRequestIntegrationIde.diff

import com.intellij.diff.FrameDiffTool
import com.intellij.openapi.actionSystem.AnActionEvent
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequest.MergeRequest
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.View
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.AddGutterIconRenderer
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.CommentsGutterIconRenderer
import java.util.*

interface DiffView<V : FrameDiffTool.DiffViewer> : View<DiffView.Action> {
    val viewer: V

    fun initialize()

    fun displayAddGutterIcons()

    fun displayCommentsGutterIcon(visibleLine: Int, contentType: ContentType, comments: List<Comment>)

    fun toggleCommentsOnLine(
        providerData: ProviderData,
        visibleLine: Int,
        logicalLine: Int,
        contentType: ContentType,
        comments: List<Comment>
    )

    enum class EditorType {
        SINGLE_SIDE,
        TWO_SIDE_LEFT,
        TWO_SIDE_RIGHT,
        UNIFIED
    }

    enum class ContentType {
        BEFORE,
        AFTER
    }

    enum class ChangeType {
        UNKNOWN,
        INSERTED,
        DELETED,
        MODIFIED
    }

    interface Action : EventListener {
        fun onInit()

        fun onDispose()

        fun onBeforeRediff()

        fun onAfterRediff()

        fun onRediffAborted()

        fun onAddGutterIconClicked(renderer: AddGutterIconRenderer, position: AddCommentRequestedPosition)

        fun onCommentsGutterIconClicked(renderer: CommentsGutterIconRenderer, e: AnActionEvent)
    }
}
