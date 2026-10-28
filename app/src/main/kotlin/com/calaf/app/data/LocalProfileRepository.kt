package com.calaf.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.ProfileRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/** Private, account-scoped drafts. These are never public discovery profiles. */
class LocalProfileRepository(private val store: DataStore<Preferences>) : ProfileRepository {
    override suspend fun load(userId: String): ProfileDraft {
        val raw = store.data.first()[stringPreferencesKey("profile_$userId")] ?: return ProfileDraft()
        val value = JSONObject(raw)
        return ProfileDraft(value.optString("name"), value.optString("birthDate"),
            value.optString("city"), value.optString("country"), value.optString("languages"),
            value.optString("culture"), value.optString("biography"))
    }
    override suspend fun save(userId: String, profile: ProfileDraft) {
        val value = JSONObject().put("name", profile.name).put("birthDate", profile.birthDate)
            .put("city", profile.city).put("country", profile.country).put("languages", profile.languages)
            .put("culture", profile.culture).put("biography", profile.biography).toString()
        store.edit { it[stringPreferencesKey("profile_$userId")] = value }
    }
}
