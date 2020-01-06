package net.ntworld.mergeRequestIntegration.provider.gitlab.request

import net.ntworld.foundation.Request
import net.ntworld.mergeRequest.api.ApiCredentials
import net.ntworld.mergeRequestIntegration.provider.gitlab.GitlabRequest
import net.ntworld.mergeRequestIntegration.provider.gitlab.response.GitlabFindMRResponse

// FIXME: projectId, mergeRequestInternalId in this package belong to Gitlab, then it should be an integer not String
data class GitlabFindMRRequest(
    override val credentials: ApiCredentials,
    val mergeRequestInternalId: String
): GitlabRequest, Request<GitlabFindMRResponse>