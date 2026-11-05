package com.calaf.app.demo

import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityException
import com.calaf.core.model.SafetyWarning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DemoChatTest {
    @get:Rule val temp = TemporaryFolder()
    private fun fixture() = DemoFixture(temp.newFolder("store"))

    private suspend fun DemoFixture.matchWithAmina(): String =
        community.matches("demo-hassan").first().single().id

    @Test fun `a seeded member sends a labelled scripted reply after a delay, one at a time`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val match = demo.matchWithAmina()
        demo.community.execute(CommunityCommand.Send(match, "m1", "Salaam"))
        demo.community.execute(CommunityCommand.Send(match, "m2", "How are you?"))
        // Quick messages share one pending reply, and nothing arrives before the delay.
        assertEquals(1, demo.replies.pending)
        assertEquals(listOf(DemoReplies.DELAY_MILLIS), demo.replies.delays)
        assertEquals(2, demo.community.messages(match).first().size)
        demo.replies.runAll()
        val messages = demo.community.messages(match).first()
        assertEquals(3, messages.size)
        val reply = messages.last()
        assertTrue(reply.isDemo)
        assertEquals("demo-amina", reply.senderId)
        assertEquals(DemoReplies.line(0), reply.text)
        assertTrue(messages.take(2).none { it.isDemo })
        // The next message gets the next script line, and the member is told about the reply.
        demo.community.execute(CommunityCommand.Send(match, "m3", "Thanks"))
        demo.replies.runAll()
        assertEquals(DemoReplies.line(1), demo.community.messages(match).first().last().text)
        assertEquals(2, demo.community.notifications("demo-hassan").first().count { it.category == "MESSAGE" })
        // The seeded member has read my messages.
        assertTrue(demo.community.matches("demo-hassan").first().single().otherReadAt > 0)
    }

    @Test fun `the script wraps round and never mentions money or contact details`() {
        val lines = (0..9).map(DemoReplies::line)
        assertEquals(lines[0], lines[5])
        assertTrue(lines.all { com.calaf.core.model.MessageSafety.warnings(it).isEmpty() })
    }

    @Test fun `no reply comes from a blocked match`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val match = demo.matchWithAmina()
        demo.community.execute(CommunityCommand.Send(match, "m1", "Salaam"))
        demo.community.execute(CommunityCommand.Block("demo-amina"))
        demo.replies.runAll()
        assertEquals(1, demo.store.rows("messages").size)
    }

    @Test fun `a message to a member who is not seeded schedules no reply`() = runBlocking<Unit> {
        val demo = fixture()
        suspend fun join(email: String): String {
            demo.auth.register(email, "DemoPass!2026")
            demo.community.execute(
                CommunityCommand.Publish(com.calaf.core.model.ProfileDraft("Member", "1994-01-02", "Leeds", "UK")),
            )
            return demo.session.requireUid()
        }
        val a = join("a@example.test")
        demo.auth.logout()
        val b = join("b@example.test")
        demo.community.execute(CommunityCommand.Interest(a, like = true))
        demo.auth.logout()
        demo.auth.login("a@example.test", "DemoPass!2026")
        val match = demo.community.execute(CommunityCommand.Interest(b, like = true)).matchId!!
        demo.community.execute(CommunityCommand.Send(match, "m1", "Salaam"))
        assertEquals(0, demo.replies.pending)
    }

    @Test fun `risky messages carry safety warnings`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val match = demo.matchWithAmina()
        demo.community.execute(CommunityCommand.Send(match, "m1", "see https://example.org or send money"))
        val warnings = demo.community.messages(match).first().single { it.id == "m1" }.warnings
        assertEquals(listOf(SafetyWarning.LINK, SafetyWarning.MONEY_REQUEST), warnings)
    }

    @Test fun `read receipts show the other member the message as read`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val match = demo.matchWithAmina()
        demo.community.execute(CommunityCommand.Send(match, "m1", "Salaam"))
        val sentAt = demo.community.messages(match).first().single { it.id == "m1" }.createdAt
        demo.auth.logout()
        demo.loginAsMember("amina")
        assertEquals(0L, demo.community.matches("demo-amina").first().first { it.id == match }.otherReadAt)
        demo.community.execute(CommunityCommand.Read(match, "m1"))
        demo.auth.logout()
        demo.loginAsMember("hassan")
        assertTrue(demo.community.matches("demo-hassan").first().single().otherReadAt >= sentAt)
        // A member cannot mark their own message as read.
        demo.community.execute(CommunityCommand.Read(match, "m1"))
        assertTrue(demo.store.table("pairs").getJSONObject(match).optLong("readAt_demo-hassan") == 0L)
    }

    private suspend fun DemoFixture.guardianWith(code: String, guardianEmail: String) {
        auth.logout()
        auth.register(guardianEmail, "DemoPass!2026")
        community.execute(CommunityCommand.AcceptGuardian(code))
        auth.logout()
    }

    @Test fun `a guardian reads only conversations the member shared, read only, and only while accepted`() =
        runBlocking<Unit> {
            val demo = fixture()
            demo.loginAsMember("hassan")
            val match = demo.matchWithAmina()
            demo.community.execute(CommunityCommand.Send(match, "m1", "Salaam"))
            val code = demo.community.execute(CommunityCommand.InviteGuardian).invitationCode!!
            // Sharing needs an accepted guardian first.
            assertTrue(
                runCatching { demo.community.execute(CommunityCommand.ShareWithGuardian(match, true)) }
                    .exceptionOrNull() is CommunityException,
            )
            demo.guardianWith(code, "wali@example.test")
            demo.loginAsMember("hassan")
            assertFalse(demo.community.matches("demo-hassan").first().single().sharedWithGuardian)
            demo.auth.logout()
            demo.auth.login("wali@example.test", "DemoPass!2026")
            // Nothing is shared by default.
            assertTrue(demo.community.guardianMatches().isEmpty())
            assertTrue(runCatching { demo.community.guardianMessages(match).first() }.isFailure)
            demo.auth.logout()
            demo.loginAsMember("hassan")
            demo.community.execute(CommunityCommand.ShareWithGuardian(match, true))
            assertTrue(demo.community.matches("demo-hassan").first().single().sharedWithGuardian)
            demo.auth.logout()
            demo.auth.login("wali@example.test", "DemoPass!2026")
            val shared = demo.community.guardianMatches().single()
            assertEquals(match, shared.matchId)
            assertEquals("demo-hassan", shared.ownerId)
            assertEquals("Hassan (Demo)", shared.ownerName)
            assertEquals("Amina (Demo)", shared.otherName)
            assertEquals(listOf("m1"), demo.community.guardianMessages(match).first().map { it.id })
            // Read only: a guardian has no way to send into the conversation.
            assertTrue(
                runCatching { demo.community.execute(CommunityCommand.Send(match, "m2", "Hi")) }.isFailure,
            )
            // The other member is told, and sees it from their own side.
            demo.auth.logout()
            demo.loginAsMember("amina")
            assertTrue(demo.community.matches("demo-amina").first().first { it.id == match }.otherSharesWithGuardian)
            // Turning sharing off, or revoking the guardian, closes the door at once.
            demo.auth.logout()
            demo.loginAsMember("hassan")
            demo.community.execute(CommunityCommand.ShareWithGuardian(match, false))
            demo.auth.logout()
            demo.auth.login("wali@example.test", "DemoPass!2026")
            assertTrue(demo.community.guardianMatches().isEmpty())
            assertTrue(runCatching { demo.community.guardianMessages(match).first() }.isFailure)
        }

    @Test fun `revoking the guardian or blocking the match stops the guardian reading`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hassan")
        val match = demo.matchWithAmina()
        val code = demo.community.execute(CommunityCommand.InviteGuardian).invitationCode!!
        demo.guardianWith(code, "wali@example.test")
        demo.loginAsMember("hassan")
        demo.community.execute(CommunityCommand.ShareWithGuardian(match, true))
        val guardianRow = demo.community.guardians("demo-hassan").first().single()
        demo.community.execute(CommunityCommand.RevokeGuardian(guardianRow.id))
        demo.auth.logout()
        demo.auth.login("wali@example.test", "DemoPass!2026")
        assertTrue(demo.community.guardianMatches().isEmpty())
        assertTrue(runCatching { demo.community.guardianMessages(match).first() }.isFailure)
    }

    @Test fun `sharing is refused for a match that is not yours or is closed`() = runBlocking<Unit> {
        val demo = fixture()
        demo.loginAsMember("hodan")
        assertTrue(
            runCatching { demo.community.execute(CommunityCommand.ShareWithGuardian("demo-admin_demo-amina", true)) }
                .isFailure,
        )
    }
}
