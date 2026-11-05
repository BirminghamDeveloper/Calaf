package com.calaf.core.testing

import com.calaf.core.model.ChatMessage
import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.DiscoveryPage
import com.calaf.core.model.Guardian
import com.calaf.core.model.Match
import com.calaf.core.model.MemberNotification
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PrivacySettings
import com.calaf.core.model.Subscription
import kotlinx.coroutines.flow.MutableStateFlow

/** A controllable [CommunityRepository]. Set the public flows and fields, then read back [commands]. */
class FakeCommunityRepository : CommunityRepository {
    val liveProfile = MutableStateFlow<CommunityProfile?>(null)
    val liveMatches = MutableStateFlow<List<Match>>(emptyList())
    val livePrivacy = MutableStateFlow(PrivacySettings())
    val livePreferences = MutableStateFlow(PartnerPreference())
    val liveGuardians = MutableStateFlow<List<Guardian>>(emptyList())
    val liveSubscription = MutableStateFlow(Subscription())
    val liveNotifications = MutableStateFlow<List<MemberNotification>>(emptyList())
    val liveMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    /** What [earlierMessages] returns. */
    var older = emptyList<ChatMessage>()

    /** What [discover] returns. */
    var page = DiscoveryPage(emptyList(), null)

    /** When set, [execute] throws this error instead of recording success. */
    var failWith: CommunityError? = null

    /** What [execute] returns when it succeeds. */
    var result = CommandResult()

    /** Every command passed to [execute], including ones that failed. */
    val commands = mutableListOf<CommunityCommand>()

    override fun profile(userId: String) = liveProfile
    override fun matches(userId: String) = liveMatches
    override fun settings(userId: String) = livePrivacy
    override fun preferences(userId: String) = livePreferences
    override fun guardians(userId: String) = liveGuardians
    override fun subscription(userId: String) = liveSubscription
    override fun notifications(userId: String) = liveNotifications
    override fun messages(matchId: String) = liveMessages
    override suspend fun earlierMessages(matchId: String, beforeId: String) = older
    override suspend fun discover(cursor: String?) = page

    override suspend fun execute(command: CommunityCommand): CommandResult {
        commands += command
        failWith?.let { throw CommunityException(it) }
        return result
    }
}
