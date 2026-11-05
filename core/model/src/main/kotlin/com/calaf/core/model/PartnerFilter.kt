package com.calaf.core.model

/**
 * Whether [profile] passes this member's filters. Shared by every backend that filters on the device, and mirrored by
 * `eligible` in `backend/src/domain.js`. A filter on a field a member left unspecified (height, faith, heritage and
 * so on) never matches that member, so people who chose not to share something are not guessed at.
 */
fun PartnerPreference.accepts(profile: CommunityProfile): Boolean =
    profile.age in minAge..maxAge &&
        (gender == null || gender == profile.gender) &&
        (country.isBlank() || profile.country.equals(country, true)) &&
        (language.isBlank() || profile.languages.any { it.equals(language, true) }) &&
        (!nonSmokerOnly || profile.smoking == Answer.NO) &&
        (wantsChildren == null || profile.wantsChildren == wantsChildren) &&
        (maritalStatuses.isEmpty() || profile.maritalStatus in maritalStatuses) &&
        (hasChildren == null || profile.children == hasChildren) &&
        (minHeightCm == 0 || (profile.heightCm != 0 && profile.heightCm >= minHeightCm)) &&
        (maxHeightCm == 0 || (profile.heightCm != 0 && profile.heightCm <= maxHeightCm)) &&
        (religiosity.isEmpty() || profile.religiosity in religiosity) &&
        (prays.isEmpty() || profile.prays in prays) &&
        (heritage.isEmpty() || profile.heritage in heritage) &&
        (countryOfBirth.isBlank() || profile.countryOfBirth.equals(countryOfBirth, true)) &&
        (profession.isBlank() || profile.occupation.contains(profession, true)) &&
        (education.isBlank() || profile.education.contains(education, true)) &&
        (timelines.isEmpty() || profile.timeline in timelines) &&
        relocationMatches(profile.relocation)

private fun PartnerPreference.relocationMatches(answer: Answer): Boolean = when (willingToRelocate) {
    null -> true
    Answer.YES -> answer == Answer.YES || answer == Answer.OPEN
    else -> answer == Answer.NO
}

/** How many filters are switched on, for the badge on the Filters button. The default age range does not count. */
val PartnerPreference.activeFilterCount: Int
    get() = listOf(
        minAge != 18 || maxAge != 70, gender != null, country.isNotBlank(), language.isNotBlank(), nonSmokerOnly,
        wantsChildren != null, maritalStatuses.isNotEmpty(), hasChildren != null, minHeightCm != 0 || maxHeightCm != 0,
        religiosity.isNotEmpty(), prays.isNotEmpty(), heritage.isNotEmpty(), countryOfBirth.isNotBlank(),
        profession.isNotBlank(), education.isNotBlank(), timelines.isNotEmpty(), willingToRelocate != null,
    ).count { it }
