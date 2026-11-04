package com.calaf.app.offline

import android.content.Context
import com.calaf.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Isolated, device-only demonstration. Never used by the Firebase application. */
class OfflineDemo(context: Context, storeName: String = "offline_demo_v1") : AuthRepository, CommunityRepository {
    private val preferencesStore = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
    private val photoRoot = File(context.filesDir, storeName)
    private val root = JSONObject(preferencesStore.getString("data", null) ?: "{}")
    val revision = MutableStateFlow(0)
    private fun table(name: String): JSONObject = root.optJSONObject(name) ?: JSONObject().also { root.put(name, it) }
    private fun rows(name: String) = table(name).let { t -> t.keys().asSequence().map { it to t.getJSONObject(it) }.toList() }
    private fun save() { preferencesStore.edit().putString("data", root.toString()).apply(); revision.value++ }
    private fun user(id: String) = table("users").optJSONObject(id)
    private fun account(id: String?): Account? = id?.let { uid -> user(uid)?.takeIf { !it.optBoolean("suspended") }?.let { Account(uid, it.getString("email"), true) } }
    override val accounts = MutableStateFlow<Account?>(null)
    val isAdmin get() = accounts.value?.id == "demo-admin"
    private fun uid(): String = accounts.value?.id?.takeIf { user(it)?.optBoolean("suspended") == false }
        ?: throw CommunityException(CommunityError.SIGN_IN_AGAIN)
    private fun own(id: String) { if (uid() != id) throw CommunityException(CommunityError.PERMISSION) }
    private fun admin() { if (!isAdmin) throw CommunityException(CommunityError.PERMISSION); uid() }
    // PBKDF2-HMAC-SHA1 is supported across the minimum Android API 24 and later.
    private fun passwordHash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 120000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded.joinToString("") { "%02x".format(it) } }
        finally { spec.clearPassword() }
    }
    init {
        if (!root.optBoolean("seeded")) {
            listOf("admin", "hassan", "amina", "hodan", "yusuf").forEachIndexed { index, name ->
                val id = "demo-$name"
                table("users").put(id, JSONObject().put("email", "$name@calaf.test").put("seed", true).put("suspended", false))
                table("profiles").put(id, JSONObject().put("name", name.replaceFirstChar { it.uppercase() } + " (Demo)")
                    .put("birthDate", "199${index + 1}-04-12").put("city", "London").put("country", "United Kingdom")
                    .put("culture", "Somali / East African").put("languages", "Somali, English")
                    .put("biography", "Fictional offline demonstration profile. Interested in family and a serious commitment.")
                    .put("gender", if (name in listOf("amina", "hodan")) "WOMAN" else "MAN")
                    .put("religion", "Islam").put("smoking", "NO").put("wantsChildren", "YES"))
            }
            newMatch("demo-admin", "demo-amina")
            newMatch("demo-hassan", "demo-amina")
            table("reports").put("sample-report", JSONObject().put("reporter", "demo-amina").put("target", "demo-yusuf")
                .put("category", "OTHER").put("details", "Sample report for trying moderation. No real person is involved.").put("status", "OPEN"))
            root.put("seeded", true); save()
        }
        accounts.value = account(preferencesStore.getString("session", null))
    }
    private fun signIn(id: String) { accounts.value = account(id); preferencesStore.edit().putString("session", id).apply(); revision.value++ }
    override suspend fun login(email: String, password: String) {
        val entry = rows("users").firstOrNull { it.second.optString("email").equals(email.trim(), true) }
            ?: throw AccountException(AccountError.Credentials)
        val (id, data) = entry
        val valid = if (data.optBoolean("seed")) password == if (id == "demo-admin") "CalafAdmin!2026" else "CalafDemo!2026"
            else withContext(Dispatchers.Default) { passwordHash(password, data.getString("salt")) == data.optString("hash") }
        if (!valid || data.optBoolean("suspended")) throw AccountException(AccountError.Credentials)
        signIn(id)
    }
    override suspend fun register(email: String, password: String) {
        if (!AccountValidation.email(email) || !AccountValidation.password(password)) throw AccountException(AccountError.InvalidInput)
        val normalized = email.trim().lowercase()
        if (rows("users").any { it.second.optString("email") == normalized }) throw AccountException(AccountError.EmailInUse)
        val id = UUID.randomUUID().toString(); val salt = UUID.randomUUID().toString()
        val hash = withContext(Dispatchers.Default) { passwordHash(password, salt) }
        if (rows("users").any { it.second.optString("email") == normalized }) throw AccountException(AccountError.EmailInUse)
        table("users").put(id, JSONObject().put("email", normalized).put("salt", salt).put("hash", hash).put("suspended", false))
        save(); signIn(id)
    }
    // There is no email transport or email verification in an offline demonstration.
    override suspend fun resetPassword(email: String) { throw AccountException(AccountError.Unavailable) }
    override suspend fun sendVerification() = Unit
    override suspend fun refresh(): Account? { accounts.value = account(accounts.value?.id); return accounts.value }
    override fun logout() { accounts.value = null; preferencesStore.edit().remove("session").apply(); revision.value++ }

    private fun JSONObject.draft() = ProfileDraft(optString("name"), optString("birthDate"), optString("city"), optString("country"),
        optString("languages"), optString("culture"), optString("biography"), enumValue(optString("gender"), Gender.UNSPECIFIED),
        optString("religion"), optString("occupation"), optString("education"), optString("interests"), optString("values"),
        enumValue(optString("children"), Answer.UNSPECIFIED), enumValue(optString("wantsChildren"), Answer.UNSPECIFIED),
        enumValue(optString("smoking"), Answer.UNSPECIFIED), enumValue(optString("relocation"), Answer.UNSPECIFIED),
        enumValue(optString("timeline"), MarriageTimeline.WHEN_READY))
    private fun ProfileDraft.json() = JSONObject().put("name", name).put("birthDate", birthDate).put("city", city).put("country", country)
        .put("languages", languages).put("culture", culture).put("biography", biography).put("gender", gender.name)
        .put("religion", religion).put("occupation", occupation).put("education", education).put("interests", interests).put("values", values)
        .put("children", children.name).put("wantsChildren", wantsChildren.name).put("smoking", smoking.name).put("relocation", relocation.name).put("timeline", timeline.name)
    val drafts = object : ProfileRepository {
        override suspend fun load(userId: String): ProfileDraft { own(userId); return (table("drafts").optJSONObject(userId) ?: table("profiles").optJSONObject(userId))?.draft() ?: ProfileDraft() }
        override suspend fun save(userId: String, profile: ProfileDraft) { own(userId); table("drafts").put(userId, profile.json()); save() }
    }
    private inline fun <reified T : Enum<T>> enumValue(value: String, default: T) = enumValues<T>().firstOrNull { it.name == value } ?: default
    private fun publicProfile(id: String, json: JSONObject): CommunityProfile {
        val p = json.draft()
        fun tags(s: String) = s.split(',').map(String::trim).filter(String::isNotBlank)
        return CommunityProfile(id, p.name, Period.between(LocalDate.parse(p.birthDate), LocalDate.now()).years,
            if (settingsValue(id).showCity) p.city else "", p.country, p.biography, p.culture, tags(p.languages), tags(p.interests), tags(p.values),
            p.gender, p.religion, p.occupation, p.education, p.children, p.wantsChildren, p.smoking, p.relocation, p.timeline)
    }
    private fun <T> observe(block: () -> T): Flow<T> = revision.map { block() }.distinctUntilChanged()
    override fun profile(userId: String) = observe { own(userId); table("profiles").optJSONObject(userId)?.let { publicProfile(userId, it) } }
    private fun settingsValue(id: String): PrivacySettings { val s = table("settings").optJSONObject(id) ?: JSONObject(); return PrivacySettings(s.optBoolean("visible", true), s.optBoolean("receipts"), s.optBoolean("city", true), s.optBoolean("notifications", true), s.optBoolean("analytics"), s.optBoolean("marketing")) }
    override fun settings(userId: String) = observe { own(userId); settingsValue(userId) }
    private fun preferenceValue(id: String): PartnerPreference { val p = table("preferences").optJSONObject(id) ?: JSONObject(); return PartnerPreference(p.optInt("min", 18), p.optInt("max", 70), Gender.entries.firstOrNull { it.name == p.optString("gender") }, p.optString("country"), p.optString("language"), p.optBoolean("nonSmoker"), Answer.entries.firstOrNull { it.name == p.optString("children") }) }
    override fun preferences(userId: String) = observe { own(userId); preferenceValue(userId) }
    override fun subscription(userId: String) = observe { own(userId); Subscription() }
    private fun pairId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")
    private fun newMatch(a: String, b: String): String {
        val id = pairId(a, b)
        if (!table("pairs").has(id)) {
            table("pairs").put(id, JSONObject().put("a", a).put("b", b).put("status", "ACTIVE"))
            listOf(a, b).forEach { member -> table("notifications").put(UUID.randomUUID().toString(), JSONObject().put("owner", member).put("category", "MATCH").put("target", id).put("time", System.currentTimeMillis())) }
        }
        return id
    }
    private fun pair(id: String): JSONObject {
        val p = table("pairs").optJSONObject(id) ?: throw CommunityException(CommunityError.PERMISSION)
        if (uid() !in listOf(p.optString("a"), p.optString("b"))) throw CommunityException(CommunityError.PERMISSION)
        return p
    }
    private fun activePair(id: String): JSONObject = pair(id).also { p ->
        if (p.optString("status") != "ACTIVE" || listOf(p.optString("a"), p.optString("b")).any { user(it)?.optBoolean("suspended") != false }) throw CommunityException(CommunityError.PERMISSION)
    }
    override fun matches(userId: String) = observe { own(userId); rows("pairs").filter { userId in listOf(it.second.optString("a"), it.second.optString("b")) }.map { (id, p) ->
        val other = if (p.optString("a") == userId) p.optString("b") else p.optString("a")
        Match(id, other, table("profiles").optJSONObject(other)?.optString("name") ?: "Demo member", if (user(other)?.optBoolean("suspended") == true) MatchStatus.CLOSED else enumValue(p.optString("status"), MatchStatus.CLOSED), p.optString("blockedBy") == userId,
            rows("messages").count { it.second.optString("pair") == id }.toLong())
    } }
    override fun messages(matchId: String) = observe { activePair(matchId); rows("messages").filter { it.second.optString("pair") == matchId }.map { (id, m) -> ChatMessage(id, m.getString("sender"), m.getString("text"), m.getLong("time"), emptyList()) }.sortedBy { it.createdAt }.takeLast(50) }
    override suspend fun earlierMessages(matchId: String, beforeId: String): List<ChatMessage> {
        activePair(matchId)
        val messages = rows("messages").filter { it.second.optString("pair") == matchId }.map { (id, m) -> ChatMessage(id, m.getString("sender"), m.getString("text"), m.getLong("time"), emptyList()) }.sortedBy { it.createdAt }
        return messages.takeWhile { it.id != beforeId }.takeLast(50)
    }
    override fun notifications(userId: String) = observe { own(userId); rows("notifications").filter { it.second.optString("owner") == userId }.map { (id, n) -> MemberNotification(id, n.getString("category"), n.getString("target"), n.getLong("time"), n.optBoolean("read")) }.sortedByDescending { it.createdAt } }
    override fun guardians(userId: String) = observe { own(userId); rows("guardians").filter { userId in listOf(it.second.optString("owner"), it.second.optString("guardian")) }.map { (id, g) -> Guardian(id, g.getString("owner"), g.optString("guardian").ifEmpty { null }, enumValue(g.optString("status"), GuardianStatus.INVITED)) } }
    override suspend fun discover(cursor: String?): DiscoveryPage {
        val me = uid(); val preference = preferenceValue(me)
        if (!settingsValue(me).visible) throw CommunityException(CommunityError.PERMISSION)
        return DiscoveryPage(rows("profiles").filter { (id, _) -> id != me && user(id)?.optBoolean("suspended") == false && settingsValue(id).visible && !table("choices").has("${me}_$id") && !table("pairs").has(pairId(me, id)) }
            .map { (id, data) -> publicProfile(id, data) }.filter { p -> p.age in preference.minAge..preference.maxAge && (preference.gender == null || preference.gender == p.gender) &&
                (preference.country.isBlank() || p.country.equals(preference.country, true)) && (preference.language.isBlank() || p.languages.any { it.equals(preference.language, true) }) &&
                (!preference.nonSmokerOnly || p.smoking == Answer.NO) && (preference.wantsChildren == null || p.wantsChildren == preference.wantsChildren) }, null)
    }
    override suspend fun execute(command: CommunityCommand): CommandResult {
        val me = uid(); var result = CommandResult()
        when (command) {
            is CommunityCommand.Publish -> { if (AccountValidation.profile(command.profile) != null) throw CommunityException(CommunityError.INVALID); table("profiles").put(me, command.profile.json()) }
            is CommunityCommand.Preferences -> with(command.value) { table("preferences").put(me, JSONObject().put("min", minAge).put("max", maxAge).put("gender", gender?.name).put("country", country).put("language", language).put("nonSmoker", nonSmokerOnly).put("children", wantsChildren?.name)) }
            is CommunityCommand.Privacy -> with(command.value) { table("settings").put(me, JSONObject().put("visible", visible).put("receipts", readReceipts).put("city", showCity).put("notifications", notifications).put("analytics", analytics).put("marketing", marketing)) }
            is CommunityCommand.Interest -> {
                if (command.userId == me || user(command.userId)?.optBoolean("suspended") != false || !table("profiles").has(command.userId)) throw CommunityException(CommunityError.INVALID)
                val existing = table("pairs").optJSONObject(pairId(me, command.userId))
                if (existing != null && existing.optString("status") != "ACTIVE") throw CommunityException(CommunityError.PERMISSION)
                table("choices").put("${me}_${command.userId}", command.like)
                // Seed profiles simulate reciprocal interest; device-created members must both like.
                if (command.like && (user(command.userId)?.optBoolean("seed") == true || table("choices").optBoolean("${command.userId}_$me"))) {
                    newMatch(me, command.userId); result = CommandResult(matched = true)
                }
            }
            is CommunityCommand.Send -> {
                val p = activePair(command.matchId)
                if (command.text.isBlank() || command.text.length > 2000) throw CommunityException(CommunityError.INVALID)
                if (!table("messages").has(command.messageId)) {
                    table("messages").put(command.messageId, JSONObject().put("pair", command.matchId).put("sender", me).put("text", command.text).put("time", System.currentTimeMillis()))
                    val other = if (p.optString("a") == me) p.optString("b") else p.optString("a")
                    table("notifications").put(UUID.randomUUID().toString(), JSONObject().put("owner", other).put("category", "MESSAGE").put("target", command.matchId).put("time", System.currentTimeMillis()))
                }
            }
            is CommunityCommand.Read -> { activePair(command.matchId) /* No remote delivery in the offline demo. */ }
            is CommunityCommand.ReadNotification -> { table("notifications").optJSONObject(command.id)?.takeIf { it.optString("owner") == me }?.put("read", true) }
            is CommunityCommand.Unmatch -> pair(command.matchId).put("status", "CLOSED")
            is CommunityCommand.Block -> { if (command.userId == me || user(command.userId) == null) throw CommunityException(CommunityError.INVALID); val id = pairId(me, command.userId); if (!table("pairs").has(id)) newMatch(me, command.userId); pair(id).put("status", "BLOCKED").put("blockedBy", me) }
            is CommunityCommand.Report -> { if (command.userId == me || user(command.userId) == null) throw CommunityException(CommunityError.INVALID); table("reports").put(UUID.randomUUID().toString(), JSONObject().put("reporter", me).put("target", command.userId).put("category", command.category.name).put("details", command.details.take(2000)).put("status", "OPEN")) }
            CommunityCommand.InviteGuardian -> { val code = UUID.randomUUID().toString(); table("guardians").put(code, JSONObject().put("owner", me).put("status", "INVITED")); result = CommandResult(invitationCode = code) }
            is CommunityCommand.AcceptGuardian -> { val g = table("guardians").optJSONObject(command.code); if (g == null || g.optString("owner") == me || g.optString("status") != "INVITED") throw CommunityException(CommunityError.INVALID); g.put("guardian", me).put("status", "ACCEPTED") }
            is CommunityCommand.RevokeGuardian -> { val g = table("guardians").optJSONObject(command.id); if (g == null || me !in listOf(g.optString("owner"), g.optString("guardian"))) throw CommunityException(CommunityError.PERMISSION); g.put("status", "REVOKED") }
            CommunityCommand.RevokeSessions -> logout()
            CommunityCommand.RequestDeletion -> user(me)?.put("deletionRequested", true)
            CommunityCommand.ExportData -> result = CommandResult(exportJson = JSONObject().put("email", user(me)?.optString("email")).put("profile", table("profiles").optJSONObject(me)).put("note", "Partial offline demo export; no server records exist.").toString(2))
        }
        save(); return result
    }

    data class AdminMember(val id: String, val email: String, val suspended: Boolean)
    data class AdminReport(val id: String, val target: String, val details: String, val status: String)
    fun adminMembers(): List<AdminMember> { admin(); return rows("users").map { (id, u) -> AdminMember(id, u.getString("email"), u.optBoolean("suspended")) } }
    fun adminReports(): List<AdminReport> { admin(); return rows("reports").map { (id, r) -> AdminReport(id, r.getString("target"), "${r.optString("category")}: ${r.optString("details")}", r.getString("status")) } }
    private fun audit(action: String, target: String) { table("audit").put(UUID.randomUUID().toString(), JSONObject().put("actor", uid()).put("action", action).put("target", target).put("time", System.currentTimeMillis())) }
    fun suspendMember(id: String, suspended: Boolean) { admin(); if (id == "demo-admin" || user(id) == null) throw CommunityException(CommunityError.INVALID); user(id)!!.put("suspended", suspended); audit(if (suspended) "SUSPEND" else "RESTORE", id); save() }
    fun resolveReport(id: String, dismiss: Boolean) { admin(); val report = table("reports").optJSONObject(id) ?: throw CommunityException(CommunityError.INVALID); if (report.optString("status") != "OPEN") throw CommunityException(CommunityError.INVALID); report.put("status", if (dismiss) "DISMISSED" else "RESOLVED"); audit(if (dismiss) "DISMISS_REPORT" else "RESOLVE_REPORT", id); save() }
    fun auditCount(): Int { admin(); return table("audit").length() }
    fun photos(owner: String) = object : PhotoRepository {
        private fun file(slot: Int): File { own(owner); require(slot in 0..5); return File(File(photoRoot, owner), "photo-$slot.jpg") }
        override suspend fun slots(): List<Int> = withContext(Dispatchers.IO) { (0..5).filter { file(it).exists() } }
        override suspend fun read(slot: Int): ByteArray = withContext(Dispatchers.IO) { file(slot).readBytes() }
        override suspend fun upload(slot: Int, jpeg: ByteArray) = withContext(Dispatchers.IO) { require(jpeg.isNotEmpty() && jpeg.size < 5 * 1024 * 1024); val target = file(slot); require(!target.exists()); target.parentFile!!.mkdirs(); target.writeBytes(jpeg) }
        override suspend fun delete(slot: Int) { withContext(Dispatchers.IO) { val target = file(slot); check(!target.exists() || target.delete()) } }
    }
}
