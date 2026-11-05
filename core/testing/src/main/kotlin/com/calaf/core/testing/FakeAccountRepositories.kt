package com.calaf.core.testing

import com.calaf.core.model.Account
import com.calaf.core.model.AccountError
import com.calaf.core.model.AccountException
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.ProfileDraft
import com.calaf.core.model.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** Signs in any email as an unverified account called "user". Refresh marks it verified. */
class FakeAuthRepository : AuthRepository {
    override val accounts = MutableStateFlow<Account?>(null)
    var calls = 0
    var lastPassword = ""
    var fail = false

    override suspend fun register(email: String, password: String) = login(email, password)

    override suspend fun login(email: String, password: String) {
        calls++
        lastPassword = password
        if (fail) throw AccountException(AccountError.Network)
        accounts.value = Account("user", email, false)
    }

    override suspend fun resetPassword(email: String) = Unit
    override suspend fun sendVerification() = Unit
    override suspend fun refresh() = accounts.value?.copy(emailVerified = true)
    override fun logout() {
        accounts.value = null
    }
}

/** Keeps drafts in memory, per member. Set [fail] to make saves throw. */
class FakeProfileRepository : ProfileRepository {
    private val values = mutableMapOf<String, ProfileDraft>()
    var fail = false

    override suspend fun load(userId: String) = values[userId] ?: ProfileDraft()

    override suspend fun save(userId: String, profile: ProfileDraft) {
        if (fail) throw IOException()
        values[userId] = profile
    }
}
