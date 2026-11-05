package com.calaf.app.demo

import com.calaf.core.model.Answer
import com.calaf.core.model.Gender
import com.calaf.core.model.Heritage
import com.calaf.core.model.MaritalStatus
import com.calaf.core.model.MarriageTimeline
import com.calaf.core.model.PartnerPreference
import com.calaf.core.model.PrayerHabit
import com.calaf.core.model.Religiosity
import org.json.JSONArray
import org.json.JSONObject

/** Converts [PartnerPreference] to and from the JSON rows of the demo store. Rows without the newer keys mean "any". */
internal object DemoPreferenceCodec {
    fun toJson(value: PartnerPreference): JSONObject = JSONObject()
        .put("min", value.minAge).put("max", value.maxAge).put("gender", value.gender?.name)
        .put("country", value.country).put("language", value.language).put("nonSmoker", value.nonSmokerOnly)
        .put("children", value.wantsChildren?.name)
        .put("maritalStatuses", names(value.maritalStatuses)).put("hasChildren", value.hasChildren?.name)
        .put("minHeight", value.minHeightCm).put("maxHeight", value.maxHeightCm)
        .put("religiosity", names(value.religiosity)).put("prays", names(value.prays))
        .put("heritage", names(value.heritage)).put("birthCountry", value.countryOfBirth)
        .put("profession", value.profession).put("education", value.education)
        .put("timelines", names(value.timelines)).put("relocate", value.willingToRelocate?.name)

    fun fromJson(p: JSONObject): PartnerPreference = PartnerPreference(
        minAge = p.optInt("min", 18),
        maxAge = p.optInt("max", 70),
        gender = Gender.entries.firstOrNull { it.name == p.optString("gender") },
        country = p.optString("country"),
        language = p.optString("language"),
        nonSmokerOnly = p.optBoolean("nonSmoker"),
        wantsChildren = Answer.entries.firstOrNull { it.name == p.optString("children") },
        maritalStatuses = set<MaritalStatus>(p.optJSONArray("maritalStatuses")),
        hasChildren = Answer.entries.firstOrNull { it.name == p.optString("hasChildren") },
        minHeightCm = p.optInt("minHeight"),
        maxHeightCm = p.optInt("maxHeight"),
        religiosity = set<Religiosity>(p.optJSONArray("religiosity")),
        prays = set<PrayerHabit>(p.optJSONArray("prays")),
        heritage = set<Heritage>(p.optJSONArray("heritage")),
        countryOfBirth = p.optString("birthCountry"),
        profession = p.optString("profession"),
        education = p.optString("education"),
        timelines = set<MarriageTimeline>(p.optJSONArray("timelines")),
        willingToRelocate = Answer.entries.firstOrNull { it.name == p.optString("relocate") },
    )

    private fun names(values: Set<Enum<*>>) = JSONArray(values.map { it.name })

    private inline fun <reified T : Enum<T>> set(array: JSONArray?): Set<T> =
        (0 until (array?.length() ?: 0)).mapNotNull { index ->
            enumValues<T>().firstOrNull { it.name == array?.optString(index) }
        }.toSet()
}
