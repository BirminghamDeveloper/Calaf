package com.calaf.app.demo

import org.json.JSONObject

/**
 * Fictional demo members. Every credential here is PUBLIC and printed on the demo login screen.
 * They protect nothing and must never be reused for anything real.
 */
object DemoSeed {
    const val ADMIN_ID = "demo-admin"
    const val ADMIN_EMAIL = "admin@calaf.test"
    const val ADMIN_PASSWORD = "CalafAdmin!2026"

    /** Public password shared by every seeded member. */
    const val MEMBER_PASSWORD = "CalafDemo!2026"

    fun memberEmail(name: String) = "$name@calaf.test"

    private val names = listOf("admin", "hassan", "amina", "hodan", "yusuf")
    private val women = listOf("amina", "hodan")

    fun seedIfNeeded(store: DemoStore) {
        if (store.table("meta").optBoolean("seeded")) return
        names.forEachIndexed { index, name ->
            val id = "demo-$name"
            store.table("users").put(
                id,
                JSONObject().put("email", memberEmail(name)).put("seed", true).put("suspended", false)
                    .put("admin", name == "admin"),
            )
            store.table("profiles").put(id, profile(name, index))
        }
        store.createMatch(ADMIN_ID, "demo-amina")
        store.createMatch("demo-hassan", "demo-amina")
        store.table("reports").put(
            SAMPLE_REPORT_ID,
            JSONObject().put("reporter", "demo-amina").put("target", "demo-yusuf").put("category", "OTHER")
                .put("details", "Sample report for trying moderation. No real person is involved.")
                .put("status", "OPEN"),
        )
        store.table("meta").put("seeded", true)
        store.save()
    }

    private fun profile(name: String, index: Int) = JSONObject()
        .put("name", name.replaceFirstChar { it.uppercase() } + " (Demo)")
        .put("birthDate", "199${index + 1}-04-12").put("city", "London").put("country", "United Kingdom")
        .put("culture", "Somali / East African").put("languages", "Somali, English")
        .put("biography", "Fictional offline demonstration profile. Interested in family and a serious commitment.")
        .put("gender", if (name in women) "WOMAN" else "MAN")
        .put("religion", "Islam").put("smoking", "NO").put("wantsChildren", "YES")

    const val SAMPLE_REPORT_ID = "sample-report"
}
