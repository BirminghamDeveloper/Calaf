package com.calaf.app.demo

import com.calaf.core.model.Account
import com.calaf.core.model.CommunityError
import com.calaf.core.model.CommunityException
import kotlinx.coroutines.flow.MutableStateFlow

/** Who is signed in. Shared by every demo repository so they agree on the current member. */
class DemoSession(private val store: DemoStore) {
    val accounts = MutableStateFlow(account(store.sessionId))

    /** A suspended or unknown member has no account, so cannot sign in or stay signed in. */
    fun account(id: String?): Account? = id?.let { uid ->
        store.user(uid)?.takeIf { !it.optBoolean("suspended") }?.let { Account(uid, it.getString("email"), true) }
    }

    fun signIn(id: String) {
        accounts.value = account(id)
        store.sessionId = id
    }

    fun signOut() {
        accounts.value = null
        store.sessionId = null
    }

    fun refresh(): Account? {
        accounts.value = account(accounts.value?.id)
        return accounts.value
    }

    /** The signed in member's id, or [CommunityError.SIGN_IN_AGAIN] if nobody valid is signed in. */
    fun requireUid(): String = accounts.value?.id?.takeIf { !store.isSuspended(it) }
        ?: throw CommunityException(CommunityError.SIGN_IN_AGAIN)

    fun requireOwner(id: String) {
        if (requireUid() != id) throw CommunityException(CommunityError.PERMISSION)
    }

    val isAdmin: Boolean
        get() = accounts.value?.id?.let { store.user(it)?.optBoolean("admin") } == true

    /** The signed in administrator's id, or [CommunityError.PERMISSION]. */
    fun requireAdmin(): String {
        if (!isAdmin) throw CommunityException(CommunityError.PERMISSION)
        return requireUid()
    }
}
