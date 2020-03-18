package net.ntworld.mergeRequestIntegrationIde.diff.gutter

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import net.ntworld.mergeRequestIntegrationIde.diff.DiffView
import net.ntworld.mergeRequestIntegrationIde.ui.util.Icons
import com.intellij.openapi.editor.markup.GutterIconRenderer as GutterIconRendererClass

class GutterIconRendererImpl(
    private val showAddIcon: Boolean,
    override val visibleLine: Int,
    override val logicalLine: Int,
    override val contentType: DiffView.ContentType,
    private val action: ((GutterIconRenderer, GutterActionType) -> Unit)
) : GutterIconRenderer, GutterIconRendererClass() {
    private var icon = if (showAddIcon) Icons.Gutter.AddComment else Icons.Gutter.Empty
    private var desc = ""

    private val clickAction = object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            if (icon == Icons.Gutter.AddComment || icon == Icons.Gutter.Empty) {
                action.invoke(this@GutterIconRendererImpl, GutterActionType.ADD)
            } else {
                action.invoke(this@GutterIconRendererImpl, GutterActionType.TOGGLE)
            }
        }
    }

    override fun setState(state: GutterState) {
        when (state) {
            GutterState.NO_COMMENT -> {
                icon = if (showAddIcon) Icons.Gutter.Empty else Icons.Gutter.AddComment
                desc = if (showAddIcon) "Add new comment" else ""
            }
            GutterState.COMMENTS_FROM_ONE_AUTHOR -> {
                icon = Icons.Gutter.Comment
                desc = "Toggle comment thread"
            }
            GutterState.COMMENTS_FROM_MULTI_AUTHORS -> {
                icon = Icons.Gutter.Comments
                desc = "Toggle comment thread"
            }
            GutterState.WRITING -> {
                icon = Icons.Gutter.WritingComment
                desc = "Continue writing your comment"
            }
        }
    }

    override fun triggerAddAction() {
        action.invoke(this, GutterActionType.ADD)
    }

    override fun triggerToggleAction() {
        action.invoke(this, GutterActionType.TOGGLE)
    }

    override fun getClickAction() = clickAction
    override fun getIcon() = icon
    override fun isNavigateAction() = icon != Icons.Gutter.Empty
    override fun hashCode(): Int = System.identityHashCode(this)
    override fun equals(other: Any?): Boolean = other == this
}
