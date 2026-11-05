package com.calaf.core.ui

import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand

/** A short confirmation shown after a successful action. */
enum class Notice {
    SAVED, MATCHED, REPORTED, BLOCKED, UNMATCHED, SENT, INVITATION_ACCEPTED, REVOKED, DELETION_REQUESTED
}

fun noticeFor(command: CommunityCommand, result: CommandResult): Notice = when {
    result.matched -> Notice.MATCHED
    command is CommunityCommand.Report -> Notice.REPORTED
    command is CommunityCommand.Block -> Notice.BLOCKED
    command is CommunityCommand.Unmatch -> Notice.UNMATCHED
    command is CommunityCommand.Send -> Notice.SENT
    command is CommunityCommand.AcceptGuardian -> Notice.INVITATION_ACCEPTED
    command is CommunityCommand.RevokeGuardian || command == CommunityCommand.RevokeSessions -> Notice.REVOKED
    command == CommunityCommand.RequestDeletion -> Notice.DELETION_REQUESTED
    else -> Notice.SAVED
}
