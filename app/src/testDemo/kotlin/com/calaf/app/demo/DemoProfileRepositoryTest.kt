package com.calaf.app.demo

import com.calaf.core.model.CommunityException
import com.calaf.core.model.Heritage
import com.calaf.core.model.MaritalStatus
import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.Religiosity
import com.calaf.core.model.withConsent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoProfileRepositoryTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun `drafts persist, fall back to the published profile and stay private`() = runBlocking {
        val demo = DemoFixture(temp.newFolder("store"))
        demo.loginAsMember("hassan")
        assertEquals("Hassan (Demo)", demo.profiles.load("demo-hassan").name)
        val draft = ProfileDraft("Hassan", "1990-01-01", "Leeds", "UK")
        demo.profiles.save("demo-hassan", draft)
        assertEquals(draft, demo.reopen().profiles.load("demo-hassan"))
        assertTrue(runCatching { demo.profiles.load("demo-amina") }.exceptionOrNull() is CommunityException)
        assertTrue(runCatching { demo.profiles.save("demo-amina", draft) }.isFailure)
    }

    @Test fun `nobody signed in cannot read or write drafts`() {
        val demo = DemoFixture(temp.newFolder("store"))
        assertTrue(runCatching { runBlocking { demo.profiles.load("demo-hassan") } }.isFailure)
    }

    @Test fun `new profile fields round trip and drafts never keep faith details without consent`() = runBlocking {
        val demo = DemoFixture(temp.newFolder("store"))
        demo.loginAsMember("hassan")
        val draft = ProfileDraft(
            "Hassan", "1990-01-01", "Leeds", "UK", maritalStatus = MaritalStatus.DIVORCED, heightCm = 180,
            countryOfBirth = "Somalia", religion = "Islam", religiosity = Religiosity.PRACTISING,
            heritage = Heritage.SOMALILAND,
        )
        demo.profiles.save("demo-hassan", draft)
        val stored = demo.reopen().profiles.load("demo-hassan")
        assertEquals(MaritalStatus.DIVORCED, stored.maritalStatus)
        assertEquals(180, stored.heightCm)
        assertEquals("Somalia", stored.countryOfBirth)
        // No consent was given, so religion, religiosity and heritage were dropped.
        assertEquals("", stored.religion)
        assertEquals(Heritage.UNSPECIFIED, stored.heritage)
        demo.profiles.save("demo-hassan", draft.withConsent(true))
        val consented = demo.reopen().profiles.load("demo-hassan")
        assertEquals(Heritage.SOMALILAND, consented.heritage)
        assertTrue(consented.sensitiveConsent)
        demo.profiles.save("demo-hassan", consented.withConsent(false))
        assertEquals(Heritage.UNSPECIFIED, demo.reopen().profiles.load("demo-hassan").heritage)
    }

    @Test fun `a draft saved before the new fields existed still loads`() = runBlocking {
        val demo = DemoFixture(temp.newFolder("store"))
        demo.store.table("drafts").put(
            "demo-hassan",
            org.json.JSONObject().put("name", "Old").put("birthDate", "1990-01-01").put("city", "Leeds")
                .put("country", "UK").put("gender", "MAN"),
        )
        demo.loginAsMember("hassan")
        val loaded = demo.profiles.load("demo-hassan")
        assertEquals("Old", loaded.name)
        assertEquals(MaritalStatus.UNSPECIFIED, loaded.maritalStatus)
        assertEquals(0, loaded.heightCm)
        assertFalse(loaded.sensitiveConsent)
    }
}
