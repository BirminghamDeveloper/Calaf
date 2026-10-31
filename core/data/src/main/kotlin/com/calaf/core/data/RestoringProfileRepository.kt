package com.calaf.core.data

import com.calaf.core.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/** Keeps unsent edits local; restores a published profile on a new device. */
class RestoringProfileRepository(private val local: ProfileRepository, app: FirebaseApp) : ProfileRepository {
    private val db = FirebaseFirestore.getInstance(app)
    override suspend fun load(userId: String): ProfileDraft {
        val draft = local.load(userId)
        if (draft.name.isNotBlank() || draft.birthDate.isNotBlank()) return draft
        return try {
            val profile = db.document("profiles/$userId").get().await()
            val user = db.document("users/$userId").get().await()
            if (!profile.exists() || !user.exists()) return draft
            fun value(key: String) = profile.getString(key).orEmpty()
            fun tags(key: String) = (profile.get(key) as? List<*>)?.filterIsInstance<String>()?.joinToString().orEmpty()
            val restored = ProfileDraft(value("name"), user.getString("birthDate").orEmpty(), value("city"), value("country"),
                tags("languages"), value("culture"), value("biography"),
                enumValues<Gender>().firstOrNull { it.name == value("gender") } ?: Gender.UNSPECIFIED,
                value("religion"), value("occupation"), value("education"), tags("interests"), tags("values"),
                enumValues<Answer>().firstOrNull { it.name == value("children") } ?: Answer.UNSPECIFIED,
                enumValues<Answer>().firstOrNull { it.name == value("wantsChildren") } ?: Answer.UNSPECIFIED,
                enumValues<Answer>().firstOrNull { it.name == value("smoking") } ?: Answer.UNSPECIFIED,
                enumValues<Answer>().firstOrNull { it.name == value("relocation") } ?: Answer.UNSPECIFIED,
                enumValues<MarriageTimeline>().firstOrNull { it.name == value("timeline") } ?: MarriageTimeline.WHEN_READY)
            local.save(userId, restored)
            restored
        } catch (_: FirebaseFirestoreException) { draft }
    }
    override suspend fun save(userId: String, profile: ProfileDraft) = local.save(userId, profile)
}
