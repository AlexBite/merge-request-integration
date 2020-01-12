package net.ntworld.mergeRequestIntegrationIde.ui.mergeRequest.tab.commit

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.ChangesTreeImpl
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder
import com.intellij.ui.ScrollPaneFactory
import net.miginfocom.swing.MigLayout
import net.ntworld.mergeRequest.Commit
import net.ntworld.mergeRequest.MergeRequestInfo
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.ui.service.DisplayChangesService
import net.ntworld.mergeRequestIntegrationIde.ui.util.CustomSimpleToolWindowPanel
import net.ntworld.mergeRequestIntegrationIde.ui.util.ToolbarUtil
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.tree.DefaultTreeModel

class CommitChanges(private val ideaProject: IdeaProject) : CommitChangesUI {
    private val myComponent = CustomSimpleToolWindowPanel(vertical = true, borderless = true)
    private val myTree = MyTree(ideaProject)

    init {
        myComponent.setContent(ScrollPaneFactory.createScrollPane(myTree, true))
        myComponent.toolbar = createToolbar()
    }

    override fun clear() {
        ApplicationManager.getApplication().invokeLater {
            myTree.setChangesToDisplay(listOf())
        }
    }

    override fun disable() {
        myComponent.isVisible = false
    }

    override fun enable() {
        myComponent.isVisible = true
    }

    override fun setCommits(providerData: ProviderData, mergeRequestInfo: MergeRequestInfo, commits: Collection<Commit>) {
        myTree.isVisible = false
        ApplicationManager.getApplication().invokeLater {
            val changes = DisplayChangesService.findChanges(ideaProject, providerData, commits.map { it.id })
            myTree.setChangesToDisplay(changes)
            myTree.isVisible = true
        }
    }

    override fun createComponent(): JComponent = myComponent

    private fun createToolbar(): JComponent {
        val panel = JPanel(MigLayout("ins 0, fill", "[left]push[right]", "center"))

        val leftActionGroup = DefaultActionGroup()
        val leftToolbar = ActionManager.getInstance().createActionToolbar(
            "${CommitChanges::class.java.canonicalName}/toolbar-left",
            leftActionGroup,
            true
        )

        panel.add(leftToolbar.component)
        panel.add(
            ToolbarUtil.createExpandAndCollapseToolbar(
                "${CommitChanges::class.java.canonicalName}/toolbar-right",
                myTree
            )
        )
        return panel
    }

    private class MyTree(ideaProject: IdeaProject) : ChangesTreeImpl<Change>(
        ideaProject, false, false, Change::class.java
    ) {
        override fun buildTreeModel(changes: MutableList<out Change>): DefaultTreeModel {
            return TreeModelBuilder.buildFromChanges(myProject, grouping, changes, null)
        }
    }
}