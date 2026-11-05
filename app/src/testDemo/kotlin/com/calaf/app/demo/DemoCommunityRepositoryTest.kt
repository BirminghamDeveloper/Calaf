package com.calaf.app.demo

import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.ReportCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoCommunityRepositoryTest {
    @get:Rule val temp = TemporaryFolder()
    private fun fixture() = DemoFixture(temp.newFolder("store"))

    private fun matchNotifications(demo: DemoFixture) =
        demo.store.rows("notifications").filter { it.second.getString("category") == "MATCH" }

    @Test fun `messages persist, are idempotent and blocking revokes conversation access`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val match = demo.community.matches("demo-hassan").first().single()
        demo.community.execute(CommunityCommand.Send(match.id, "m1", "Hello from the demo"))
        demo.community.execute(CommunityCommand.Send(match.id, "m1", "Duplicate"))
        assertEquals(1, demo.community.messages(match.id).first().size)
        demo.auth.logout()
        val reopened = demo.reopen()
        reopened.loginAsMember("amina")
        assertEquals("Hello from the demo", reopened.community.messages(match.id).first().single().text)
        assertEquals(1, reopened.community.notifications("demo-amina").first().count { it.category == "MESSAGE" })
        reopened.community.execute(CommunityCommand.Block("demo-hassan"))
        val denied = CommunityCommand.Send(match.id, "m2", "Denied")
        assertTrue(runCatching { reopened.community.execute(denied) }.isFailure)
        assertTrue(runCatching { reopened.community.messages(match.id).first() }.isFailure)
        assertEquals(MatchStatus.BLOCKED, reopened.community.matches("demo-amina").first()
            .single { it.otherId == "demo-hassan" }.status)
    }

    @Test fun `blocking an unmatched member creates no pair and no MATCH notification`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hodan")
        val pairsBefore = demo.store.rows("pairs").map { it.first }.toSet()
        val matchNotificationsBefore = matchNotifications(demo).size
        assertTrue(demo.community.discover().profiles.any { it.id == "demo-yusuf" })
        demo.community.execute(CommunityCommand.Block("demo-yusuf"))
        assertEquals(pairsBefore, demo.store.rows("pairs").map { it.first }.toSet())
        assertEquals(matchNotificationsBefore, matchNotifications(demo).size)
        assertTrue(demo.community.notifications("demo-hodan").first().none { it.category == "MATCH" })
        assertTrue(demo.community.matches("demo-hodan").first().isEmpty())
        // The block still works: they vanish from discovery and cannot be liked.
        assertFalse(demo.community.discover().profiles.any { it.id == "demo-yusuf" })
        assertTrue(runCatching { demo.community.execute(CommunityCommand.Interest("demo-yusuf", true)) }.isFailure)
        assertEquals(pairsBefore, demo.store.rows("pairs").map { it.first }.toSet())
    }

    @Test fun `a block hides both members from each other in discovery`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("yusuf")
        demo.community.execute(CommunityCommand.Block("demo-hodan"))
        demo.auth.logout()
        demo.loginAsMember("hodan")
        assertFalse(demo.community.discover().profiles.any { it.id == "demo-yusuf" })
        assertTrue(runCatching { demo.community.execute(CommunityCommand.Interest("demo-yusuf", true)) }.isFailure)
    }

    @Test fun `liking a seeded member matches, and discovery then excludes them`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hodan")
        assertFalse(demo.community.execute(CommunityCommand.Interest("demo-yusuf", false)).matched)
        demo.community.execute(CommunityCommand.Interest("demo-hassan", true)).also { assertTrue(it.matched) }
        assertEquals(1, demo.community.matches("demo-hodan").first().size)
        assertEquals(1, matchNotifications(demo).count { it.second.getString("owner") == "demo-hodan" })
        val ids = demo.community.discover().profiles.map { it.id }
        assertFalse("demo-hassan" in ids || "demo-yusuf" in ids || "demo-hodan" in ids)
    }

    @Test fun `discovery shows only the allowlisted public fields`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hodan")
        val profile = demo.community.discover().profiles.first { it.id == "demo-yusuf" }
        assertEquals("Yusuf (Demo)", profile.name)
        assertTrue(profile.age in 18..70)
        assertEquals("Hodan (Demo)", demo.community.profile("demo-hodan").first()!!.name)
    }

    @Test fun `unmatch, report and privacy settings work and are scoped to the signed in member`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("amina")
        val match = demo.community.matches("demo-amina").first().first()
        demo.community.execute(CommunityCommand.Unmatch(match.id))
        val after = demo.community.matches("demo-amina").first().first { it.id == match.id }
        assertEquals(MatchStatus.CLOSED, after.status)
        demo.community.execute(CommunityCommand.Report("demo-yusuf", ReportCategory.OTHER, "details"))
        assertEquals(2, demo.store.rows("reports").size)
        assertTrue(runCatching { demo.community.settings("demo-hassan").first() }.isFailure)
        val selfReport = CommunityCommand.Report("demo-amina", ReportCategory.OTHER, "")
        assertTrue(runCatching { demo.community.execute(selfReport) }.isFailure)
    }

    @Test fun `guardian invite, accept and revoke`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("amina")
        val code = demo.community.execute(CommunityCommand.InviteGuardian).invitationCode!!
        assertTrue(runCatching { demo.community.execute(CommunityCommand.AcceptGuardian(code)) }.isFailure)
        demo.auth.logout()
        demo.loginAsMember("hassan")
        demo.community.execute(CommunityCommand.AcceptGuardian(code))
        assertEquals(1, demo.community.guardians("demo-hassan").first().size)
        demo.community.execute(CommunityCommand.RevokeGuardian(code))
        assertEquals("REVOKED", demo.store.table("guardians").getJSONObject(code).getString("status"))
    }

    @Test fun `nobody signed in can do nothing`() {
        val demo = fixture()
        assertTrue(runCatching { runBlocking { demo.community.discover() } }.isFailure)
        val block = CommunityCommand.Block("demo-hassan")
        assertTrue(runCatching { runBlocking { demo.community.execute(block) } }.isFailure)
    }

    @Test fun `discovery exposes the new profile fields only for members who consented`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hodan")
        val yusuf = demo.community.discover().profiles.first { it.id == "demo-yusuf" }
        assertEquals(com.calaf.core.model.MaritalStatus.NEVER_MARRIED, yusuf.maritalStatus)
        assertTrue(yusuf.heightCm in 120..230)
        assertEquals("Somalia", yusuf.countryOfBirth)
        assertEquals(com.calaf.core.model.Religiosity.PRACTISING, yusuf.religiosity)
        // Withdraw Yusuf's consent in the stored row: faith and heritage must disappear from the projection.
        val row = demo.store.table("profiles").getJSONObject("demo-yusuf")
        row.put("sensitiveConsent", false)
        val hidden = demo.community.discover().profiles.first { it.id == "demo-yusuf" }
        assertEquals("", hidden.religion)
        assertEquals(com.calaf.core.model.Religiosity.UNSPECIFIED, hidden.religiosity)
        assertEquals(com.calaf.core.model.Heritage.UNSPECIFIED, hidden.heritage)
        assertEquals("Somalia", hidden.countryOfBirth)
    }

    @Test fun `publishing faith details without consent is refused`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hodan")
        val draft = com.calaf.core.model.ProfileDraft(
            "Hodan", "1993-11-05", "London", "UK", religion = "Islam",
        )
        assertTrue(runCatching { demo.community.execute(CommunityCommand.Publish(draft)) }.isFailure)
        val consented = draft.copy(sensitiveConsent = true)
        demo.community.execute(CommunityCommand.Publish(consented))
        assertEquals("Islam", com.calaf.app.demo.DemoProfileCodec.toDraft(
            demo.store.table("profiles").getJSONObject("demo-hodan")).religion)
    }

    @Test fun `discovery lists approved photos main first with the blur choice`() = runBlocking {
        val demo = fixture()
        demo.loginAsMember("hodan")
        val photos = demo.community.discover().profiles.first { it.id == "demo-hassan" }.photos
        assertTrue(photos.size in 2..3)
        assertEquals(DemoPhotoRows.ownerPhotos(demo.store, "demo-hassan").map { it.id }, photos.map { it.id })
        assertTrue(photos.none { it.blurred })
        val firstId = photos.first().id
        DemoPhotoRows.row(demo.store, firstId)!!.put("blur", true)
        assertTrue(demo.community.discover().profiles.first { it.id == "demo-hassan" }.photos.first().blurred)
        DemoPhotoRows.row(demo.store, firstId)!!.put("state", "PENDING")
        assertFalse(firstId in demo.community.discover().profiles.first { it.id == "demo-hassan" }.photos.map { it.id })
    }

    @Test fun `saved phase 2 filters narrow discovery and survive a restart`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hodan")
        fun ids() = runBlocking { demo.community.discover().profiles.map { it.id }.sorted() }
        assertEquals(listOf("demo-admin", "demo-amina", "demo-hassan", "demo-yusuf"), ids())
        val filters = com.calaf.core.model.PartnerPreference(
            heritage = setOf(com.calaf.core.model.Heritage.SOMALILAND),
            maritalStatuses = setOf(com.calaf.core.model.MaritalStatus.NEVER_MARRIED),
            willingToRelocate = null, minHeightCm = 160,
        )
        demo.community.execute(CommunityCommand.Preferences(filters))
        assertEquals(listOf("demo-hassan"), ids())
        val reopened = demo.reopen()
        reopened.auth.refresh()
        assertEquals(filters, reopened.community.preferences("demo-hodan").first())
        demo.community.execute(CommunityCommand.Preferences(filters.copy(minHeightCm = 175, heritage = emptySet())))
        assertEquals(listOf("demo-yusuf"), ids())
        demo.community.execute(CommunityCommand.Preferences(com.calaf.core.model.PartnerPreference(profession = "nurse")))
        assertEquals(emptyList<String>(), ids())
    }

    @Test fun `a preference row saved before the new filters existed still means any`() = runBlocking<Unit> {
        val demo = fixture()
        demo.store.table("preferences").put(
            "demo-hodan",
            org.json.JSONObject().put("min", 25).put("max", 60).put("gender", "MAN").put("country", "United Kingdom")
                .put("nonSmoker", true).put("children", "YES"),
        )
        demo.loginAsMember("hodan")
        val loaded = demo.community.preferences("demo-hodan").first()
        assertEquals(25, loaded.minAge)
        assertTrue(loaded.maritalStatuses.isEmpty() && loaded.heritage.isEmpty() && loaded.timelines.isEmpty())
        assertEquals(0, loaded.minHeightCm)
        assertEquals(null, loaded.hasChildren)
        assertEquals(null, loaded.willingToRelocate)
        assertEquals("", loaded.profession)
        assertTrue(demo.community.discover().profiles.isNotEmpty())
    }
}
