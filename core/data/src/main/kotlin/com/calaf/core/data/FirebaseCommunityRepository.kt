package com.calaf.core.data

import com.calaf.core.model.*
import com.google.firebase.firestore.*
import com.google.firebase.functions.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import com.google.firebase.FirebaseApp

fun createCommunityRepository(emulator: Boolean = false, app: FirebaseApp = FirebaseApp.getInstance(), emulatorHost: String = "10.0.2.2"): CommunityRepository {
    val db = FirebaseFirestore.getInstance(app)
    val functions = FirebaseFunctions.getInstance(app, "europe-west2")
    if (emulator) {
        db.useEmulator(emulatorHost, 8080)
        functions.useEmulator(emulatorHost, 5001)
    }
    return FirebaseCommunityRepository(db, functions, emulator)
}

class FirebaseCommunityRepository(private val db: FirebaseFirestore, private val functions: FirebaseFunctions, private val localDiagnostics: Boolean = false) : CommunityRepository {
    private fun document(path: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = db.document(path).addSnapshotListener { value, error ->
            if (error != null) close(failure(error)) else trySend(value?.data)
        }
        awaitClose { listener.remove() }
    }
    private fun query(query: Query): Flow<List<Pair<String, Map<String, Any>>>> = callbackFlow {
        val listener = query.addSnapshotListener { value, error ->
            if (error != null) close(failure(error)) else trySend(value?.documents.orEmpty().map { it.id to it.data.orEmpty() })
        }
        awaitClose { listener.remove() }
    }
    override fun profile(userId: String) = document("profiles/$userId").map { it?.let { data -> profileFrom(userId, data) } }
    override fun settings(userId: String) = document("settings/$userId").map { raw ->
        val data = raw.orEmpty()
        PrivacySettings(data["visible"] != false, data["readReceipts"] == true, data["showCity"] != false,
            data["notifications"] != false, data["analytics"] == true, data["marketing"] == true)
    }
    override fun preferences(userId: String) = document("preferences/$userId").map { raw ->
        val data = raw.orEmpty()
        PartnerPreference(data.number("minAge", 18).toInt(), data.number("maxAge", 70).toInt(),
            enumValues<Gender>().firstOrNull { it.name == data["gender"] }, data.string("country"), data.string("language"),
            data["nonSmokerOnly"] == true, enumValues<Answer>().firstOrNull { it.name == data["wantsChildren"] })
    }
    override fun matches(userId: String) = query(db.collection("pairs").whereArrayContains("members", userId)).map { docs ->
        docs.mapNotNull { (id, data) ->
            val other = data.strings("members").firstOrNull { it != userId } ?: return@mapNotNull null
            Match(id, other, (data["names"] as? Map<*, *>)?.get(other) as? String ?: "",
                enumValue(data.string("state"), MatchStatus.PENDING), userId in data.strings("blockedBy"), data.number("messageCount"),
                ((data["readAt"] as? Map<*, *>)?.get(other) as? Number)?.toLong() ?: 0)
        }.filter { it.status != MatchStatus.PENDING }
    }
    override fun guardians(userId: String) = query(db.collection("guardians").whereArrayContains("members", userId)).map { docs ->
        docs.map { (id, data) -> Guardian(id, data.string("ownerId"), data["guardianId"] as? String,
            enumValue(data.string("state"), GuardianStatus.INVITED)) }
    }
    override fun subscription(userId: String) = document("subscriptions/$userId").map {
        val until = it.orEmpty().number("verifiedUntil")
        Subscription(if (until > System.currentTimeMillis()) SubscriptionTier.PREMIUM else SubscriptionTier.FREE, until)
    }
    override fun notifications(userId: String) = query(db.collection("notifications/$userId/items")
        .orderBy("createdAt", Query.Direction.DESCENDING).limit(100)).map { docs ->
        docs.map { (id, data) -> MemberNotification(id, data.string("category"), data.string("targetId"),
            (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0, data["read"] == true) }
    }
    override fun messages(matchId: String) = query(db.collection("pairs/$matchId/messages")
        .orderBy("createdAt", Query.Direction.DESCENDING).limit(50)).map { docs ->
        docs.map { (id, data) -> messageFrom(id, data) }.reversed()
    }
    override suspend fun earlierMessages(matchId: String, beforeId: String): List<ChatMessage> = try {
        val collection = db.collection("pairs/$matchId/messages")
        val anchor = collection.document(beforeId).get(Source.SERVER).await()
        if (!anchor.exists()) throw CommunityException(CommunityError.INVALID)
        collection.orderBy("createdAt", Query.Direction.DESCENDING).startAfter(anchor).limit(50)
            .get(Source.SERVER).await().documents.map { messageFrom(it.id, it.data.orEmpty()) }.reversed()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { throw failure(error) }
    override suspend fun discover(cursor: String?): DiscoveryPage {
        val data = call(mapOf("action" to "discover", "cursor" to cursor))
        val profiles = (data["profiles"] as? List<*>).orEmpty().mapNotNull { raw ->
            @Suppress("UNCHECKED_CAST") val profile = raw as? Map<String, Any> ?: return@mapNotNull null
            profileFrom(profile.string("id"), profile)
        }
        return DiscoveryPage(profiles, data["cursor"] as? String)
    }
    override suspend fun execute(command: CommunityCommand): CommandResult {
        val payload: Map<String, Any?> = when (command) {
            is CommunityCommand.Publish -> mapOf("action" to "saveProfile", "profile" to command.profile.payload())
            is CommunityCommand.Preferences -> with(command.value) { mapOf("action" to "savePreferences", "preferences" to mapOf(
                "minAge" to minAge, "maxAge" to maxAge, "gender" to (gender?.name ?: "ANY"), "country" to country,
                "language" to language, "nonSmokerOnly" to nonSmokerOnly, "wantsChildren" to (wantsChildren?.name ?: "ANY"))) }
            is CommunityCommand.Privacy -> with(command.value) { mapOf("action" to "saveSettings", "settings" to mapOf(
                "visible" to visible, "readReceipts" to readReceipts, "showCity" to showCity, "notifications" to notifications,
                "analytics" to analytics, "marketing" to marketing)) }
            is CommunityCommand.Interest -> mapOf("action" to "interest", "userId" to command.userId, "decision" to if (command.like) "LIKE" else "PASS")
            is CommunityCommand.Send -> mapOf("action" to "sendMessage", "matchId" to command.matchId, "messageId" to command.messageId, "text" to command.text)
            is CommunityCommand.Read -> mapOf("action" to "markRead", "matchId" to command.matchId, "messageId" to command.messageId)
            is CommunityCommand.ReadNotification -> mapOf("action" to "readNotification", "id" to command.id, "createdAt" to command.createdAt)
            is CommunityCommand.Unmatch -> mapOf("action" to "unmatch", "matchId" to command.matchId)
            is CommunityCommand.Block -> mapOf("action" to "block", "userId" to command.userId)
            is CommunityCommand.Report -> mapOf("action" to "report", "userId" to command.userId, "category" to command.category.name, "details" to command.details)
            CommunityCommand.InviteGuardian -> mapOf("action" to "guardianInvite")
            is CommunityCommand.AcceptGuardian -> mapOf("action" to "guardianAccept", "code" to command.code)
            is CommunityCommand.RevokeGuardian -> mapOf("action" to "guardianRevoke", "id" to command.id)
            CommunityCommand.RevokeSessions -> mapOf("action" to "revokeSessions")
            CommunityCommand.RequestDeletion -> mapOf("action" to "requestDeletion")
            CommunityCommand.ExportData -> mapOf("action" to "exportData")
        }
        val result = call(payload)
        return CommandResult(result["matched"] == true, result["code"] as? String,
            if (command == CommunityCommand.ExportData) JSONObject(result).toString(2) else null)
    }
    private suspend fun call(payload: Map<String, Any?>): Map<String, Any> = try {
        @Suppress("UNCHECKED_CAST")
        (functions.getHttpsCallable("calaf").call(payload).await().data as? Map<String, Any>).orEmpty()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { throw failure(error) }

    private fun failure(error: Exception): CommunityException {
        if (localDiagnostics) android.util.Log.w("CalafBackend", "Local request failed: ${error.javaClass.simpleName}; code=${(error as? FirebaseFunctionsException)?.code}; cause=${error.cause?.javaClass?.simpleName}")
        val reason = when (error) {
            is FirebaseFunctionsException -> when (error.code) {
                FirebaseFunctionsException.Code.NOT_FOUND, FirebaseFunctionsException.Code.UNIMPLEMENTED -> CommunityError.CONFIGURATION
                FirebaseFunctionsException.Code.UNAVAILABLE, FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> CommunityError.OFFLINE
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> CommunityError.SIGN_IN_AGAIN
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> CommunityError.PERMISSION
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> CommunityError.LIMIT
                FirebaseFunctionsException.Code.INVALID_ARGUMENT, FirebaseFunctionsException.Code.FAILED_PRECONDITION -> CommunityError.INVALID
                else -> CommunityError.UNKNOWN
            }
            is FirebaseFirestoreException -> when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> CommunityError.PERMISSION
                FirebaseFirestoreException.Code.UNAVAILABLE -> CommunityError.OFFLINE
                else -> CommunityError.CONFIGURATION
            }
            else -> CommunityError.UNKNOWN
        }
        return CommunityException(reason)
    }
}

private fun messageFrom(id: String, data: Map<String, Any>) = ChatMessage(id, data.string("senderId"), data.string("text"),
    (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0,
    data.strings("warnings").mapNotNull { name -> enumValues<SafetyWarning>().firstOrNull { it.name == name } })

private fun Map<String, Any>.string(key: String) = this[key] as? String ?: ""
private fun Map<String, Any>.number(key: String, default: Long = 0) = (this[key] as? Number)?.toLong() ?: default
private fun Map<String, Any>.strings(key: String) = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
private inline fun <reified T : Enum<T>> enumValue(value: String, default: T): T = enumValues<T>().firstOrNull { it.name == value } ?: default
private fun area(value: String): CompatibilityArea? = when (value) {
    "timeline" -> CompatibilityArea.TIMELINE; "wantsChildren" -> CompatibilityArea.WANTS_CHILDREN
    "smoking" -> CompatibilityArea.SMOKING; "relocation" -> CompatibilityArea.RELOCATION
    "religion" -> CompatibilityArea.RELIGION; "languages" -> CompatibilityArea.LANGUAGES
    "values" -> CompatibilityArea.VALUES; "interests" -> CompatibilityArea.INTERESTS; else -> null
}
private fun profileFrom(id: String, d: Map<String, Any>): CommunityProfile {
    val score = d["compatibility"] as? Map<*, *>
    return CommunityProfile(id, d.string("name"), d.number("age").toInt(), d.string("city"), d.string("country"),
        d.string("biography"), d.string("culture"), d.strings("languages"), d.strings("interests"), d.strings("values"),
        enumValue(d.string("gender"), Gender.UNSPECIFIED), d.string("religion"), d.string("occupation"), d.string("education"),
        enumValue(d.string("children"), Answer.UNSPECIFIED), enumValue(d.string("wantsChildren"), Answer.UNSPECIFIED),
        enumValue(d.string("smoking"), Answer.UNSPECIFIED), enumValue(d.string("relocation"), Answer.UNSPECIFIED),
        enumValue(d.string("timeline"), MarriageTimeline.WHEN_READY), enumValue(d.string("verification"), VerificationStatus.NOT_STARTED),
        CompatibilityResult((score?.get("score") as? Number)?.toInt(),
            (score?.get("strong") as? List<*>)?.filterIsInstance<String>()?.mapNotNull(::area).orEmpty(),
            (score?.get("differences") as? List<*>)?.filterIsInstance<String>()?.mapNotNull(::area).orEmpty()))
}
private fun ProfileDraft.payload(): Map<String, Any> = mapOf(
    "name" to name, "birthDate" to birthDate, "city" to city, "country" to country, "biography" to biography,
    "culture" to culture, "languages" to languages.split(',').map(String::trim).filter(String::isNotEmpty),
    "interests" to interests.split(',').map(String::trim).filter(String::isNotEmpty),
    "values" to values.split(',').map(String::trim).filter(String::isNotEmpty), "gender" to gender.name,
    "religion" to religion, "occupation" to occupation, "education" to education, "children" to children.name,
    "wantsChildren" to wantsChildren.name, "smoking" to smoking.name, "relocation" to relocation.name, "timeline" to timeline.name)
