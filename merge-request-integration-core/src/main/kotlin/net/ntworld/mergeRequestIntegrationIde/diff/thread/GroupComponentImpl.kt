package net.ntworld.mergeRequestIntegrationIde.diff.thread

import com.intellij.openapi.project.Project as IdeaProject
import com.intellij.ui.JBColor
import com.intellij.ui.components.Panel
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBUI
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequest.MergeRequest
import net.ntworld.mergeRequest.ProviderData
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JComponent

class GroupComponentImpl(
    private val borderTop: Boolean,
    private val providerData: ProviderData,
    private val mergeRequest: MergeRequest,
    private val project: IdeaProject,
    override val id: String,
    override val comments: List<Comment>
) : GroupComponent {
    private val myBoxLayoutPanel = JBUI.Panels.simplePanel()
    private val myPanel = Panel()
    private var myEditor: EditorComponent? = null

    init {
        myPanel.layout = BoxLayout(myPanel, BoxLayout.Y_AXIS)
        if (borderTop) {
            myPanel.border = BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border())
        }
        myBoxLayoutPanel.addToCenter(myPanel)

        comments.forEachIndexed { index, comment ->
            myPanel.add(
                ComponentFactory
                    .makeComment(this, providerData, mergeRequest, comment, if (index == 0) 0 else 1)
                    .component
            )
        }

    }

    override val dispatcher = EventDispatcher.create(GroupComponent.Event::class.java)

    override val component: JComponent = myBoxLayoutPanel

    override fun showReplyEditor() {
        val editor = myEditor
        if (null === editor) {
            val createdEditor = ComponentFactory.makeEditor(project, EditorComponent.Type.REPLY, 1)
            dispatcher.multicaster.onEditorCreated(this.id, createdEditor)

            myBoxLayoutPanel.addToBottom(createdEditor.component)
            createdEditor.focus()
        } else {
            editor.focus()
        }
        myEditor = editor
    }

    override fun destroyReplyEditor() {
        val editor = myEditor
        if (null !== editor) {
            myBoxLayoutPanel.remove(editor.component)
            dispatcher.multicaster.onEditorDestroyed(this.id, editor)
            myEditor = null
        }
    }

    override fun dispose() {
        dispatcher.listeners.clear()
    }
}