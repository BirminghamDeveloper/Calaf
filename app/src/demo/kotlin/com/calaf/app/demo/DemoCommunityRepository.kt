package com.calaf.app.demo

import com.calaf.app.demo.DemoProfileCodec.enumValue
import com.calaf.core.model.Answer
import com.calaf.core.model.ChatMessage
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityCommand
import com.calaf.core.model.CommunityException
import com.calaf.core.model.CommunityProfile
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.DiscoveryPage
import com.calaf.core.model.Gender
import com.calaf.core.model.Guardian
import com.calaf.core.model.GuardianStatus
import com.calaf.core.model.Match
import com.calaf.core.model.MatchStatus
import com.calaf.core.model.MemberNotification
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PhotoState
import com.calaf.core.model.PrivacySettings
import com.calaf.core.model.PublicPhoto
import com.calaf.core.model.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.time.LocalDate
import java.time.Period

/** Reads for the demo community. Writes live in [DemoCommandHandler]. */
class DemoCommunityRepository(
    private val store: DemoStore,
    private val session: DemoSession,
) : CommunityRepository {
    private val commands = DemoCommandHandler(store, session)

    private fun <T> observe(block: () -> T): Flow<T> = store.revision.map { block() }.distinctUntilChanged()

    override fun profile(userId: String) = observe {
        session.requireOwner(userId)
        store.table("profiles").optJSONObject(userId)?.let { publicProfile(userId, it) }
    }

    override fun settings(userId: String) = observe {
        session.requireOwner(userId)
        privacy(userId)
    }

    override fun preferences(userId: String) = observe {
        session.requireOwner(userId)
        preference(userId)
    }

    override fun subscription(userId: String) = observe {
        session.requireOwner(userId)
        Subscription()
    }

    override fun matches(userId: String) = observe {
        session.requireOwner(userId)
        store.rows("pairs").filter { (_, pair) -> userId in members(pair) }.map { (id, pair) ->
            val other = if (pair.optString("a") == userId) pair.optString("b") else pair.optString("a")
            val status = if (store.user(other)?.optBoolean("suspended") == true) {
                MatchStatus.CLOSED
            } else {
                enumValue(pair.optString("status"), MatchStatus.CLOSED)
            }
            Match(
                id, other, store.table("profiles").optJSONObject(other)?.optString("name") ?: "Demo member", status,
                pair.optString("blockedBy") == userId, messageRows(id).size.toLong(),
            )
        }
    }

    override fun messages(matchId: String) = observe {
        requireActivePair(matchId)
        messageRows(matchId).takeLast(PAGE_SIZE)
    }

    override suspend fun earlierMessages(matchId: String, beforeId: String): List<ChatMessage> {
        requireActivePair(matchId)
        return messageRows(matchId).takeWhile { it.id != beforeId }.takeLast(PAGE_SIZE)
    }

    override fun notifications(userId: String) = observe {
        session.requireOwner(userId)
        store.rows("notifications").filter { it.second.optString("owner") == userId }.map { (id, n) ->
            MemberNotification(
                id, n.getString("category"), n.getString("target"), n.getLong("time"), n.optBoolean("read"),
            )
        }.sortedByDescending { it.createdAt }
    }

    override fun guardians(userId: String) = observe {
        session.requireOwner(userId)
        store.rows("guardians")
            .filter { (_, g) -> userId in listOf(g.optString("owner"), g.optString("guardian")) }
            .map { (id, g) ->
                Guardian(
                    id, g.getString("owner"), g.optString("guardian").ifEmpty { null },
                    enumValue(g.optString("status"), GuardianStatus.INVITED),
                )
            }
    }

    override suspend fun discover(cursor: String?): DiscoveryPage {
        val me = session.requireUid()
        val preference = preference(me)
        if (!privacy(me).visible) throw CommunityException(CommunityError.PERMISSION)
        val candidates = store.rows("profiles").filter { (id, _) -> isCandidate(me, id) }
            .map { (id, data) -> publicProfile(id, data) }
            .filter { matchesPreference(it, preference) }
        return DiscoveryPage(candidates, null)
    }

    override suspend fun execute(command: CommunityCommand) = commands.execute(command)

    private fun isCandidate(me: String, id: String) = id != me &&
        !store.isSuspended(id) &&
        privacy(id).visible &&
        !store.table("choices").has("${me}_$id") &&
        !store.table("pairs").has(store.pairId(me, id)) &&
        !store.isBlockedEitherWay(me, id)

    private fun matchesPreference(p: CommunityProfile, want: PartnerPreference) =
        p.age in want.minAge..want.maxAge &&
            (want.gender == null || want.gender == p.gender) &&
            (want.country.isBlank() || p.country.equals(want.country, true)) &&
            (want.language.isBlank() || p.languages.any { it.equals(want.language, true) }) &&
            (!want.nonSmokerOnly || p.smoking == Answer.NO) &&
            (want.wantsChildren == null || p.wantsChildren == want.wantsChildren)

    private fun members(pair: JSONObject) = listOf(pair.optString("a"), pair.optString("b"))

    private fun requireActivePair(id: String) {
        val pair = store.table("pairs").optJSONObject(id) ?: throw CommunityException(CommunityError.PERMISSION)
        if (session.requireUid() !in members(pair)) throw CommunityException(CommunityError.PERMISSION)
        if (pair.optString("status") != "ACTIVE" || members(pair).any { store.isSuspended(it) }) {
            throw CommunityException(CommunityError.PERMISSION)
        }
    }

    private fun messageRows(matchId: String): List<ChatMessage> =
        store.rows("messages").filter { it.second.optString("pair") == matchId }.map { (id, m) ->
            ChatMessage(id, m.getString("sender"), m.getString("text"), m.getLong("time"), emptyList())
        }.sortedBy { it.createdAt }

    private fun privacy(id: String): PrivacySettings {
        val s = store.table("settings").optJSONObject(id) ?: JSONObject()
        return PrivacySettings(
            s.optBoolean("visible", true), s.optBoolean("receipts"), s.optBoolean("city", true),
            s.optBoolean("notifications", true), s.optBoolean("analytics"), s.optBoolean("marketing"),
        )
    }

    private fun preference(id: String): PartnerPreference {
        val p = store.table("preferences").optJSONObject(id) ?: JSONObject()
        return PartnerPreference(
            p.optInt("min", 18), p.optInt("max", 70),
            Gender.entries.firstOrNull { it.name == p.optString("gender") },
            p.optString("country"), p.optString("language"), p.optBoolean("nonSmoker"),
            Answer.entries.firstOrNull { it.name == p.optString("children") },
        )
    }

    private fun publicProfile(id: String, json: JSONObject): CommunityProfile {
        val p = DemoProfileCodec.toDraft(json)
        fun tags(text: String) = text.split(',').map(String::trim).filter(String::isNotBlank)
        val age = Period.between(LocalDate.parse(p.birthDate), LocalDate.now()).years
        return CommunityProfile(
            id, p.name, age, if (privacy(id).showCity) p.city else "", p.country, p.biography, p.culture,
            tags(p.languages), tags(p.interests), tags(p.values), p.gender, p.religion, p.occupation, p.education,
            p.children, p.wantsChildren, p.smoking, p.relocation, p.timeline,
            maritalStatus = p.maritalStatus, heightCm = p.heightCm, countryOfBirth = p.countryOfBirth,
            religiosity = p.religiosity, prays = p.prays, heritage = p.heritage,
            // Only approved photos are ever visible to other members, main photo first.
            photos = DemoPhotoRows.ownerPhotos(store, id).filter { it.state == PhotoState.APPROVED }
                .map { PublicPhoto(it.id, it.blurForNonMatches) },
        )
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
