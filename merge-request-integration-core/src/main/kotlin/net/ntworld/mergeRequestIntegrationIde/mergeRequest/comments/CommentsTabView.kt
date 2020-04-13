package net.ntworld.mergeRequestIntegrationIde.mergeRequest.comments

import com.intellij.openapi.Disposable
import com.intellij.ui.tabs.TabInfo
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequestIntegrationIde.Component
import net.ntworld.mergeRequestIntegrationIde.View
import java.util.*

interface CommentsTabView : View<CommentsTabView.ActionListener>, Component, Disposable {
    val tabInfo: TabInfo

    fun displayCommentCount(count: Int)

    fun renderTree(comments: List<Comment>, displayResolvedComments: Boolean)

    interface ActionListener : EventListener {

    }
}