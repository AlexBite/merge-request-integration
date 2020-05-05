package net.ntworld.mergeRequestIntegrationIde.component.comment

import com.intellij.openapi.Disposable
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequestIntegrationIde.Component

interface GroupComponent : Component, Disposable {
    val id: String

    var comments: List<Comment>

    var collapse: Boolean

    fun requestOpenDialog()

    fun requestDeleteComment(comment: Comment)

    fun requestToggleResolvedStateOfComment(comment: Comment)

    fun resetReplyEditor()

    fun showReplyEditor()

    fun destroyReplyEditor()

    fun addListener(listener: EventListener)

    fun hideMoveToDialogButtons()

    fun showMoveToDialogButtons()

    interface EventListener : java.util.EventListener, CommentEvent {
        fun onResized()

        fun onOpenDialogClicked()

        fun onEditorCreated(groupId: String, editor: EditorComponent)

        fun onEditorDestroyed(groupId: String, editor: EditorComponent)

        fun onReplyCommentRequested(comment: Comment, content: String)
    }
}