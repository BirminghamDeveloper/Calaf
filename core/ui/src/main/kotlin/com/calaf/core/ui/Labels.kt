package com.calaf.core.ui

import androidx.annotation.StringRes
import com.calaf.core.model.CommunityError
import com.calaf.core.model.ReportCategory

@StringRes
fun CommunityError.labelRes(): Int = when (this) {
    CommunityError.OFFLINE -> R.string.offline
    CommunityError.CONFIGURATION -> R.string.configuration
    CommunityError.PERMISSION -> R.string.permission
    CommunityError.LIMIT -> R.string.limit
    CommunityError.INVALID -> R.string.invalid
    CommunityError.SIGN_IN_AGAIN -> R.string.sign_in_again
    CommunityError.UNKNOWN -> R.string.unknown_error
}

@StringRes
fun Notice.labelRes(): Int = when (this) {
    Notice.SAVED -> R.string.saved
    Notice.MATCHED -> R.string.matched_notice
    Notice.REPORTED -> R.string.reported_notice
    Notice.BLOCKED -> R.string.blocked_notice
    Notice.UNMATCHED -> R.string.unmatched_notice
    Notice.SENT -> R.string.sent_notice
    Notice.INVITATION_ACCEPTED -> R.string.accepted_notice
    Notice.REVOKED -> R.string.revoked_notice
    Notice.DELETION_REQUESTED -> R.string.deletion_notice
}

@StringRes
fun ReportCategory.labelRes(): Int = when (this) {
    ReportCategory.HARASSMENT -> R.string.harassment
    ReportCategory.IMPERSONATION -> R.string.impersonation
    ReportCategory.FAKE_PROFILE -> R.string.fake_profile
    ReportCategory.UNDERAGE -> R.string.underage
    ReportCategory.FINANCIAL_SCAM -> R.string.scam
    ReportCategory.INAPPROPRIATE_CONTENT -> R.string.inappropriate
    ReportCategory.OTHER -> R.string.other
}
