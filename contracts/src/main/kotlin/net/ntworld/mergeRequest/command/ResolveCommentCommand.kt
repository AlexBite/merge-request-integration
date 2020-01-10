package net.ntworld.mergeRequest.command

import net.ntworld.foundation.cqrs.Command
import net.ntworld.mergeRequest.Comment

interface ResolveCommentCommand : Command {
    val providerId: String

    val mergeRequestId: String

    val comment: Comment

    companion object
}
