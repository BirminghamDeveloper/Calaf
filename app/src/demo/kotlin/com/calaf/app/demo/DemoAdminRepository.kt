package com.calaf.app.demo

import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.UUID

data class AdminMember(val id: String, val email: String, val suspended: Boolean, val isAdmin: Boolean)
data class AdminReport(val id: String, val target: String, val details: String, val status: String)

/** Demo moderation. Every call needs a signed in member whose stored user has the admin role flag. */
class DemoAdminRepository(private val store: DemoStore, private val session: DemoSession) {
    /** Changes whenever demo data changes, so screens can refresh. */
    val revision: StateFlow<Int> get() = store.revision

    val isAdmin: Boolean get() = session.isAdmin

    fun members(): List<AdminMember> {
        session.requireAdmin()
        return store.rows("users").map { (id, user) ->
            AdminMember(id, user.getString("email"), user.optBoolean("suspended"), user.optBoolean("admin"))
        }
    }

    fun reports(): List<AdminReport> {
        session.requireAdmin()
        return store.rows("reports").map { (id, report) ->
            val details = "${report.optString("category")}: ${report.optString("details")}"
            AdminReport(id, report.getString("target"), details, report.getString("status"))
        }
    }

    fun auditCount(): Int {
        session.requireAdmin()
        return store.table("audit").length()
    }

    /** Administrators cannot be suspended, so nobody can lock the demo out of its moderation tools. */
    fun suspend(id: String, suspended: Boolean) {
        val actor = session.requireAdmin()
        val target = store.user(id)
        if (target == null || target.optBoolean("admin")) throw CommunityException(CommunityError.INVALID)
        target.put("suspended", suspended)
        audit(actor, if (suspended) "SUSPEND" else "RESTORE", id)
        store.save()
    }

    fun resolveReport(id: String, dismiss: Boolean) {
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
