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
)
data class PartnerPreference(val minAge: Int = 18, val maxAge: Int = 70, val gender: Gender? = null,
    val country: String = "", val language: String = "", val nonSmokerOnly: Boolean = false, val wantsChildren: Answer? = null)
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
    data class Interest(val userId: String, val like: Boolean) : CommunityCommand
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
data class CommandResult(val matched: Boolean = false, val invitationCode: String? = null, val exportJson: String? = null)

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
    suspend fun execute(command: CommunityCommand): CommandResult
}
