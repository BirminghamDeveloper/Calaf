package com.calaf.core.model

/** Optional profile choices added in Task 2.1. UNSPECIFIED always means the member chose not to say. */
enum class MaritalStatus { UNSPECIFIED, NEVER_MARRIED, DIVORCED, WIDOWED }

enum class Religiosity { UNSPECIFIED, VERY_PRACTISING, PRACTISING, MODERATELY, NOT_PRACTISING }

/** How often the member prays. */
enum class PrayerHabit { UNSPECIFIED, ALWAYS, USUALLY, SOMETIMES, NEVER }

/** Heritage is ethnicity data. It is a free choice from this list and never a clan (qabiil). */
enum class Heritage { UNSPECIFIED, SOMALI, SOMALILAND, ETHIOPIAN, ERITREAN, DJIBOUTIAN, KENYAN, SUDANESE, OTHER }

/**
 * Religion, how practising, prayer and heritage are UK GDPR special category data. They are only kept while
 * [ProfileDraft.sensitiveConsent] is true, and withdrawing consent removes them.
 */
val ProfileDraft.hasSpecialCategoryData: Boolean
    get() = religion.isNotBlank() || religiosity != Religiosity.UNSPECIFIED ||
        prays != PrayerHabit.UNSPECIFIED || heritage != Heritage.UNSPECIFIED

/** Grants or withdraws consent. Withdrawing deletes every special category field. */
fun ProfileDraft.withConsent(granted: Boolean): ProfileDraft =
    if (granted) {
        copy(sensitiveConsent = true)
    } else {
        copy(
            sensitiveConsent = false, religion = "", religiosity = Religiosity.UNSPECIFIED,
            prays = PrayerHabit.UNSPECIFIED, heritage = Heritage.UNSPECIFIED,
        )
    }

/** Drops special category data that has no consent behind it. Safe to call on any draft. */
fun ProfileDraft.withoutUnconsentedData(): ProfileDraft = if (sensitiveConsent) this else withConsent(false)

/** How complete a profile is, from 0 to 100. Optional faith and heritage fields only count once consent is given. */
object ProfileCompletion {
    fun percent(draft: ProfileDraft): Int {
        val items = mutableListOf(
            draft.name.isNotBlank(), draft.birthDate.isNotBlank(), draft.city.isNotBlank(),
            draft.country.isNotBlank(), draft.gender != Gender.UNSPECIFIED, draft.biography.isNotBlank(),
            draft.languages.isNotBlank(), draft.interests.isNotBlank(), draft.values.isNotBlank(),
            draft.occupation.isNotBlank(), draft.education.isNotBlank(),
            draft.maritalStatus != MaritalStatus.UNSPECIFIED, draft.heightCm != 0,
            draft.countryOfBirth.isNotBlank(), draft.children != Answer.UNSPECIFIED,
            draft.wantsChildren != Answer.UNSPECIFIED, draft.smoking != Answer.UNSPECIFIED,
            draft.relocation != Answer.UNSPECIFIED,
        )
        if (draft.sensitiveConsent) {
            items += listOf(
                draft.religion.isNotBlank(), draft.religiosity != Religiosity.UNSPECIFIED,
                draft.prays != PrayerHabit.UNSPECIFIED, draft.heritage != Heritage.UNSPECIFIED,
            )
        }
        return 100 * items.count { it } / items.size
    }
}
