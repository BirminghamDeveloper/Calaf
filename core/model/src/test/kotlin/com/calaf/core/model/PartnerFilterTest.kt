package com.calaf.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PartnerFilterTest {
    private val member = CommunityProfile(
        "m", "Amina", 30, "London", "United Kingdom", languages = listOf("Somali", "English"),
        occupation = "Primary school teacher", education = "University degree", children = Answer.NO,
        wantsChildren = Answer.YES, smoking = Answer.NO, relocation = Answer.OPEN,
        timeline = MarriageTimeline.WITHIN_YEAR, maritalStatus = MaritalStatus.NEVER_MARRIED, heightCm = 168,
        countryOfBirth = "Somalia", religiosity = Religiosity.PRACTISING, prays = PrayerHabit.USUALLY,
        heritage = Heritage.SOMALILAND,
    )

    @Test fun `no filters accepts everyone in the default age range`() {
        assertTrue(PartnerPreference().accepts(member))
        assertTrue(PartnerPreference().accepts(CommunityProfile("x", "X", 40, "A", "B")))
        assertFalse(PartnerPreference(maxAge = 29).accepts(member))
        assertFalse(PartnerPreference(minAge = 31).accepts(member))
    }

    @Test fun `each phase 2 filter accepts and rejects`() {
        fun accepts(preference: PartnerPreference) = preference.accepts(member)
        assertTrue(accepts(PartnerPreference(maritalStatuses = setOf(MaritalStatus.NEVER_MARRIED, MaritalStatus.WIDOWED))))
        assertFalse(accepts(PartnerPreference(maritalStatuses = setOf(MaritalStatus.DIVORCED))))
        assertTrue(accepts(PartnerPreference(hasChildren = Answer.NO)))
        assertFalse(accepts(PartnerPreference(hasChildren = Answer.YES)))
        assertTrue(accepts(PartnerPreference(minHeightCm = 160, maxHeightCm = 175)))
        assertFalse(accepts(PartnerPreference(minHeightCm = 170)))
        assertFalse(accepts(PartnerPreference(maxHeightCm = 160)))
        assertTrue(accepts(PartnerPreference(religiosity = setOf(Religiosity.PRACTISING))))
        assertFalse(accepts(PartnerPreference(religiosity = setOf(Religiosity.NOT_PRACTISING))))
        assertTrue(accepts(PartnerPreference(prays = setOf(PrayerHabit.ALWAYS, PrayerHabit.USUALLY))))
        assertFalse(accepts(PartnerPreference(prays = setOf(PrayerHabit.NEVER))))
        assertTrue(accepts(PartnerPreference(heritage = setOf(Heritage.SOMALILAND))))
        assertFalse(accepts(PartnerPreference(heritage = setOf(Heritage.KENYAN))))
        assertTrue(accepts(PartnerPreference(countryOfBirth = "somalia")))
        assertFalse(accepts(PartnerPreference(countryOfBirth = "Kenya")))
        assertTrue(accepts(PartnerPreference(profession = "teacher")))
        assertFalse(accepts(PartnerPreference(profession = "doctor")))
        assertTrue(accepts(PartnerPreference(education = "degree")))
        assertFalse(accepts(PartnerPreference(education = "phd")))
        assertTrue(accepts(PartnerPreference(timelines = setOf(MarriageTimeline.WITHIN_YEAR))))
        assertFalse(accepts(PartnerPreference(timelines = setOf(MarriageTimeline.ONE_TO_TWO_YEARS))))
        assertTrue(accepts(PartnerPreference(language = "somali", wantsChildren = Answer.YES, nonSmokerOnly = true)))
    }

    @Test fun `willing to relocate YES accepts yes and open and NO accepts only no`() {
        val yes = PartnerPreference(willingToRelocate = Answer.YES)
        val no = PartnerPreference(willingToRelocate = Answer.NO)
        assertTrue(yes.accepts(member.copy(relocation = Answer.YES)))
        assertTrue(yes.accepts(member.copy(relocation = Answer.OPEN)))
        assertFalse(yes.accepts(member.copy(relocation = Answer.NO)))
        assertFalse(yes.accepts(member.copy(relocation = Answer.UNSPECIFIED)))
        assertTrue(no.accepts(member.copy(relocation = Answer.NO)))
        assertFalse(no.accepts(member.copy(relocation = Answer.OPEN)))
    }

    @Test fun `a filter never matches a member who left that field unspecified`() {
        val reserved = member.copy(
            heightCm = 0, religiosity = Religiosity.UNSPECIFIED, prays = PrayerHabit.UNSPECIFIED,
            heritage = Heritage.UNSPECIFIED, maritalStatus = MaritalStatus.UNSPECIFIED, countryOfBirth = "",
        )
        assertTrue(PartnerPreference().accepts(reserved))
        assertFalse(PartnerPreference(minHeightCm = 150).accepts(reserved))
        assertFalse(PartnerPreference(maxHeightCm = 200).accepts(reserved))
        assertFalse(PartnerPreference(religiosity = setOf(Religiosity.PRACTISING)).accepts(reserved))
        assertFalse(PartnerPreference(prays = setOf(PrayerHabit.ALWAYS)).accepts(reserved))
        assertFalse(PartnerPreference(heritage = setOf(Heritage.SOMALI)).accepts(reserved))
        assertFalse(PartnerPreference(maritalStatuses = setOf(MaritalStatus.NEVER_MARRIED)).accepts(reserved))
        assertFalse(PartnerPreference(countryOfBirth = "Somalia").accepts(reserved))
    }

    @Test fun `the active filter count ignores the default age range and counts each switched on filter`() {
        assertEquals(0, PartnerPreference().activeFilterCount)
        assertEquals(1, PartnerPreference(minAge = 25).activeFilterCount)
        val several = PartnerPreference(
            maxAge = 40, heritage = setOf(Heritage.SOMALI), minHeightCm = 150, maxHeightCm = 190,
            profession = "nurse", willingToRelocate = Answer.YES, nonSmokerOnly = true,
        )
        assertEquals(6, several.activeFilterCount)
    }
}
