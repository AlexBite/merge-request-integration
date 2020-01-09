package net.ntworld.mergeRequestIntegrationIde.ui.panel

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorSettingsProvider
import com.intellij.ui.EditorTextField
import com.intellij.util.EventDispatcher
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequest.MergeRequest
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequest.command.CreateCommentCommand
import net.ntworld.mergeRequest.command.ReplyCommentCommand
import net.ntworld.mergeRequestIntegration.internal.CommentImpl
import net.ntworld.mergeRequestIntegration.internal.CommentPositionImpl
import net.ntworld.mergeRequestIntegration.make
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService
import net.ntworld.mergeRequestIntegrationIde.service.CommentStore
import net.ntworld.mergeRequestIntegrationIde.ui.Component
import java.lang.Exception
import java.util.*
import javax.swing.*

class CommentEditorPanel(
    private val ideaProject: Project,
    private val providerData: ProviderData,
    private val mergeRequest: MergeRequest,
    private val comment: Comment?,
    private val item: CommentStore.Item
) : Component {
    var myWholePanel: JPanel? = null
    var myEditorWrapper: JPanel? = null
    var myDebugInfoWrapper: JPanel? = null
    var myAddDiffComment: JCheckBox? = null
    var myOkButton: JButton? = null
    var myCancelButton: JButton? = null
    var myOldHash: JTextField? = null
    var myOldLine: JTextField? = null
    var myOldPath: JLabel? = null
    var myNewHash: JTextField? = null
    var myNewLine: JTextField? = null
    var myNewPath: JLabel? = null

    private val dispatcher = EventDispatcher.create(Listener::class.java)
    private val myLanguage = Language.findInstancesByMimeType("text/x-markdown")
    private val myFileType = object : LanguageFileType(myLanguage.first()) {
        override fun getIcon(): Icon? = null

        override fun getName(): String {
            return "Markdown"
        }

        override fun getDefaultExtension(): String {
            return "md"
        }

        override fun getDescription(): String {
            return "Markdown"
        }
    }

    private val myDocument = DocumentImpl("")
    private val myEditorSettingsProvider = object : EditorSettingsProvider {
        override fun customizeSettings(editor: EditorEx?) {
            if (null !== editor) {
                editor.settings.isLineNumbersShown = true
                editor.settings.isFoldingOutlineShown = true

                editor.setHorizontalScrollbarVisible(true)
                editor.setVerticalScrollbarVisible(true)
            }
        }
    }
    private val myEditorTextField by lazy {
        val textField = EditorTextField(myDocument, ideaProject, myFileType)
        textField.setOneLineMode(false)
        textField.addSettingsProvider(myEditorSettingsProvider)
        textField
    }

    init {
        myEditorWrapper!!.add(myEditorTextField)
        myEditorTextField.text = item.body
        myOkButton!!.text = when (item.type) {
            CommentStore.ItemType.EDIT -> "Update"
            CommentStore.ItemType.NEW -> "Create"
            CommentStore.ItemType.REPLY -> "Reply"
        }
        when (item.type) {
            CommentStore.ItemType.EDIT -> initUpdateComment()
            CommentStore.ItemType.NEW -> initCreateComment()
            CommentStore.ItemType.REPLY -> initReplyComment()
        }
        myOkButton!!.addActionListener {
            when (item.type) {
                CommentStore.ItemType.EDIT -> updateComment()
                CommentStore.ItemType.NEW -> createComment()
                CommentStore.ItemType.REPLY -> replyComment()
            }
        }
        myCancelButton!!.addActionListener {
            dispatcher.multicaster.onDestroyRequested(providerData, mergeRequest, comment, item)
        }
        myAddDiffComment!!.addActionListener {
            myDebugInfoWrapper!!.isVisible = myAddDiffComment!!.isVisible && myAddDiffComment!!.isSelected
        }
    }

    private fun initUpdateComment() {}
    private fun updateComment() {}

    private fun initCreateComment() {
        val position = item.position
        if (null !== position) {
            myAddDiffComment!!.isSelected = true
            myOldHash!!.text = position.baseHash
            myNewHash!!.text = position.headHash
            myNewLine!!.text = findLine(position.newLine)
            myOldLine!!.text = findLine(position.oldLine)
            myNewPath!!.text = if (null !== position.newPath) position.newPath else ""
            myOldPath!!.text = if (null !== position.oldPath) position.oldPath else ""
        }
        showDebugInfo()
    }

    private fun findLine(line: Int?): String {
        if (null === line) {
            return ""
        }
        return if (line > 0) line.toString() else ""
    }

    private fun createComment() {
        if (!myAddDiffComment!!.isSelected) {
            try {
                ApplicationService.instance.infrastructure.commandBus() process CreateCommentCommand.make(
                    providerId = providerData.id,
                    mergeRequestId = mergeRequest.id,
                    position = null,
                    body = myEditorTextField.text
                )
                dispatcher.multicaster.onDestroyRequested(providerData, mergeRequest, comment, item)
            } catch (exception: Exception) {
            }
        }

        val position = item.position
        if (null !== position) {
            try {
                ApplicationService.instance.infrastructure.commandBus() process CreateCommentCommand.make(
                    providerId = providerData.id,
                    mergeRequestId = mergeRequest.id,
                    position = position,
                    body = myEditorTextField.text
                )
                dispatcher.multicaster.onDestroyRequested(providerData, mergeRequest, comment, item)
            } catch (exception: Exception) {
            }
        }
    }

    private fun initReplyComment() {
        hideDebugInfo()
    }

    private fun replyComment() {
        val repliedComment = comment
        if (null !== repliedComment) {
            ApplicationService.instance.infrastructure.commandBus() process ReplyCommentCommand.make(
                providerId = providerData.id,
                mergeRequestId = mergeRequest.id,
                repliedComment = repliedComment,
                body = myEditorTextField.text
            )
            dispatcher.multicaster.onDestroyRequested(providerData, mergeRequest, comment, item)
        }
    }

    fun addDestroyListener(listener: Listener) {
        dispatcher.addListener(listener)
    }

    private fun showDebugInfo() {
        myAddDiffComment!!.isVisible = true
        myDebugInfoWrapper!!.isVisible = true
    }

    private fun hideDebugInfo() {
        myAddDiffComment!!.isVisible = false
        myDebugInfoWrapper!!.isVisible = false
    }

    fun grabFocus() {
        ApplicationManager.getApplication().invokeLater {
            Thread.sleep(100)
            myEditorTextField.grabFocus()
        }
    }

    override fun createComponent(): JComponent = myWholePanel!!

    interface Listener : EventListener {
        fun onDestroyRequested(
            providerData: ProviderData,
            mergeRequest: MergeRequest,
            comment: Comment?,
            item: CommentStore.Item
        )
    }
}
