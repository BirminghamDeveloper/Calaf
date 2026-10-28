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
}
