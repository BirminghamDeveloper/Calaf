package com.calaf.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.ProfileRepository
import com.calaf.core.model.Gender
import com.calaf.core.model.Answer
import com.calaf.core.model.Heritage
import com.calaf.core.model.MaritalStatus
import com.calaf.core.model.MarriageTimeline
import com.calaf.core.model.PrayerHabit
import com.calaf.core.model.Religiosity
import com.calaf.core.model.withoutUnconsentedData
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/** Private, account-scoped drafts. These are never public discovery profiles. */
class LocalProfileRepository(private val store: DataStore<Preferences>) : ProfileRepository {
    override suspend fun load(userId: String): ProfileDraft {
        val raw = store.data.first()[stringPreferencesKey("profile_$userId")] ?: return ProfileDraft()
        val value = JSONObject(raw)
        return ProfileDraft(value.optString("name"), value.optString("birthDate"),
            value.optString("city"), value.optString("country"), value.optString("languages"),
            value.optString("culture"), value.optString("biography"),
            enumValues<Gender>().firstOrNull { it.name == value.optString("gender") } ?: Gender.UNSPECIFIED,
            value.optString("religion"), value.optString("occupation"), value.optString("education"),
            value.optString("interests"), value.optString("values"),
            value.answer("children"), value.answer("wantsChildren"), value.answer("smoking"), value.answer("relocation"),
            enumValues<MarriageTimeline>().firstOrNull { it.name == value.optString("timeline") } ?: MarriageTimeline.WHEN_READY,
            value.choice("maritalStatus", MaritalStatus.UNSPECIFIED), value.optInt("heightCm"), value.optString("countryOfBirth"),
            value.choice("religiosity", Religiosity.UNSPECIFIED), value.choice("prays", PrayerHabit.UNSPECIFIED),
            value.choice("heritage", Heritage.UNSPECIFIED), value.optBoolean("sensitiveConsent"),
        ).withoutUnconsentedData()
    }
    override suspend fun save(userId: String, profile: ProfileDraft) {
        val value = JSONObject().put("maritalStatus", profile.maritalStatus.name).put("heightCm", profile.heightCm)
            .put("countryOfBirth", profile.countryOfBirth).put("religiosity", profile.religiosity.name)
            .put("prays", profile.prays.name).put("heritage", profile.heritage.name)
            .put("sensitiveConsent", profile.sensitiveConsent).put("name", profile.name).put("birthDate", profile.birthDate)
            .put("city", profile.city).put("country", profile.country).put("languages", profile.languages)
            .put("culture", profile.culture).put("biography", profile.biography)
            .put("gender", profile.gender.name).put("religion", profile.religion).put("occupation", profile.occupation)
            .put("education", profile.education).put("interests", profile.interests).put("values", profile.values)
            .put("children", profile.children.name).put("wantsChildren", profile.wantsChildren.name)
            .put("smoking", profile.smoking.name).put("relocation", profile.relocation.name).put("timeline", profile.timeline.name).toString()
        store.edit { it[stringPreferencesKey("profile_$userId")] = value }
    }
}
private inline fun <reified T : Enum<T>> JSONObject.choice(key: String, default: T) =
    enumValues<T>().firstOrNull { it.name == optString(key) } ?: default
private fun JSONObject.answer(key: String) = enumValues<Answer>().firstOrNull { it.name == optString(key) } ?: Answer.UNSPECIFIED
