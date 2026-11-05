package com.calaf.app.demo

import com.calaf.core.model.Answer
import com.calaf.core.model.Gender
import com.calaf.core.model.Heritage
import com.calaf.core.model.MaritalStatus
import com.calaf.core.model.MarriageTimeline
import com.calaf.core.model.PrayerHabit
import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.Religiosity
import com.calaf.core.model.withoutUnconsentedData
import org.json.JSONObject

/** Converts a [ProfileDraft] to and from the JSON rows the demo store keeps. */
internal object DemoProfileCodec {
    fun toDraft(json: JSONObject) = ProfileDraft(
        name = json.optString("name"),
        birthDate = json.optString("birthDate"),
        city = json.optString("city"),
        country = json.optString("country"),
        languages = json.optString("languages"),
        culture = json.optString("culture"),
        biography = json.optString("biography"),
        gender = enumValue(json.optString("gender"), Gender.UNSPECIFIED),
        religion = json.optString("religion"),
        occupation = json.optString("occupation"),
        education = json.optString("education"),
        interests = json.optString("interests"),
        values = json.optString("values"),
        children = enumValue(json.optString("children"), Answer.UNSPECIFIED),
        wantsChildren = enumValue(json.optString("wantsChildren"), Answer.UNSPECIFIED),
        smoking = enumValue(json.optString("smoking"), Answer.UNSPECIFIED),
        relocation = enumValue(json.optString("relocation"), Answer.UNSPECIFIED),
        timeline = enumValue(json.optString("timeline"), MarriageTimeline.WHEN_READY),
        // Rows written before Task 2.1 have none of these keys, so every one falls back to "not shared".
        maritalStatus = enumValue(json.optString("maritalStatus"), MaritalStatus.UNSPECIFIED),
        heightCm = json.optInt("heightCm"),
        countryOfBirth = json.optString("countryOfBirth"),
        religiosity = enumValue(json.optString("religiosity"), Religiosity.UNSPECIFIED),
        prays = enumValue(json.optString("prays"), PrayerHabit.UNSPECIFIED),
        heritage = enumValue(json.optString("heritage"), Heritage.UNSPECIFIED),
        sensitiveConsent = json.optBoolean("sensitiveConsent"),
    ).withoutUnconsentedData()

    fun toJson(profile: ProfileDraft): JSONObject = JSONObject()
        .put("name", profile.name).put("birthDate", profile.birthDate).put("city", profile.city)
        .put("country", profile.country).put("languages", profile.languages).put("culture", profile.culture)
        .put("biography", profile.biography).put("gender", profile.gender.name).put("religion", profile.religion)
        .put("occupation", profile.occupation).put("education", profile.education)
        .put("interests", profile.interests).put("values", profile.values)
        .put("children", profile.children.name).put("wantsChildren", profile.wantsChildren.name)
        .put("smoking", profile.smoking.name).put("relocation", profile.relocation.name)
        .put("timeline", profile.timeline.name).put("maritalStatus", profile.maritalStatus.name)
        .put("heightCm", profile.heightCm).put("countryOfBirth", profile.countryOfBirth)
        .put("religiosity", profile.religiosity.name).put("prays", profile.prays.name)
        .put("heritage", profile.heritage.name).put("sensitiveConsent", profile.sensitiveConsent)

    inline fun <reified T : Enum<T>> enumValue(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
