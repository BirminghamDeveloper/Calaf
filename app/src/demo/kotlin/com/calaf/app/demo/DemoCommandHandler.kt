package com.calaf.app.demo

import com.calaf.core.model.AccountValidation
import com.calaf.core.model.CommandResult
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import org.json.JSONObject
import java.util.UUID

/** Applies the writes a member can make. Everything is checked against the signed in member first. */
internal class DemoCommandHandler(private val store: DemoStore, private val session: DemoSession) {
    fun execute(command: CommunityCommand): CommandResult {
        val me = session.requireUid()
        var result = CommandResult()
        when (command) {
            is CommunityCommand.Publish -> publish(me, command)
            is CommunityCommand.Preferences -> savePreferences(me, command)
            is CommunityCommand.Privacy -> savePrivacy(me, command)
            is CommunityCommand.Interest -> result = interest(me, command)
            is CommunityCommand.Send -> send(me, command)
            // There is no remote delivery in the offline demo, so a read receipt has nothing to do.
            is CommunityCommand.Read -> requireActiveMatch(me, command.matchId)
            is CommunityCommand.ReadNotification -> markRead(me, command.id)
            is CommunityCommand.Unmatch -> requireMatchMember(me, command.matchId).put("status", "CLOSED")
            is CommunityCommand.Block -> block(me, command.userId)
            is CommunityCommand.Report -> report(me, command)
            CommunityCommand.InviteGuardian -> result = inviteGuardian(me)
            is CommunityCommand.AcceptGuardian -> acceptGuardian(me, command.code)
            is CommunityCommand.RevokeGuardian -> revokeGuardian(me, command.id)
            CommunityCommand.RevokeSessions -> session.signOut()
            CommunityCommand.RequestDeletion -> store.user(me)?.put("deletionRequested", true)
            CommunityCommand.ExportData -> result = export(me)
        }
        store.save()
        return result
    }

    private fun publish(me: String, command: CommunityCommand.Publish) {
        if (AccountValidation.profile(command.profile) != null) throw CommunityException(CommunityError.INVALID)
        store.table("profiles").put(me, DemoProfileCodec.toJson(command.profile))
    }

    private fun savePreferences(me: String, command: CommunityCommand.Preferences) {
        store.table("preferences").put(me, DemoPreferenceCodec.toJson(command.value))
    }

    private fun savePrivacy(me: String, command: CommunityCommand.Privacy) = with(command.value) {
        store.table("settings").put(
            me,
            JSONObject().put("visible", visible).put("receipts", readReceipts).put("city", showCity)
                .put("notifications", notifications).put("analytics", analytics).put("marketing", marketing),
        )
    }

    private fun interest(me: String, command: CommunityCommand.Interest): CommandResult {
        val other = command.userId
        if (other == me || store.isSuspended(other) || !store.table("profiles").has(other)) {
            throw CommunityException(CommunityError.INVALID)
        }
        if (store.isBlockedEitherWay(me, other)) throw CommunityException(CommunityError.PERMISSION)
        val existing = store.table("pairs").optJSONObject(store.pairId(me, other))
        if (existing != null && existing.optString("status") != "ACTIVE") {
            throw CommunityException(CommunityError.PERMISSION)
        }
        store.table("choices").put("${me}_$other", command.like)
        // Seed profiles simulate reciprocal interest. Device created members must both like.
        val reciprocal = store.user(other)?.optBoolean("seed") == true ||
            store.table("choices").optBoolean("${other}_$me")
        if (command.like && reciprocal) {
            store.createMatch(me, other)
            return CommandResult(matched = true)
        }
        return CommandResult()
    }

    private fun send(me: String, command: CommunityCommand.Send) {
        val pair = requireActiveMatch(me, command.matchId)
        if (command.text.isBlank() || command.text.length > MAX_MESSAGE) {
            throw CommunityException(CommunityError.INVALID)
        }
        // The message id makes sending idempotent: a repeat of the same id is ignored.
        if (store.table("messages").has(command.messageId)) return
        store.table("messages").put(
            command.messageId,
            JSONObject().put("pair", command.matchId).put("sender", me).put("text", command.text)
                .put("time", System.currentTimeMillis()),
        )
        val other = if (pair.optString("a") == me) pair.optString("b") else pair.optString("a")
        store.addNotification(other, "MESSAGE", command.matchId)
    }

    private fun markRead(me: String, id: String) {
        store.table("notifications").optJSONObject(id)?.takeIf { it.optString("owner") == me }?.put("read", true)
    }

    /**
     * Records the block on its own. Blocking someone you never matched with must not create a pair or a MATCH
     * notification. An existing pair is closed as BLOCKED so its conversation stops.
     */
    private fun block(me: String, other: String) {
        if (other == me || store.user(other) == null) throw CommunityException(CommunityError.INVALID)
        store.table("blocks").put("${me}_$other", true)
        store.table("pairs").optJSONObject(store.pairId(me, other))?.put("status", "BLOCKED")?.put("blockedBy", me)
    }

    private fun report(me: String, command: CommunityCommand.Report) {
        if (command.userId == me || store.user(command.userId) == null) throw CommunityException(CommunityError.INVALID)
        store.table("reports").put(
            UUID.randomUUID().toString(),
            JSONObject().put("reporter", me).put("target", command.userId).put("category", command.category.name)
                .put("details", command.details.take(MAX_MESSAGE)).put("status", "OPEN"),
        )
    }

    private fun inviteGuardian(me: String): CommandResult {
        val code = UUID.randomUUID().toString()
        store.table("guardians").put(code, JSONObject().put("owner", me).put("status", "INVITED"))
        return CommandResult(invitationCode = code)
    }

    private fun acceptGuardian(me: String, code: String) {
        val row = store.table("guardians").optJSONObject(code)
        if (row == null || row.optString("owner") == me || row.optString("status") != "INVITED") {
            throw CommunityException(CommunityError.INVALID)
        }
        row.put("guardian", me).put("status", "ACCEPTED")
    }

    private fun revokeGuardian(me: String, id: String) {
        val row = store.table("guardians").optJSONObject(id)
        if (row == null || me !in listOf(row.optString("owner"), row.optString("guardian"))) {
            throw CommunityException(CommunityError.PERMISSION)
        }
        row.put("status", "REVOKED")
    }

    private fun export(me: String) = CommandResult(
        exportJson = JSONObject().put("email", store.user(me)?.optString("email"))
            .put("profile", store.table("profiles").optJSONObject(me))
            .put("note", "Partial offline demo export; no server records exist.").toString(2),
    )

    private fun requireMatchMember(me: String, matchId: String): JSONObject {
        val pair = store.table("pairs").optJSONObject(matchId) ?: throw CommunityException(CommunityError.PERMISSION)
        if (me !in listOf(pair.optString("a"), pair.optString("b"))) throw CommunityException(CommunityError.PERMISSION)
        return pair
    }

    private fun requireActiveMatch(me: String, matchId: String): JSONObject {
        val pair = requireMatchMember(me, matchId)
        val suspended = listOf(pair.optString("a"), pair.optString("b")).any { store.isSuspended(it) }
        if (pair.optString("status") != "ACTIVE" || suspended) throw CommunityException(CommunityError.PERMISSION)
        return pair
    }

    private companion object {
        const val MAX_MESSAGE = 2000
    }
}
