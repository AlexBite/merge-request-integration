package net.ntworld.mergeRequestIntegrationIde.infrastructure.notifier.provider

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.MessageBus
import net.ntworld.mergeRequest.Comment
import net.ntworld.mergeRequest.MergeRequestInfo
import net.ntworld.mergeRequest.ProviderData
import net.ntworld.mergeRequestIntegrationIde.infrastructure.ProjectServiceProvider
import net.ntworld.mergeRequestIntegrationIde.infrastructure.notifier.MergeRequestDataNotifier
import net.ntworld.mergeRequestIntegrationIde.task.GetCommentsTask

class MergeRequestDataProvider(
    private val projectServiceProvider: ProjectServiceProvider,
    private val messageBus: MessageBus
) : MergeRequestDataNotifier {
    private val getCommentsTaskListener = object : GetCommentsTask.Listener {
        override fun dataReceived(
            providerData: ProviderData, mergeRequestInfo: MergeRequestInfo, comments: List<Comment>
        ) {
            ApplicationManager.getApplication().invokeLater {
                messageBus.syncPublisher(MergeRequestDataNotifier.TOPIC)
                    .onCommentsUpdated(providerData, mergeRequestInfo, comments)
            }
        }
    }

    override fun fetchCommentsRequested(providerData: ProviderData, mergeRequestInfo: MergeRequestInfo) {
        val task = GetCommentsTask(
            projectServiceProvider,
            providerData,
            mergeRequestInfo,
            getCommentsTaskListener
        )
        task.start()
    }

    override fun onCommentsUpdated(providerData: ProviderData, mergeRequestInfo: MergeRequestInfo, comments: List<Comment>) {
    }
}