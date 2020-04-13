package net.ntworld.mergeRequestIntegrationIde.mergeRequest.comments.tree

import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequestIntegrationIde.Component
import net.ntworld.mergeRequestIntegrationIde.View
import net.ntworld.mergeRequestIntegrationIde.mergeRequest.comments.tree.node.Node
import java.util.*

interface CommentTreeView : View<CommentTreeView.ActionListener>, Component {

    fun renderTree(comments: List<Comment>)

    fun setShowResolvedCommentState(selected: Boolean)

    interface ActionListener : EventListener {
        fun onTreeNodeSelected(node: Node)

        fun onShowResolvedCommentsToggled(displayResolvedComments: Boolean)

        fun onCreateGeneralCommentClicked()

        fun onRefreshButtonClicked()
    }
}

//data class SingleCommentTreeNode(
//    val commentId: String,
//    val position: CommentPosition?,
//    override val parent: CommentTreeNode,
//    override val children: List<CommentTreeNode> = listOf()
//) : CommentTreeNode
//
//data class CommentEditorTreeNode(
//    val isGeneral: Boolean,
//    val isReply: Boolean,
//    val repliedCommentId: String?,
//    val position: CommentPosition?,
//    override val parent: CommentTreeNode,
//    override val children: List<CommentTreeNode> = listOf()
//) : CommentTreeNode
