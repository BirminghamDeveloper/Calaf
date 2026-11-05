package com.calaf.core.model

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class Account(val id: String, val email: String, val emailVerified: Boolean)
enum class AccountError { InvalidInput, Credentials, Network, LocalBackend, EmailInUse, RateLimited, WeakPassword, Unavailable, Unknown }
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
    // Added in Task 2.1. Appended so older positional constructors and stored drafts keep working.
    val maritalStatus: MaritalStatus = MaritalStatus.UNSPECIFIED,
    /** Height in centimetres. 0 means not shared. */
    val heightCm: Int = 0,
    val countryOfBirth: String = "",
    val religiosity: Religiosity = Religiosity.UNSPECIFIED,
    val prays: PrayerHabit = PrayerHabit.UNSPECIFIED,
    val heritage: Heritage = Heritage.UNSPECIFIED,
    /** Explicit consent to save and show religion, how practising, prayer and heritage. Off by default. */
    val sensitiveConsent: Boolean = false,
)

interface ProfileRepository {
    suspend fun load(userId: String): ProfileDraft
    suspend fun save(userId: String, profile: ProfileDraft)
}

enum class ProfileIssue { Name, BirthDate, Underage, City, Country, Biography, Length, Height, ConsentRequired }

const val MIN_HEIGHT_CM = 120
const val MAX_HEIGHT_CM = 230

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
        if (value.countryOfBirth.length > 100 || value.religion.length > 60) return ProfileIssue.Length
        if (value.heightCm != 0 && value.heightCm !in MIN_HEIGHT_CM..MAX_HEIGHT_CM) return ProfileIssue.Height
        if (value.hasSpecialCategoryData && !value.sensitiveConsent) return ProfileIssue.ConsentRequired
        return null
    }
}
