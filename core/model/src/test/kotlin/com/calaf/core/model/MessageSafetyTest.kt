package com.calaf.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageSafetyTest {
    @Test fun `plain text has no warnings`() {
        assertEquals(emptyList<SafetyWarning>(), MessageSafety.warnings("Salaam, I liked your profile."))
    }

    @Test fun `links, contact details and money requests are flagged`() {
        assertEquals(listOf(SafetyWarning.LINK), MessageSafety.warnings("see https://example.org"))
        assertEquals(listOf(SafetyWarning.LINK), MessageSafety.warnings("WWW.example.org"))
        assertEquals(listOf(SafetyWarning.PERSONAL_INFORMATION), MessageSafety.warnings("call 07700 900123"))
        assertEquals(listOf(SafetyWarning.PERSONAL_INFORMATION), MessageSafety.warnings("a@example.org"))
        assertEquals(listOf(SafetyWarning.MONEY_REQUEST), MessageSafety.warnings("Please send money today"))
        assertEquals(
            listOf(SafetyWarning.LINK, SafetyWarning.MONEY_REQUEST),
            MessageSafety.warnings("Send Money via www.example.org"),
        )
    }

    @Test fun `a note is one to 140 characters after trimming`() {
        assertNull(Compliments.clean(""))
        assertNull(Compliments.clean("   "))
        assertEquals("Hello", Compliments.clean("  Hello  "))
        assertEquals(140, Compliments.clean("x".repeat(140))?.length)
        assertNull(Compliments.clean("x".repeat(141)))
    }
}
