package com.calaf.core.model

import kotlinx.coroutines.flow.Flow

enum class Gender { UNSPECIFIED, WOMAN, MAN }
enum class Answer { UNSPECIFIED, YES, NO, OPEN }
enum class MarriageTimeline { WHEN_READY, WITHIN_YEAR, ONE_TO_TWO_YEARS }
enum class VerificationStatus { NOT_STARTED, PENDING, VERIFIED, REJECTED, EXPIRED, EMAIL_ONLY }
enum class SubscriptionTier { FREE, PREMIUM }
enum class MatchStatus { PENDING, ACTIVE, CLOSED, BLOCKED }
enum class ReportCategory { HARASSMENT, IMPERSONATION, FAKE_PROFILE, UNDERAGE, FINANCIAL_SCAM, INAPPROPRIATE_CONTENT, OTHER }
enum class GuardianStatus { INVITED, ACCEPTED, REVOKED }
enum class SafetyWarning { LINK, PERSONAL_INFORMATION, MONEY_REQUEST }
enum class CompatibilityArea { TIMELINE, WANTS_CHILDREN, SMOKING, RELOCATION, RELIGION, LANGUAGES, VALUES, INTERESTS }
data class CompatibilityResult(val score: Int? = null, val strong: List<CompatibilityArea> = emptyList(), val differences: List<CompatibilityArea> = emptyList())
data class CommunityProfile(
    val id: String, val name: String, val age: Int, val city: String, val country: String,
    val biography: String = "", val culture: String = "", val languages: List<String> = emptyList(),
    val interests: List<String> = emptyList(), val values: List<String> = emptyList(),
    val gender: Gender = Gender.UNSPECIFIED, val religion: String = "", val occupation: String = "",
    val education: String = "", val children: Answer = Answer.UNSPECIFIED,
    val wantsChildren: Answer = Answer.UNSPECIFIED, val smoking: Answer = Answer.UNSPECIFIED,
    val relocation: Answer = Answer.UNSPECIFIED, val timeline: MarriageTimeline = MarriageTimeline.WHEN_READY,
    val verification: VerificationStatus = VerificationStatus.NOT_STARTED,
    val compatibility: CompatibilityResult = CompatibilityResult(),
    // Added in Task 2.1. Religion, religiosity, prays and heritage arrive only from members who consented.
    val maritalStatus: MaritalStatus = MaritalStatus.UNSPECIFIED,
    val heightCm: Int = 0,
    val countryOfBirth: String = "",
    val religiosity: Religiosity = Religiosity.UNSPECIFIED,
    val prays: PrayerHabit = PrayerHabit.UNSPECIFIED,
    val heritage: Heritage = Heritage.UNSPECIFIED,
    /** Approved photos only, main photo first. */
    val photos: List<PublicPhoto> = emptyList(),
    /** A note this member sent with their like to you. Shown on their card, with safety warnings if it has any. */
    val compliment: Compliment? = null,
)
/**
 * Who a member wants to see. Empty sets, null answers, blank text and 0 heights all mean "any". The fields from
 * [maritalStatuses] on were added in Task 3.2. Religiosity, prayer and heritage can only match members who chose to
 * share them, because everyone else has those left unspecified.
 */
/** A short note sent with a like. [warnings] flags links, contact details and money requests. */
data class Compliment(val note: String, val warnings: List<SafetyWarning> = emptyList())

/**
 * Members who liked you and are waiting for your answer. [count] is always the real number. When [locked] the
 * [profiles] are withheld, which is how the live app will gate this list behind premium. The demo never locks it.
 */
data class LikedYou(
    val count: Int,
    val profiles: List<CommunityProfile>,
    val locked: Boolean = false,
    /** True in the demo, where the admirers are simulated and the screen must say so. */
    val simulated: Boolean = false,
)

/** Shown full screen when a like becomes a match, with the chat to open if the member wants to say hello. */
data class MatchMoment(val profile: CommunityProfile, val matchId: String)

data class PartnerPreference(
    val minAge: Int = 18, val maxAge: Int = 70, val gender: Gender? = null,
    val country: String = "", val language: String = "", val nonSmokerOnly: Boolean = false,
    val wantsChildren: Answer? = null,
    val maritalStatuses: Set<MaritalStatus> = emptySet(),
    /** Whether the other member already has children. YES or NO, null for any. */
    val hasChildren: Answer? = null,
    val minHeightCm: Int = 0, val maxHeightCm: Int = 0,
    val religiosity: Set<Religiosity> = emptySet(),
    val prays: Set<PrayerHabit> = emptySet(),
    val heritage: Set<Heritage> = emptySet(),
    val countryOfBirth: String = "",
    val profession: String = "",
    val education: String = "",
    val timelines: Set<MarriageTimeline> = emptySet(),
    /** YES also accepts members who are open to it. NO accepts only members who are not. Null for any. */
    val willingToRelocate: Answer? = null,
)
data class PrivacySettings(val visible: Boolean = true, val readReceipts: Boolean = false, val showCity: Boolean = true,
    val notifications: Boolean = true, val analytics: Boolean = false, val marketing: Boolean = false)
data class Match(val id: String, val otherId: String, val name: String, val status: MatchStatus, val blockedByMe: Boolean = false, val messageCount: Long = 0,
    val otherReadAt: Long = 0)
data class ChatMessage(val id: String, val senderId: String, val text: String, val createdAt: Long, val warnings: List<SafetyWarning>)
data class Guardian(val id: String, val ownerId: String, val guardianId: String?, val status: GuardianStatus)
data class DiscoveryPage(val profiles: List<CommunityProfile>, val cursor: String?)
data class Subscription(val tier: SubscriptionTier = SubscriptionTier.FREE, val verifiedUntil: Long = 0)
data class MemberNotification(val id: String, val category: String, val targetId: String, val createdAt: Long, val read: Boolean)
enum class CommunityError { OFFLINE, CONFIGURATION, PERMISSION, LIMIT, INVALID, SIGN_IN_AGAIN, UNKNOWN }
class CommunityException(val reason: CommunityError) : Exception(reason.name)

sealed interface CommunityCommand {
    data class Publish(val profile: ProfileDraft) : CommunityCommand
    data class Preferences(val value: PartnerPreference) : CommunityCommand
    data class Privacy(val value: PrivacySettings) : CommunityCommand
    /** [note] is an optional compliment (see [Compliments]) and only goes with a like. */
    data class Interest(val userId: String, val like: Boolean, val note: String = "") : CommunityCommand
    data class Send(val matchId: String, val messageId: String, val text: String) : CommunityCommand
    data class Read(val matchId: String, val messageId: String) : CommunityCommand
    data class ReadNotification(val id: String, val createdAt: Long) : CommunityCommand
    data class Unmatch(val matchId: String) : CommunityCommand
    data class Block(val userId: String) : CommunityCommand
    data class Report(val userId: String, val category: ReportCategory, val details: String) : CommunityCommand
    data object InviteGuardian : CommunityCommand
    data class AcceptGuardian(val code: String) : CommunityCommand
    data class RevokeGuardian(val id: String) : CommunityCommand
    data object RevokeSessions : CommunityCommand
    data object RequestDeletion : CommunityCommand
    data object ExportData : CommunityCommand
}
data class CommandResult(
    val matched: Boolean = false,
    val invitationCode: String? = null,
    val exportJson: String? = null,
    /** The new match, when [matched] is true. */
    val matchId: String? = null,
)

interface CommunityRepository {
    fun profile(userId: String): Flow<CommunityProfile?>
    fun matches(userId: String): Flow<List<Match>>
    fun settings(userId: String): Flow<PrivacySettings>
    fun preferences(userId: String): Flow<PartnerPreference>
    fun guardians(userId: String): Flow<List<Guardian>>
    fun subscription(userId: String): Flow<Subscription>
    fun notifications(userId: String): Flow<List<MemberNotification>>
    fun messages(matchId: String): Flow<List<ChatMessage>>
    suspend fun earlierMessages(matchId: String, beforeId: String): List<ChatMessage>
    suspend fun discover(cursor: String? = null): DiscoveryPage

    /** Members who liked you and are waiting for an answer. */
    suspend fun likedYou(): LikedYou
    suspend fun execute(command: CommunityCommand): CommandResult
}
