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
    seed: (DemoStore) -> Unit = DemoSeed::seedIfNeeded,
) {
    private val file = File(directory, "demo.json")
    private val root: JSONObject = load()

    /** Bumped on every [save]. Repositories re-read their data when it changes. */
    val revision = MutableStateFlow(0)
    val photoRoot = File(directory, "photos")

    init {
        migrate(root.optInt(KEY_VERSION, 0))
        root.put(KEY_VERSION, SCHEMA_VERSION)
        seed(this)
    }

    /** The id of the signed in member, kept so the demo reopens signed in. */
    var sessionId: String?
        get() = root.optString(KEY_SESSION).ifEmpty { null }
        set(value) {
            if (value == null) root.remove(KEY_SESSION) else root.put(KEY_SESSION, value)
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

    @Suppress("UNUSED_PARAMETER")
    private fun migrate(from: Int) {
        // Version 1 is the first versioned layout, so nothing to migrate yet. Future steps go here:
        // if (from < 2) { ...rewrite rows... }
    }

    companion object {
        const val SCHEMA_VERSION = 1
        private const val KEY_VERSION = "schemaVersion"
        private const val KEY_SESSION = "session"
    }
}
