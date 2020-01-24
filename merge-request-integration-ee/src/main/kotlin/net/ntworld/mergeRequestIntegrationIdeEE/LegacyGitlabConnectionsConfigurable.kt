package net.ntworld.mergeRequestIntegrationIdeEE

import com.intellij.openapi.project.Project
import net.ntworld.mergeRequestIntegrationIde.ui.configuration.gitlab.GitlabConnectionsConfigurableBase

class LegacyGitlabConnectionsConfigurable(myIdeaProject: Project) : GitlabConnectionsConfigurableBase(myIdeaProject) {
    override fun getId(): String {
        return "merge-request-integration-ee-gitlab-connections"
    }

    override fun getDisplayName(): String {
        return "Gitlab"
    }
}