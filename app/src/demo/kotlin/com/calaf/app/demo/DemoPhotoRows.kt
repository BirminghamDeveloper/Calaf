package com.calaf.app.demo

import com.calaf.core.model.Photo
import com.calaf.core.model.PhotoRules
import com.calaf.core.model.PhotoState
import org.json.JSONObject
import java.io.File

/**
 * How photos sit in the demo store: one row per photo in the `photos` table. A row holds the owner, order, state,
 * blur choice and either an `avatar` key (bundled artwork) or nothing (an uploaded file named `<id>.jpg`).
 */
internal object DemoPhotoRows {
    private const val TABLE = "photos"

    fun ownerPhotos(store: DemoStore, owner: String): List<Photo> =
        PhotoRules.normalise(
            store.rows(TABLE).filter { it.second.optString("owner") == owner }.map { (id, row) -> photo(id, row) },
        )

    fun photo(id: String, row: JSONObject) = Photo(
        id = id,
        order = row.optInt("order"),
        isPrimary = row.optBoolean("primary"),
        state = runCatching { PhotoState.valueOf(row.optString("state")) }.getOrDefault(PhotoState.PENDING),
        blurForNonMatches = row.optBoolean("blur"),
    )

    fun row(store: DemoStore, id: String): JSONObject? = store.table(TABLE).optJSONObject(id)

    /** Writes order, primary flag and blur for the owner's photos, leaving their source and state alone. */
    fun write(store: DemoStore, photos: List<Photo>, newRows: Map<String, JSONObject> = emptyMap()) {
        val table = store.table(TABLE)
        for ((id, row) in newRows) table.put(id, row)
        for (photo in photos) {
            val row = table.getJSONObject(photo.id)
            row.put("order", photo.order).put("primary", photo.isPrimary).put("blur", photo.blurForNonMatches)
            row.put("state", photo.state.name)
        }
    }

    fun remove(store: DemoStore, id: String) {
        store.table(TABLE).remove(id)
    }

    fun file(store: DemoStore, owner: String, id: String) = File(File(store.photoRoot, owner), "$id.jpg")

    fun bytes(store: DemoStore, avatars: AvatarSource, id: String): ByteArray {
        val row = row(store, id) ?: throw NoSuchElementException(id)
        val avatar = row.optString("avatar")
        return if (avatar.isNotEmpty()) avatars.bytes(avatar) else file(store, row.getString("owner"), id).readBytes()
    }

    fun newRow(owner: String, avatar: String? = null) = JSONObject().put("owner", owner).apply {
        if (avatar != null) put("avatar", avatar)
    }
}
