package com.calaf.core.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AccountValidationTest {
    private val today = LocalDate.of(2026, 10, 28)
    private val valid = ProfileDraft("Amina", "2000-03-05", "London", "United Kingdom")

    @Test fun `eighteenth birthday is eligible but day before is not`() {
        assertNull(AccountValidation.profile(valid.copy(birthDate = "2008-10-28"), today))
        assertEquals(ProfileIssue.Underage, AccountValidation.profile(valid.copy(birthDate = "2008-10-29"), today))
    }
    @Test fun `invalid dates and future dates are rejected`() {
        for (date in listOf("2000-02-30", "tomorrow", "2027-01-01")) {
            assertEquals(ProfileIssue.BirthDate, AccountValidation.profile(valid.copy(birthDate = date), today))
        }
    }
    @Test fun `required fields and biography limits are enforced`() {
        assertEquals(ProfileIssue.Name, AccountValidation.profile(valid.copy(name = " "), today))
        assertEquals(ProfileIssue.City, AccountValidation.profile(valid.copy(city = ""), today))
        assertEquals(ProfileIssue.Biography, AccountValidation.profile(valid.copy(biography = "x".repeat(501)), today))
    }
    @Test fun `email and password validation reject malformed input`() {
        assertTrue(AccountValidation.email("test@example.org"))
        assertFalse(AccountValidation.email("test@"))
        assertFalse(AccountValidation.email("test name@example.org"))
        assertFalse(AccountValidation.password("short"))
        assertTrue(AccountValidation.password("long password"))
    }

    @Test fun `faith and heritage need consent and withdrawing it deletes them`() {
        val faith = valid.copy(
            religion = "Islam", religiosity = Religiosity.PRACTISING, prays = PrayerHabit.USUALLY,
            heritage = Heritage.SOMALI,
        )
        assertEquals(ProfileIssue.ConsentRequired, AccountValidation.profile(faith, today))
        assertNull(AccountValidation.profile(faith.copy(sensitiveConsent = true), today))
        assertTrue(faith.hasSpecialCategoryData)
        val kept = faith.withConsent(true)
        assertEquals(faith.religion, kept.religion)
        val withdrawn = kept.withConsent(false)
        assertFalse(withdrawn.hasSpecialCategoryData)
        assertFalse(withdrawn.sensitiveConsent)
        assertEquals(withdrawn, faith.withoutUnconsentedData())
        assertEquals(kept, kept.withoutUnconsentedData())
    }
    @Test fun `height is optional but must be realistic when given`() {
        assertNull(AccountValidation.profile(valid.copy(heightCm = 0), today))
        assertNull(AccountValidation.profile(valid.copy(heightCm = 170), today))
        assertEquals(ProfileIssue.Height, AccountValidation.profile(valid.copy(heightCm = 80), today))
        assertEquals(ProfileIssue.Height, AccountValidation.profile(valid.copy(heightCm = 260), today))
    }
    @Test fun `completion counts optional faith fields only after consent`() {
        assertEquals(0, ProfileCompletion.percent(ProfileDraft()))
        val full = valid.copy(
            gender = Gender.WOMAN, biography = "Hello", languages = "Somali", interests = "Reading", values = "Family",
            occupation = "Nurse", education = "Degree", maritalStatus = MaritalStatus.NEVER_MARRIED, heightCm = 165,
            countryOfBirth = "Somalia", children = Answer.NO, wantsChildren = Answer.YES, smoking = Answer.NO,
            relocation = Answer.OPEN,
        )
        assertEquals(100, ProfileCompletion.percent(full))
        // Not consenting never lowers the score, consenting without filling the fields does.
        assertEquals(100, ProfileCompletion.percent(full.withConsent(false)))
        assertTrue(ProfileCompletion.percent(full.withConsent(true)) < 100)
        assertEquals(100, ProfileCompletion.percent(full.copy(
            sensitiveConsent = true, religion = "Islam", religiosity = Religiosity.PRACTISING,
            prays = PrayerHabit.ALWAYS, heritage = Heritage.SOMALILAND,
        )))
    }
}
