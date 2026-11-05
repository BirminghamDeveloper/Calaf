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
        seedPhotos(store)
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

    /** Gives each seeded member two or three bundled avatars, if they have no photos yet. Safe to repeat. */
    fun seedPhotos(store: DemoStore) {
        names.forEachIndexed { index, name ->
            val id = "demo-$name"
            if (store.user(id)?.optBoolean("seed") != true) return@forEachIndexed
            if (DemoPhotoRows.ownerPhotos(store, id).isNotEmpty()) return@forEachIndexed
            val keys = buildList {
                add(DemoAvatars.key(index * 2))
                add(DemoAvatars.key(index * 2 + 1))
                if (index % 2 == 0) add(DemoAvatars.key(index * 2 + 5))
            }
            val rows = keys.mapIndexed { n, key -> "$id-photo-$n" to DemoPhotoRows.newRow(id, key) }.toMap()
            val photos = rows.keys.mapIndexed { n, photoId ->
                com.calaf.core.model.Photo(photoId, n, n == 0, com.calaf.core.model.PhotoState.APPROVED)
            }
            DemoPhotoRows.write(store, photos, rows)
        }
    }

    private fun profile(name: String, index: Int) = JSONObject()
        .put("name", name.replaceFirstChar { it.uppercase() } + " (Demo)")
        .put("birthDate", "199${index + 1}-04-12").put("city", "London").put("country", "United Kingdom")
        .put("culture", "Somali / East African").put("languages", "Somali, English")
        .put("biography", "Fictional offline demonstration profile. Interested in family and a serious commitment.")
        .put("gender", if (name in women) "WOMAN" else "MAN")
        .put("smoking", "NO").put("wantsChildren", "YES")
        // Fictional members consent to sharing faith and heritage, so the demo can show those fields.
        .put("sensitiveConsent", true).put("religion", "Islam").put("religiosity", "PRACTISING")
        .put("prays", "USUALLY").put("heritage", if (index % 2 == 0) "SOMALI" else "SOMALILAND")
        .put("maritalStatus", "NEVER_MARRIED").put("heightCm", 165 + index * 4).put("countryOfBirth", "Somalia")
        .put("occupation", "Teacher").put("education", "University degree")

    const val SAMPLE_REPORT_ID = "sample-report"
}
