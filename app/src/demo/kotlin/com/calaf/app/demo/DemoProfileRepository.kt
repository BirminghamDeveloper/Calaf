package com.calaf.app.demo

import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.ProfileRepository
import com.calaf.core.model.withoutUnconsentedData

/** Private drafts. A member who has published but never saved a draft gets their published profile back. */
class DemoProfileRepository(private val store: DemoStore, private val session: DemoSession) : ProfileRepository {
    override suspend fun load(userId: String): ProfileDraft {
        session.requireOwner(userId)
        val row = store.table("drafts").optJSONObject(userId) ?: store.table("profiles").optJSONObject(userId)
        return row?.let(DemoProfileCodec::toDraft) ?: ProfileDraft()
    }

    override suspend fun save(userId: String, profile: ProfileDraft) {
        session.requireOwner(userId)
        // Faith and heritage are special category data: never keep them without consent.
        store.table("drafts").put(userId, DemoProfileCodec.toJson(profile.withoutUnconsentedData()))
        store.save()
    }
}
