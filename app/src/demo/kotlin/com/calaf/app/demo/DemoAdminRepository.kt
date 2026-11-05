package com.calaf.app.demo

import com.calaf.core.model.AdminMember
import com.calaf.core.model.AdminPhoto
import com.calaf.core.model.AdminReport
import com.calaf.core.model.AdminRepository
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import com.calaf.core.model.PhotoState
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.UUID

/** Demo moderation. Every call needs a signed in member whose stored user has the admin role flag. */
class DemoAdminRepository(
    private val store: DemoStore,
    private val session: DemoSession,
    private val avatars: AvatarSource,
) : AdminRepository {
    /** Changes whenever demo data changes, so screens can refresh. */
    override val revision: StateFlow<Int> get() = store.revision

    override val isAdmin: Boolean get() = session.isAdmin

    override fun members(): List<AdminMember> {
        session.requireAdmin()
        return store.rows("users").map { (id, user) ->
            AdminMember(id, user.getString("email"), user.optBoolean("suspended"), user.optBoolean("admin"))
        }
    }

    override fun reports(): List<AdminReport> {
        session.requireAdmin()
        return store.rows("reports").map { (id, report) ->
            val details = "${report.optString("category")}: ${report.optString("details")}"
            AdminReport(id, report.getString("target"), details, report.getString("status"))
        }
    }

    override fun photos(): List<AdminPhoto> {
        session.requireAdmin()
        return store.rows("photos").map { (id, row) ->
            val owner = row.getString("owner")
            AdminPhoto(id, owner, store.user(owner)?.optString("email").orEmpty(), DemoPhotoRows.photo(id, row).state)
        }.sortedWith(compareBy({ it.ownerEmail }, { it.id }))
    }

    override fun photoBytes(photoId: String): ByteArray {
        session.requireAdmin()
        return DemoPhotoRows.bytes(store, avatars, photoId)
    }

    override fun reviewPhoto(photoId: String, approve: Boolean) {
        val actor = session.requireAdmin()
        val row = DemoPhotoRows.row(store, photoId) ?: throw CommunityException(CommunityError.INVALID)
        row.put("state", (if (approve) PhotoState.APPROVED else PhotoState.REJECTED).name)
        audit(actor, if (approve) "APPROVE_PHOTO" else "REJECT_PHOTO", photoId)
        store.save()
    }

    override fun auditCount(): Int {
        session.requireAdmin()
        return store.table("audit").length()
    }

    /** Administrators cannot be suspended, so nobody can lock the demo out of its moderation tools. */
    override fun suspend(id: String, suspended: Boolean) {
        val actor = session.requireAdmin()
        val target = store.user(id)
        if (target == null || target.optBoolean("admin")) throw CommunityException(CommunityError.INVALID)
        target.put("suspended", suspended)
        audit(actor, if (suspended) "SUSPEND" else "RESTORE", id)
        store.save()
    }

    override fun resolveReport(id: String, dismiss: Boolean) {
        val actor = session.requireAdmin()
        val report = store.table("reports").optJSONObject(id) ?: throw CommunityException(CommunityError.INVALID)
        if (report.optString("status") != "OPEN") throw CommunityException(CommunityError.INVALID)
        report.put("status", if (dismiss) "DISMISSED" else "RESOLVED")
        audit(actor, if (dismiss) "DISMISS_REPORT" else "RESOLVE_REPORT", id)
        store.save()
    }

    private fun audit(actor: String, action: String, target: String) {
        store.table("audit").put(
            UUID.randomUUID().toString(),
            JSONObject().put("actor", actor).put("action", action).put("target", target)
                .put("time", System.currentTimeMillis()),
        )
    }
}
