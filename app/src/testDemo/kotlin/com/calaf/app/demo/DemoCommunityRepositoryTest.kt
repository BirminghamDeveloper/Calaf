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
        val photos = demo.community.discover().profiles.first { it.id == "demo-yusuf" }.photos
        assertTrue(photos.size in 2..3)
        assertEquals(DemoPhotoRows.ownerPhotos(demo.store, "demo-yusuf").map { it.id }, photos.map { it.id })
        assertTrue(photos.none { it.blurred })
        val firstId = photos.first().id
        DemoPhotoRows.row(demo.store, firstId)!!.put("blur", true)
        assertTrue(demo.community.discover().profiles.first { it.id == "demo-yusuf" }.photos.first().blurred)
        DemoPhotoRows.row(demo.store, firstId)!!.put("state", "PENDING")
        assertFalse(firstId in demo.community.discover().profiles.first { it.id == "demo-yusuf" }.photos.map { it.id })
    }

    @Test fun `saved phase 2 filters narrow discovery and survive a restart`() = runBlocking<Unit> {
        val demo = fixture()
        // Amina is already matched with Hassan and the admin, so her candidates are Hodan and Yusuf.
        demo.loginAsMember("amina")
        fun ids() = runBlocking { demo.community.discover().profiles.map { it.id }.sorted() }
        assertEquals(listOf("demo-hodan", "demo-yusuf"), ids())
        val filters = com.calaf.core.model.PartnerPreference(
            heritage = setOf(com.calaf.core.model.Heritage.SOMALILAND),
            maritalStatuses = setOf(com.calaf.core.model.MaritalStatus.NEVER_MARRIED),
            minHeightCm = 160,
        )
        demo.community.execute(CommunityCommand.Preferences(filters))
        assertEquals(listOf("demo-hodan"), ids())
        val reopened = demo.reopen()
        reopened.auth.refresh()
        assertEquals(filters, reopened.community.preferences("demo-amina").first())
        demo.community.execute(CommunityCommand.Preferences(filters.copy(minHeightCm = 180, heritage = emptySet())))
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

    private fun newMemberDraft(name: String = "Newcomer") =
        com.calaf.core.model.ProfileDraft(name, "1994-05-06", "Leeds", "United Kingdom")

    @Test fun `liked you lists admirers with their compliment, and liking back makes a match with its id`() =
        runBlocking<Unit> {
            val demo = fixture()
            demo.loginAsMember("hodan")
            val liked = demo.community.likedYou()
            assertEquals(1, liked.count)
            assertTrue(liked.simulated && !liked.locked)
            val yusuf = liked.profiles.single()
            assertEquals("demo-yusuf", yusuf.id)
            assertTrue(yusuf.compliment!!.note.startsWith("Salaam Hodan"))
            assertEquals(emptyList<com.calaf.core.model.SafetyWarning>(), yusuf.compliment!!.warnings)
            val result = demo.community.execute(CommunityCommand.Interest("demo-yusuf", like = true))
            assertTrue(result.matched)
            assertEquals(demo.store.pairId("demo-hodan", "demo-yusuf"), result.matchId)
            assertEquals(0, demo.community.likedYou().count)
        }

    @Test fun `passing or blocking removes someone from liked you`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hodan")
        demo.community.execute(CommunityCommand.Interest("demo-yusuf", like = false))
        assertEquals(0, demo.community.likedYou().count)
        demo.auth.logout()
        demo.loginAsMember("hassan")
        assertEquals(listOf("demo-hodan"), demo.community.likedYou().profiles.map { it.id })
        demo.community.execute(CommunityCommand.Block("demo-hodan"))
        assertEquals(0, demo.community.likedYou().count)
    }

    @Test fun `a compliment shows on the receivers card with safety warnings and only while the like stands`() =
        runBlocking<Unit> {
            val demo = fixture()
            demo.auth.register("a@example.test", "DemoPass!2026")
            val a = demo.session.requireUid()
            demo.community.execute(CommunityCommand.Publish(newMemberDraft("Alpha Member")))
            demo.auth.logout()
            demo.auth.register("b@example.test", "DemoPass!2026")
            val b = demo.session.requireUid()
            demo.community.execute(CommunityCommand.Publish(newMemberDraft("Beta Member")))
            demo.auth.logout()
            demo.auth.login("a@example.test", "DemoPass!2026")
            demo.community.execute(
                CommunityCommand.Interest(b, like = true, note = "  Lovely profile, mail me a@example.org  "),
            )
            demo.auth.logout()
            demo.auth.login("b@example.test", "DemoPass!2026")
            val card = demo.community.likedYou().profiles.first { it.id == a }.compliment!!
            assertEquals("Lovely profile, mail me a@example.org", card.note)
            assertEquals(listOf(com.calaf.core.model.SafetyWarning.PERSONAL_INFORMATION), card.warnings)
            assertEquals(card, demo.community.discover().profiles.first { it.id == a }.compliment)
            // Liking back matches. The note is no longer a pending like, so it stops showing.
            assertTrue(demo.community.execute(CommunityCommand.Interest(a, like = true)).matched)
            assertEquals(null, demo.community.likedYou().profiles.firstOrNull { it.id == a })
        }

    @Test fun `a compliment must be one to 140 characters and only goes with a like`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val tooLong = CommunityCommand.Interest("demo-hodan", like = true, note = "x".repeat(141))
        assertTrue(runCatching { demo.community.execute(tooLong) }.exceptionOrNull() is com.calaf.core.model.CommunityException)
        assertTrue(demo.store.table("notes").length() == 0 || !demo.store.table("notes").has("demo-hassan_demo-hodan"))
        demo.community.execute(CommunityCommand.Interest("demo-hodan", like = true, note = "x".repeat(140)))
        assertEquals(140, demo.store.table("notes").getJSONObject("demo-hassan_demo-hodan").getString("note").length)
        demo.community.execute(CommunityCommand.Interest("demo-yusuf", like = false, note = "ignored"))
        assertFalse(demo.store.table("notes").has("demo-hassan_demo-yusuf"))
    }

    @Test fun `a new member gets simulated admirers once, when they first publish`() = runBlocking<Unit> {
        val demo = fixture()
        demo.auth.register("new@example.test", "DemoPass!2026")
        assertEquals(0, demo.community.likedYou().count)
        demo.community.execute(CommunityCommand.Publish(newMemberDraft()))
        val liked = demo.community.likedYou()
        assertEquals(setOf("demo-hodan", "demo-yusuf"), liked.profiles.map { it.id }.toSet())
        assertTrue(liked.profiles.single { it.id == "demo-hodan" }.compliment != null)
        assertEquals(null, liked.profiles.single { it.id == "demo-yusuf" }.compliment)
        demo.community.execute(CommunityCommand.Interest("demo-hodan", like = true))
        demo.community.execute(CommunityCommand.Publish(newMemberDraft("Newcomer Two")))
        assertEquals(listOf("demo-yusuf"), demo.community.likedYou().profiles.map { it.id })
    }
}
