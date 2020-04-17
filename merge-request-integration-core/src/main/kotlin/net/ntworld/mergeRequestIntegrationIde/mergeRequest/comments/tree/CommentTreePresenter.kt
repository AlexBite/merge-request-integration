package net.ntworld.mergeRequestIntegrationIde.mergeRequest.comments.tree

import net.ntworld.mergeRequestIntegrationIde.Component
import net.ntworld.mergeRequestIntegrationIde.Presenter
import javax.swing.JComponent

interface CommentTreePresenter : Presenter<CommentTreePresenter.Listener>, Component {
    val model: CommentTreeModel

    val view: CommentTreeView

    override val component: JComponent
        get() = view.component

    interface Listener : CommentTreeView.ActionListener
}