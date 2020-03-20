package net.ntworld.mergeRequestIntegrationIde.diff.thread

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.EditorEmbeddedComponentManager
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.view.FontLayoutService
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.Panel
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBUI
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequest.MergeRequest
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.diff.AddCommentRequestedPosition
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class ThreadViewImpl(
    private val editor: EditorEx,
    private val providerData: ProviderData,
    private val mergeRequest: MergeRequest,
    private val logicalLine: Int,
    private val position: AddCommentRequestedPosition
) : ThreadView, EditorComponent.Event {
    override val dispatcher = EventDispatcher.create(ThreadView.Action::class.java)

    private val myThreadPanel = Panel()
    private val myBoxLayoutPanel = JBUI.Panels.simplePanel()
    private val myWrapper = ComponentWrapper(myBoxLayoutPanel)
    private val myEditorWidthWatcher = EditorTextWidthWatcher()
    private val resizePolicy by lazy {
        val constructors = EditorEmbeddedComponentManager.ResizePolicy::class.java.declaredConstructors
        for (ctor in constructors) {
            ctor.isAccessible = true
            return@lazy ctor.newInstance(0) as EditorEmbeddedComponentManager.ResizePolicy
        }
        return@lazy EditorEmbeddedComponentManager.ResizePolicy.any()
    }
    private val myCreatedEditors = mutableMapOf<String, EditorComponent>()
    private val myGroups = mutableMapOf<String, GroupComponent>()
    private val myEditor by lazy {
        val editorComponent = ComponentFactory.makeEditor(editor.project!!, EditorComponent.Type.NEW_DISCUSSION, 0)
        editorComponent.isVisible = false
        editorComponent.dispatcher.addListener(this)
        Disposer.register(this@ThreadViewImpl, editorComponent)

        myCreatedEditors[""] = editorComponent
        myBoxLayoutPanel.addToBottom(editorComponent.component)
        editorComponent
    }
    private val myGroupComponentEventListener = object : GroupComponent.Event {
        override fun onEditorCreated(groupId: String, editor: EditorComponent) {
            editor.dispatcher.addListener(this@ThreadViewImpl)
            Disposer.register(this@ThreadViewImpl, editor)

            myCreatedEditors[groupId] = editor
        }

        override fun onEditorDestroyed(groupId: String, editor: EditorComponent) {
            myCreatedEditors.remove(groupId)
        }
    }

    init {
        myBoxLayoutPanel.addToCenter(myThreadPanel)
        myThreadPanel.layout = BoxLayout(myThreadPanel, BoxLayout.Y_AXIS)

        myWrapper.isVisible = false
    }

    override val isEditorDisplayed: Boolean
        get() = myEditor.isVisible

    override fun initialize() {
        editor.scrollPane.viewport.addComponentListener(myEditorWidthWatcher)
        Disposer.register(this, Disposable {
            editor.scrollPane.viewport.removeComponentListener(myEditorWidthWatcher)
        })

        val editorEmbeddedComponentManager = EditorEmbeddedComponentManager.getInstance()
        val offset = editor.document.getLineEndOffset(logicalLine)
        editorEmbeddedComponentManager.addComponent(
            editor,
            myWrapper,
            EditorEmbeddedComponentManager.Properties(
                resizePolicy,
                true,
                false,
                0,
                offset
            )
        )

        EditorUtil.disposeWithEditor(editor, this)
    }

    override fun dispose() {
        editor.scrollPane.viewport.removeComponentListener(myEditorWidthWatcher)
        myCreatedEditors.values.forEach { it.dispose() }
    }

    override fun addGroupOfComments(groupId: String, comments: List<Comment>) {
        val group = ComponentFactory.makeGroup(
            providerData, mergeRequest, editor.project!!, myGroups.isEmpty(), groupId, comments
        )
        group.dispatcher.addListener(myGroupComponentEventListener)
        Disposer.register(this, group)

        myGroups[groupId] = group
        myThreadPanel.add(group.component)
    }

//    override fun showReplyEditorForGroup(groupId: String) {
//        val group = myGroups[groupId]
//        if (null !== group) {
//            group.showReplyEditor()
//        }
//    }

    override fun showEditor() {
        myEditor.isVisible = true
        myEditorWidthWatcher.updateWidthForAllInlays()
    }

    override fun show() {
        myWrapper.isVisible = true
        myEditorWidthWatcher.updateWidthForAllInlays()
    }

    override fun hide() {
        myWrapper.isVisible = false
        myEditorWidthWatcher.updateWidthForAllInlays()
    }

    override fun onEditorResized() {
        myEditorWidthWatcher.updateWidthForAllInlays()
    }

    private inner class ComponentWrapper(private val component: JComponent) : JBScrollPane(component) {
        init {
            isOpaque = false
            viewport.isOpaque = false

            border = JBUI.Borders.empty()
            viewportBorder = JBUI.Borders.empty()

            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBar.preferredSize = Dimension(0, 0)
            setViewportView(component)

            component.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    return dispatchEvent(ComponentEvent(component, ComponentEvent.COMPONENT_RESIZED))
                }
            })
        }

        override fun getPreferredSize(): Dimension {
            return Dimension(
                myEditorWidthWatcher.editorTextWidth,
                if (component.isVisible) component.preferredSize.height else 0
            )
        }
    }

    private inner class EditorTextWidthWatcher : ComponentAdapter() {
        var editorTextWidth: Int = 0

        private val maximumEditorTextWidth: Int
        private val verticalScrollbarFlipped: Boolean

        init {
            val metrics = (editor as EditorImpl).getFontMetrics(Font.PLAIN)
            val spaceWidth = FontLayoutService.getInstance().charWidth2D(metrics, ' '.toInt())
            maximumEditorTextWidth = ceil(spaceWidth * (editor.settings.getRightMargin(editor.project)) - 1).toInt()

            val scrollbarFlip = editor.scrollPane.getClientProperty(JBScrollPane.Flip::class.java)
            verticalScrollbarFlipped = scrollbarFlip == JBScrollPane.Flip.HORIZONTAL ||
                scrollbarFlip == JBScrollPane.Flip.BOTH
        }

        override fun componentResized(e: ComponentEvent) = updateWidthForAllInlays()
        override fun componentHidden(e: ComponentEvent) = updateWidthForAllInlays()
        override fun componentShown(e: ComponentEvent) = updateWidthForAllInlays()

        fun updateWidthForAllInlays() {
            val newWidth = calcWidth()
            editorTextWidth = newWidth

            myWrapper.dispatchEvent(ComponentEvent(myWrapper, ComponentEvent.COMPONENT_RESIZED))
            myWrapper.invalidate()
        }

        private fun calcWidth(): Int {
            val visibleEditorTextWidth =
                editor.scrollPane.viewport.width - getVerticalScrollbarWidth() - getGutterTextGap()
            return min(max(visibleEditorTextWidth, 0), maximumEditorTextWidth)
        }

        private fun getVerticalScrollbarWidth(): Int {
            val width = editor.scrollPane.verticalScrollBar.width
            return if (!verticalScrollbarFlipped) width * 2 else width
        }

        private fun getGutterTextGap(): Int {
            return if (verticalScrollbarFlipped) {
                val gutter = editor.gutterComponentEx
                gutter.width - gutter.whitespaceSeparatorOffset
            } else 0
        }
    }
}