package com.calaf.app.demo

import com.calaf.core.model.Account
import com.calaf.core.model.AccountError
import com.calaf.core.model.AccountException
import com.calaf.core.model.AccountValidation
import com.calaf.core.model.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Device only accounts. No email is ever sent, so there is no verification or password reset. */
class DemoAuthRepository(private val store: DemoStore, private val session: DemoSession) : AuthRepository {
    override val accounts: Flow<Account?> get() = session.accounts

    override suspend fun login(email: String, password: String) {
        val (id, data) = store.rows("users").firstOrNull { it.second.optString("email").equals(email.trim(), true) }
            ?: throw AccountException(AccountError.Credentials)
        val valid = if (data.optBoolean("seed")) {
            password == if (data.optBoolean("admin")) DemoSeed.ADMIN_PASSWORD else DemoSeed.MEMBER_PASSWORD
        } else {
            val salt = data.getString("salt")
            withContext(Dispatchers.Default) { passwordHash(password, salt) == data.optString("hash") }
        }
        if (!valid || data.optBoolean("suspended")) throw AccountException(AccountError.Credentials)
        session.signIn(id)
    }

    override suspend fun register(email: String, password: String) {
        if (!AccountValidation.email(email) || !AccountValidation.password(password)) {
            throw AccountException(AccountError.InvalidInput)
        }
        val normalized = email.trim().lowercase()
        if (emailTaken(normalized)) throw AccountException(AccountError.EmailInUse)
        val id = UUID.randomUUID().toString()
        val salt = UUID.randomUUID().toString()
        val hash = withContext(Dispatchers.Default) { passwordHash(password, salt) }
        // Check again: another registration may have finished while the hash was being computed.
        if (emailTaken(normalized)) throw AccountException(AccountError.EmailInUse)
        store.table("users").put(
            id,
            JSONObject().put("email", normalized).put("salt", salt).put("hash", hash).put("suspended", false),
        )
        store.save()
        session.signIn(id)
    }

    override suspend fun resetPassword(email: String) {
        throw AccountException(AccountError.Unavailable)
    }

    override suspend fun sendVerification() = Unit

    override suspend fun refresh(): Account? = session.refresh()

    override fun logout() = session.signOut()

    private fun emailTaken(email: String) = store.rows("users").any { it.second.optString("email") == email }

    // PBKDF2-HMAC-SHA1 is supported across the minimum Android API 24 and later.
    private fun passwordHash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
                .joinToString("") { "%02x".format(it) }
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
