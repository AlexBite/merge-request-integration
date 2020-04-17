package net.ntworld.mergeRequestIntegrationIde.diff

import com.intellij.diff.util.Side
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes.Change
import com.intellij.util.EventDispatcher
import git4idea.repo.GitRepository
import net.ntworld.mergeRequest.*
import net.ntworld.mergeRequest.command.DeleteCommentCommand
import net.ntworld.mergeRequest.command.ResolveCommentCommand
import net.ntworld.mergeRequest.command.UnresolveCommentCommand
import net.ntworld.mergeRequest.request.CreateCommentRequest
import net.ntworld.mergeRequest.request.ReplyCommentRequest
import net.ntworld.mergeRequestIntegration.internal.CommentPositionImpl
import net.ntworld.mergeRequestIntegration.make
import net.ntworld.mergeRequestIntegration.provider.ProviderException
import net.ntworld.mergeRequestIntegrationIde.AbstractPresenter
import net.ntworld.mergeRequestIntegrationIde.DataChangedSource
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.GutterActionType
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.GutterIconRenderer
import net.ntworld.mergeRequestIntegrationIde.diff.gutter.GutterPosition
import net.ntworld.mergeRequestIntegrationIde.infrastructure.ReviewContext
import net.ntworld.mergeRequestIntegrationIde.infrastructure.api.MergeRequestDataNotifier
import net.ntworld.mergeRequestIntegrationIde.service.ApplicationService
import net.ntworld.mergeRequestIntegrationIde.service.ProjectService
import net.ntworld.mergeRequestIntegrationIde.util.RepositoryUtil
import java.util.*

internal class DiffPresenterImpl(
    private val applicationService: ApplicationService,
    private val projectService: ProjectService,
    override val model: DiffModel,
    override val view: DiffView<*>
) : AbstractPresenter<EventListener>(),
    DiffPresenter, DiffView.ActionListener, DiffModel.DataListener, DiffNotifier {
    override val dispatcher = EventDispatcher.create(EventListener::class.java)

    init {
        view.addActionListener(this)
        model.addDataListener(this)
        model.messageBusConnection.subscribe(DiffNotifier.TOPIC, this)
        Disposer.register(model, this)
    }

    override fun dispose() {
        view.dispose()
    }

    override fun onInit() {}
    override fun onDispose() {}
    override fun onBeforeRediff() {}

    override fun onAfterRediff() {
        view.createGutterIcons()

        val before = groupCommentsByLine(model.commentsOnBeforeSide)
        for (item in before) {
            view.initializeLine(model.reviewContext, item.key, Side.LEFT, item.value)
        }

        val after = groupCommentsByLine(model.commentsOnAfterSide)
        for (item in after) {
            view.initializeLine(model.reviewContext, item.key, Side.RIGHT, item.value)
        }

        if (applicationService.settings.displayCommentsInDiffView) {
            view.showAllComments()
        }

        val scrollLine = model.reviewContext.getChangeData(model.change, DiffNotifier.ScrollLine)
        if (null !== scrollLine && scrollLine >= 0) {
            val side = model.reviewContext.getChangeData(model.change, DiffNotifier.ScrollSide)
            val showComments = model.reviewContext.getChangeData(model.change, DiffNotifier.ScrollShowComments)
            scrollToLine(scrollLine, side, showComments)
            clearChangeDataOfScrollToLineInReviewContext()
        }
    }

    override fun onRediffAborted() {}

    override fun onCommentsUpdated(source: DataChangedSource) {
        if (source == DataChangedSource.NOTIFIER) {
            ApplicationManager.getApplication().invokeLater {
                handleWhenCommentsGetUpdated(source)
            }
        } else {
            handleWhenCommentsGetUpdated(source)
        }
    }

    private fun handleWhenCommentsGetUpdated(source: DataChangedSource) {
        view.resetGutterIcons()
        val before = groupCommentsByLine(model.commentsOnBeforeSide)
        view.destroyExistingComments(before.keys, Side.LEFT)
        for (item in before) {
            view.initializeLine(model.reviewContext, item.key, Side.LEFT, item.value)
            view.updateComments(item.key, Side.LEFT, item.value)
        }

        val after = groupCommentsByLine(model.commentsOnAfterSide)
        view.destroyExistingComments(after.keys, Side.RIGHT)
        for (item in after) {
            view.initializeLine(model.reviewContext, item.key, Side.RIGHT, item.value)
            view.updateComments(item.key, Side.RIGHT, item.value)
        }
    }

    override fun onGutterActionPerformed(
        renderer: GutterIconRenderer, type: GutterActionType, mode: DiffView.DisplayCommentMode
    ) {
        when (type) {
            GutterActionType.ADD -> {
                view.prepareLine(model.reviewContext, renderer, collectCommentsOfGutterIconRenderer(renderer))
                view.displayEditorOnLine(renderer.logicalLine, renderer.side)
            }
            GutterActionType.TOGGLE -> {
                view.prepareLine(model.reviewContext, renderer, collectCommentsOfGutterIconRenderer(renderer))
                view.displayComments(renderer, mode)
            }
        }
    }

    override fun onReplyCommentRequested(content: String, repliedComment: Comment, logicalLine: Int, side: Side) {
        applicationService.infrastructure.serviceBus() process ReplyCommentRequest.make(
            providerId = model.providerData.id,
            mergeRequestId = model.mergeRequestInfo.id,
            repliedComment = repliedComment,
            body = content
        ) ifError {
            projectService.notify(
                "There was an error from server. \n\n ${it.message}",
                NotificationType.ERROR
            )
            throw ProviderException(it)
        }
        fetchAndUpdateComments()
        view.resetEditorOnLine(logicalLine, side, repliedComment)
    }

    override fun onCreateCommentRequested(content: String, position: GutterPosition, logicalLine: Int, side: Side) {
        val commentPosition = convertGutterPositionToCommentPosition(position)
        applicationService.infrastructure.serviceBus() process CreateCommentRequest.make(
            providerId = model.providerData.id,
            mergeRequestId = model.mergeRequestInfo.id,
            position = commentPosition,
            body = content
        ) ifError {
            projectService.notify(
                "There was an error from server. \n\n ${it.message}",
                NotificationType.ERROR
            )
            throw ProviderException(it)
        }
        fetchAndUpdateComments()
        view.resetEditorOnLine(logicalLine, side, null)
    }

    override fun onDeleteCommentRequested(comment: Comment) {
        applicationService.infrastructure.commandBus() process DeleteCommentCommand.make(
            providerId = model.providerData.id,
            mergeRequestId = model.mergeRequestInfo.id,
            comment = comment
        )
        fetchAndUpdateComments()
    }

    override fun onResolveCommentRequested(comment: Comment) {
        applicationService.infrastructure.commandBus() process ResolveCommentCommand.make(
            providerId = model.providerData.id,
            mergeRequestId = model.mergeRequestInfo.id,
            comment = comment
        )
        fetchAndUpdateComments()
    }

    override fun onUnresolveCommentRequested(comment: Comment) {
        applicationService.infrastructure.commandBus() process UnresolveCommentCommand.make(
            providerId = model.providerData.id,
            mergeRequestId = model.mergeRequestInfo.id,
            comment = comment
        )
        fetchAndUpdateComments()
    }

    override fun scrollToLineRequested(
        reviewContext: ReviewContext,
        change: Change,
        line: Int,
        side: Side?,
        showComments: Boolean?
    ) {
        if (model.reviewContext === reviewContext && model.change === change) {
            scrollToLine(line, side, showComments)
            clearChangeDataOfScrollToLineInReviewContext()
        }
    }

    private fun fetchAndUpdateComments() {
        projectService.messageBus.syncPublisher(MergeRequestDataNotifier.TOPIC).fetchCommentsRequested(
            model.providerData, model.mergeRequestInfo
        )
    }

    private fun collectCommentsOfGutterIconRenderer(renderer: GutterIconRenderer): List<Comment> {
        val result = mutableMapOf<String, CommentPoint>()
        model.commentsOnBeforeSide
            .filter { it.line == renderer.visibleLineLeft }
            .forEach { result[it.id] = it }
        model.commentsOnAfterSide
            .filter { it.line == renderer.visibleLineRight }
            .forEach { result[it.id] = it }

        return result.values.map { it.comment }
    }

    private fun groupCommentsByLine(commentPoints: List<CommentPoint>): Map<Int, List<Comment>> {
        val result = mutableMapOf<Int, MutableList<Comment>>()
        for (commentPoint in commentPoints) {
            if (!result.containsKey(commentPoint.line)) {
                result[commentPoint.line] = mutableListOf()
            }

            val list = result[commentPoint.line]!!
            list.add(commentPoint.comment)
        }
        return result
    }

    private fun convertGutterPositionToCommentPosition(input: GutterPosition): CommentPosition {
        val repository: GitRepository? = RepositoryUtil.findRepository(projectService.project, model.providerData)

        return CommentPositionImpl(
            oldLine = input.oldLine,
            oldPath = if (null === input.oldPath) null else RepositoryUtil.findRelativePath(repository, input.oldPath),
            newLine = input.newLine,
            newPath = if (null === input.newPath) null else RepositoryUtil.findRelativePath(repository, input.newPath),
            baseHash = if (input.baseHash.isNullOrEmpty()) findBaseHash() else input.baseHash,
            headHash = if (input.headHash.isNullOrEmpty()) findHeadHash() else input.headHash,
            startHash = if (input.startHash.isNullOrEmpty()) findStartHash() else input.startHash,
            source = when (input.editorType) {
                DiffView.EditorType.SINGLE_SIDE -> CommentPositionSource.SINGLE_SIDE
                DiffView.EditorType.TWO_SIDE_LEFT -> CommentPositionSource.SIDE_BY_SIDE_LEFT
                DiffView.EditorType.TWO_SIDE_RIGHT -> CommentPositionSource.SIDE_BY_SIDE_RIGHT
                DiffView.EditorType.UNIFIED -> CommentPositionSource.UNIFIED
            },
            changeType = when (input.changeType) {
                DiffView.ChangeType.UNKNOWN -> CommentPositionChangeType.UNKNOWN
                DiffView.ChangeType.INSERTED -> CommentPositionChangeType.INSERTED
                DiffView.ChangeType.DELETED -> CommentPositionChangeType.DELETED
                DiffView.ChangeType.MODIFIED -> CommentPositionChangeType.MODIFIED
            }
        )
    }

    private fun findBaseHash(): String {
        if (model.commits.isNotEmpty()) {
            return model.commits.last().id
        }

        val diff = model.diffReference
        return if (null === diff) "" else diff.baseHash
    }

    private fun findStartHash(): String {
        val diff = model.diffReference
        return if (null === diff) "" else diff.startHash
    }

    private fun findHeadHash(): String {
        if (model.commits.isNotEmpty()) {
            return model.commits.first().id
        }

        val diff = model.diffReference
        return if (null === diff) "" else diff.headHash
    }

    private fun scrollToLine(line: Int, side: Side?, showComments: Boolean?) {
        if (null !== showComments && showComments) {
            if (null !== side) {
                view.displayComments(line, side, DiffView.DisplayCommentMode.SHOW)
            } else {
                view.displayComments(line, Side.LEFT, DiffView.DisplayCommentMode.SHOW)
                view.displayComments(line, Side.RIGHT, DiffView.DisplayCommentMode.SHOW)
            }
        }

        if (null !== side) {
            view.scrollToLine(line, side)
        } else {
            view.scrollToLine(line, Side.LEFT)
            view.scrollToLine(line, Side.RIGHT)
        }
    }

    private fun clearChangeDataOfScrollToLineInReviewContext() {
        model.reviewContext.putChangeData(model.change, DiffNotifier.ScrollLine, null)
        model.reviewContext.putChangeData(model.change, DiffNotifier.ScrollSide, null)
        model.reviewContext.putChangeData(model.change, DiffNotifier.ScrollShowComments, null)
    }
}