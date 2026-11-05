package com.calaf.app.demo

import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * The one place the demo keeps its data: a single JSON file inside [directory], plus a `photos` folder.
 * It has no Android dependency, so JVM tests can point it at a temp directory.
 *
 * The file carries a schema version. When [SCHEMA_VERSION] goes up, add a step to [migrate].
 */
class DemoStore(
    private val directory: File,
    private val seed: (DemoStore) -> Unit = DemoSeed::seedIfNeeded,
) {
    private val file = File(directory, "demo.json")
    private val root: JSONObject = load()

    /** Bumped on every [save]. Repositories re-read their data when it changes. */
    val revision = MutableStateFlow(0)
    val photoRoot = File(directory, "photos")

    init {
        val storedVersion = root.optInt(KEY_VERSION, 0)
        migrate(storedVersion)
        root.put(KEY_VERSION, SCHEMA_VERSION)
        seed(this)
        // Write a migrated store back straight away so the migration only ever runs once.
        if (storedVersion in 1 until SCHEMA_VERSION) save()
    }

    /** The id of the signed in member, kept so the demo reopens signed in. */
    var sessionId: String?
        get() = root.optString(KEY_SESSION).ifEmpty { null }
        set(value) {
            if (value == null) root.remove(KEY_SESSION) else root.put(KEY_SESSION, value)
            save()
        }

    /**
     * Puts the demo back as it was on first launch: every account, photo, message and setting is deleted and the seed is
     * applied again. The caller signs the member out.
     */
    fun reset() {
        root.keys().asSequence().toList().forEach(root::remove)
        photoRoot.deleteRecursively()
        root.put(KEY_VERSION, SCHEMA_VERSION)
        seed(this)
        save()
    }

    fun table(name: String): JSONObject = root.optJSONObject(name) ?: JSONObject().also { root.put(name, it) }

    fun rows(name: String): List<Pair<String, JSONObject>> =
        table(name).let { table -> table.keys().asSequence().map { it to table.getJSONObject(it) }.toList() }

    fun user(id: String): JSONObject? = table("users").optJSONObject(id)

    fun isSuspended(id: String): Boolean = user(id)?.optBoolean("suspended") != false

    fun save() {
        directory.mkdirs()
        val temporary = File(directory, "demo.json.tmp")
        temporary.writeText(root.toString())
        if (!temporary.renameTo(file)) {
            file.delete()
            check(temporary.renameTo(file)) { "Could not write the demo store" }
        }
        revision.value++
    }

    fun pairId(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")

    /** Creates the pair and one MATCH notification for each member. Does nothing if the pair exists. */
    fun createMatch(a: String, b: String): String {
        val id = pairId(a, b)
        if (!table("pairs").has(id)) {
            table("pairs").put(id, JSONObject().put("a", a).put("b", b).put("status", "ACTIVE"))
            listOf(a, b).forEach { member -> addNotification(member, "MATCH", id) }
        }
        return id
    }

    fun addNotification(owner: String, category: String, target: String) {
        table("notifications").put(
            UUID.randomUUID().toString(),
            JSONObject().put("owner", owner).put("category", category).put("target", target)
                .put("time", System.currentTimeMillis()),
        )
    }

    /** True if either member has blocked the other. A block never needs a pair. */
    fun isBlockedEitherWay(a: String, b: String): Boolean =
        table("blocks").has("${a}_$b") || table("blocks").has("${b}_$a")

    private fun load(): JSONObject {
        if (!file.exists()) return JSONObject()
        // An unreadable file is only fictional demo data, so start again rather than crash.
        return try { JSONObject(file.readText()) } catch (_: JSONException) { JSONObject() }
    }

    private fun migrate(from: Int) {
        if (from < 2) migrateToVersion2()
        if (from < 3) migrateToVersion3()
        if (from < 4) DemoSeed.seedLikes(this)
        // Future steps: if (from < 3) { ...rewrite rows... }
    }

    /**
     * Version 3 adds the photo model. Photos used to be plain files `photos/<owner>/photo-<slot>.jpg` with no record in
     * the store, so each becomes an approved photo row ordered by its old slot. Seeded members then get avatars.
     * Safe on an empty store.
     */
    private fun migrateToVersion3() {
        val owners = photoRoot.listFiles { file -> file.isDirectory }.orEmpty()
        for (directory in owners) {
            val legacy = Regex("photo-(\\d+)\\.jpg")
            val slots = directory.listFiles().orEmpty().mapNotNull { file ->
                val slot = legacy.matchEntire(file.name)?.groupValues?.get(1)?.toInt()
                slot?.let { it to file.name.removeSuffix(".jpg") }
            }.sortedBy { it.first }
            if (slots.isEmpty()) continue
            val rows = slots.associate { (_, id) -> id to DemoPhotoRows.newRow(directory.name) }
            val photos = slots.mapIndexed { index, (_, id) ->
                com.calaf.core.model.Photo(id, index, index == 0, com.calaf.core.model.PhotoState.APPROVED)
            }
            DemoPhotoRows.write(this, photos, rows)
        }
        DemoSeed.seedPhotos(this)
    }

    /**
     * Version 2 adds explicit consent for religion and heritage (UK GDPR special category data). Rows from before
     * have no consent record, so a fictional seeded member is treated as consenting and anyone else loses their
     * religion text. Safe on an empty store.
     */
    private fun migrateToVersion2() {
        for (tableName in listOf("profiles", "drafts")) {
            for ((id, row) in rows(tableName)) {
                if (row.has("sensitiveConsent")) continue
                val seeded = user(id)?.optBoolean("seed") == true
                row.put("sensitiveConsent", seeded)
                if (!seeded) row.put("religion", "")
            }
        }
    }

    companion object {
        const val SCHEMA_VERSION = 4
        private const val KEY_VERSION = "schemaVersion"
        private const val KEY_SESSION = "session"
    }
}
