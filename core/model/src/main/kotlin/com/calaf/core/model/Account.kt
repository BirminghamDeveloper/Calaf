package com.calaf.core.model

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class Account(val id: String, val email: String, val emailVerified: Boolean)
enum class AccountError { InvalidInput, Credentials, Network, RateLimited, WeakPassword, Unavailable, Unknown }
class AccountException(val reason: AccountError) : Exception(reason.name)

interface AuthRepository {
    val accounts: Flow<Account?>
    suspend fun register(email: String, password: String)
    suspend fun login(email: String, password: String)
    suspend fun resetPassword(email: String)
    suspend fun sendVerification()
    suspend fun refresh(): Account?
    fun logout()
}

data class ProfileDraft(
    val name: String = "",
    val birthDate: String = "",
    val city: String = "",
    val country: String = "",
    val languages: String = "",
    val culture: String = "",
    val biography: String = "",
    val gender: Gender = Gender.UNSPECIFIED,
    val religion: String = "",
    val occupation: String = "",
    val education: String = "",
    val interests: String = "",
    val values: String = "",
    val children: Answer = Answer.UNSPECIFIED,
    val wantsChildren: Answer = Answer.UNSPECIFIED,
    val smoking: Answer = Answer.UNSPECIFIED,
    val relocation: Answer = Answer.UNSPECIFIED,
    val timeline: MarriageTimeline = MarriageTimeline.WHEN_READY,
)

interface ProfileRepository {
    suspend fun load(userId: String): ProfileDraft
    suspend fun save(userId: String, profile: ProfileDraft)
}

enum class ProfileIssue { Name, BirthDate, Underage, City, Country, Biography, Length }

object AccountValidation {
    fun email(value: String): Boolean = value.length <= 254 &&
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())
    fun password(value: String): Boolean = value.length in 8..128
    fun profile(value: ProfileDraft, today: LocalDate = LocalDate.now()): ProfileIssue? {
        if (value.name.trim().length !in 2..60) return ProfileIssue.Name
        val dob = try { LocalDate.parse(value.birthDate) } catch (_: Exception) { return ProfileIssue.BirthDate }
        if (dob.isAfter(today) || dob.isBefore(today.minusYears(120))) return ProfileIssue.BirthDate
        if (dob.isAfter(today.minusYears(18))) return ProfileIssue.Underage
        if (value.city.trim().length !in 2..100) return ProfileIssue.City
        if (value.country.trim().length !in 2..100) return ProfileIssue.Country
        if (value.biography.length > 500) return ProfileIssue.Biography
        if (value.languages.length > 200 || value.culture.length > 200) return ProfileIssue.Length
        return null
    }
}
