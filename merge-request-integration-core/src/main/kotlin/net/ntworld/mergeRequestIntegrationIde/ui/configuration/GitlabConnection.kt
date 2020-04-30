package net.ntworld.mergeRequestIntegrationIde.ui.configuration

import com.intellij.openapi.ui.Messages
import com.intellij.util.EventDispatcher
import git4idea.repo.GitRepository
import net.ntworld.mergeRequest.Project
import net.ntworld.mergeRequest.api.ApiConnection
import net.ntworld.mergeRequest.api.ApiCredentials
import net.ntworld.mergeRequestIntegration.provider.gitlab.GitlabUtil
import net.ntworld.mergeRequestIntegrationIde.infrastructure.ProjectServiceProvider
import net.ntworld.mergeRequestIntegrationIde.infrastructure.internal.ApiCredentialsImpl
import net.ntworld.mergeRequestIntegrationIde.util.RepositoryUtil
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class GitlabConnection(
    private val projectServiceProvider: ProjectServiceProvider
) : ConnectionUI {
    var myWholePanel: JPanel? = null
    var mySettingsPanel: JPanel? = null
    var myName: JTextField? = null
    var myUrl: JTextField? = null
    var myToken: JPasswordField? = null
    var myTestBtn: JButton? = null
    var myDeleteBtn: JButton? = null
    var myRepository: JComboBox<String>? = null
    var myTerm: JTextField? = null
    var myProjectList: JList<Project>? = null
    var myShared: JCheckBox? = null
    var mySearchStarred: JCheckBox? = null
    var mySearchMembership: JCheckBox? = null
    var mySearchOwn: JCheckBox? = null
    var myMergeApprovalsFeature: JCheckBox? = null
    var myIgnoreSSLError: JCheckBox? = null

    private var myCurrentName: String = ""
    private var mySelectedRepository: String? = null
    private var mySelectedProject: Project? = null
    private var myIsTested: Boolean = false
    private val myProjectFinder: ProjectFinderUI by lazy {
        GitlabProjectFinder(
            projectServiceProvider,
            myTerm!!,
            myProjectList!!,
            mySearchStarred!!,
            mySearchMembership!!,
            mySearchOwn!!,
            myMergeApprovalsFeature!!
        )
    }
    private val myNameFieldListener = object : DocumentListener {
        override fun changedUpdate(e: DocumentEvent?) {
            val newName = myName!!.text
            if (newName.trim() != myCurrentName) {
                dispatcher.multicaster.changeName(this@GitlabConnection, myCurrentName, newName)
                myCurrentName = newName.trim()
            }
        }

        override fun insertUpdate(e: DocumentEvent?) = changedUpdate(e)

        override fun removeUpdate(e: DocumentEvent?) = changedUpdate(e)
    }
    private val myConnectionFieldsListener = object : DocumentListener {
        override fun changedUpdate(e: DocumentEvent?) {
            myTestBtn!!.isEnabled = myName!!.text.isNotEmpty() && myUrl!!.text.isNotEmpty() && getToken().isNotEmpty()
            updateConnectionData()
        }

        override fun insertUpdate(e: DocumentEvent?) = changedUpdate(e)

        override fun removeUpdate(e: DocumentEvent?) = changedUpdate(e)
    }

    override val dispatcher = EventDispatcher.create(ConnectionUI.Listener::class.java)

    init {
        val repositories = RepositoryUtil.getRepositoriesByProject(projectServiceProvider.project)
        repositories.forEach { myRepository!!.addItem(it) }
        if (repositories.isNotEmpty()) {
            myRepository!!.selectedItem = repositories.first()
        }

        myTestBtn!!.addActionListener { onTestClicked() }
        myDeleteBtn!!.addActionListener { onDeleteClicked() }

        myName!!.document.addDocumentListener(myNameFieldListener)
        myName!!.document.addDocumentListener(myConnectionFieldsListener)
        myUrl!!.document.addDocumentListener(myConnectionFieldsListener)
        myToken!!.document.addDocumentListener(myConnectionFieldsListener)

        myIgnoreSSLError!!.addChangeListener { updateConnectionData() }
        myRepository!!.addActionListener {
            requestGuessProjectByCurrentRepository()
            updateConnectionData()
        }
        myProjectFinder.addProjectChangedListener(object: ProjectFinderUI.ProjectChangedListener {
            override fun projectChanged(projectId: String) {
                updateConnectionData()
            }
        })
        myMergeApprovalsFeature!!.addChangeListener { updateConnectionData() }
    }

    override fun initialize(name: String, credentials: ApiCredentials, shared: Boolean, repository: String) {
        myIsTested = true
        myCurrentName = name.trim()
        myName!!.text = name.trim()
        myUrl!!.text = if (credentials.url.isNotEmpty()) credentials.url else "https://gitlab.com"
        myToken!!.text = credentials.token
        myIgnoreSSLError!!.isSelected = credentials.ignoreSSLCertificateErrors
        myShared!!.isSelected = shared
        mySelectedRepository = repository
        myTestBtn!!.isEnabled = false

        myMergeApprovalsFeature!!.isSelected = GitlabUtil.hasMergeApprovalFeature(credentials)
        if (credentials.projectId.isNotEmpty()) {
            dispatcher.multicaster.verify(this, name, credentials, repository)
        }
        updateFieldsState()
    }

    override fun setName(name: String) {
        myName!!.text = name.trim()
        myUrl!!.text = "https://gitlab.com"
        myCurrentName = name.trim()
    }

    override fun onConnectionTested(name: String, connection: ApiConnection, shared: Boolean) {
        myIsTested = true
        myTestBtn!!.isEnabled = false
        updateFieldsState()
        requestGuessProjectByCurrentRepository()
        Messages.showInfoMessage("Successfully connected!", "Info")
    }

    override fun onProjectGuessed(repository: GitRepository, project: Project?) {
        if (null !== project && null === mySelectedProject) {
            mySelectedProject = project
            myProjectFinder.setSelectedProject(mySelectedProject)
        }
    }

    override fun onConnectionError(name: String, connection: ApiConnection, shared: Boolean, exception: Exception) {
        myIsTested = false
        Messages.showErrorDialog("Cannot connect, error: ${exception.message}", "Error")
        updateFieldsState()
    }

    override fun onCredentialsVerified(
        name: String, credentials: ApiCredentials, repository: String, project: Project
    ) {
        mySelectedProject = project
        mySelectedRepository = repository
        updateFieldsState()
    }

    override fun onCredentialsInvalid(name: String, credentials: ApiCredentials, repository: String) {
        updateFieldsState()
    }

    override fun createComponent(): JComponent = myWholePanel!!

    private fun requestGuessProjectByCurrentRepository() {
        val repository = RepositoryUtil.findRepositoryByPath(
            projectServiceProvider.project,
            myRepository!!.selectedItem as String? ?: ""
        )
        if (null !== repository) {
            this.dispatcher.multicaster.guessProject(this, buildApiCredentials(), repository)
        }
    }

    private fun buildApiCredentials(): ApiCredentials {
        return ApiCredentialsImpl(
            url = myUrl!!.text.trim(),
            login = "",
            token = getToken().trim(),
            projectId = myProjectFinder.getSelectedProjectId(),
            version = "v4",
            info = if (myMergeApprovalsFeature!!.isSelected) GitlabUtil.getMergeApprovalFeatureInfo() else "",
            ignoreSSLCertificateErrors = myIgnoreSSLError!!.isSelected
        )
    }

    private fun updateConnectionData() {
        dispatcher.multicaster.update(
            this,
            myName!!.text.trim(),
            buildApiCredentials(),
            myShared!!.isSelected,
            myRepository!!.selectedItem as String? ?: ""
        )
    }

    private fun getToken(): String {
        return String(myToken!!.password)
    }

    private fun updateFieldsState() {
        myRepository!!.isEnabled = myIsTested
        myProjectFinder.setEnabled(
            myIsTested,
            ApiCredentialsImpl(
                url = myUrl!!.text.trim(),
                login = "",
                token = getToken().trim(),
                projectId = "",
                version = "v4",
                info = if (myMergeApprovalsFeature!!.isSelected) GitlabUtil.getMergeApprovalFeatureInfo() else "",
                ignoreSSLCertificateErrors = myIgnoreSSLError!!.isSelected
            )
        )
        if (null !== mySelectedRepository) {
            myRepository!!.selectedItem = mySelectedRepository
        }
        if (null !== mySelectedProject) {
            myProjectFinder.setSelectedProject(mySelectedProject)
        }
    }

    private fun onDeleteClicked() {
        val name = myName!!.text
        this.dispatcher.multicaster.delete(this, name)
    }

    private fun onTestClicked() {
        val name = myName!!.text
        val url = myUrl!!.text
        val token = getToken()
        val shared = myShared!!.isSelected
        val ignoreSSLCertificateErrors = myIgnoreSSLError!!.isSelected
        if (name.isEmpty() || url.isEmpty() || token.isEmpty()) {
            return
        }
        this.dispatcher.multicaster.test(
            this,
            name,
            ApiCredentialsImpl(
                url = url,
                login = "",
                token = token,
                ignoreSSLCertificateErrors = ignoreSSLCertificateErrors,
                info = "",
                projectId = "",
                version = "v4"
            ),
            shared
        )
    }
}
