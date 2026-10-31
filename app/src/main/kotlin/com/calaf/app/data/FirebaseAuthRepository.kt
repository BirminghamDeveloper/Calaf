package com.calaf.app.data

import com.calaf.core.model.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import com.calaf.app.BuildConfig
import com.calaf.app.localBackendHost

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    private val local = BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS && auth.app.name == "calaf-local"
    private suspend fun checkConnection() {
        if (!local) return
        try {
            withContext(Dispatchers.IO) {
                val connection = URL("http://${localBackendHost}:9099/emulator/v1/projects/demo-calaf/config").openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    check(connection.responseCode == 200)
                } finally { connection.disconnect() }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { throw AccountException(AccountError.LocalBackend) }
    }
    override val accounts = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebase ->
            trySend(firebase.currentUser?.let { Account(it.uid, it.email.orEmpty(), it.isEmailVerified) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    override suspend fun register(email: String, password: String) = mapped {
        checkConnection()
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        Unit
    }
    override suspend fun login(email: String, password: String) = mapped {
        checkConnection()
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }
    override suspend fun resetPassword(email: String) = mapped {
        checkConnection()
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }
    override suspend fun sendVerification() = mapped {
        checkConnection()
        val user = auth.currentUser ?: throw AccountException(AccountError.Credentials)
        user.sendEmailVerification().await()
        // Local Auth never sends mail. Only the named demo app may consume emulator codes.
        if (local) {
            val code = withContext(Dispatchers.IO) {
                val connection = URL("http://${localBackendHost}:9099/emulator/v1/projects/demo-calaf/oobCodes").openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 3000
                    connection.readTimeout = 5000
                    val codes = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONArray("oobCodes")
                    (codes.length() - 1 downTo 0).map { codes.getJSONObject(it) }
                        .first { it.optString("email") == user.email && it.optString("requestType") == "VERIFY_EMAIL" }.getString("oobCode")
                } finally { connection.disconnect() }
            }
            auth.applyActionCode(code).await()
        }
        Unit
    }
    override suspend fun refresh() = mapped {
        checkConnection()
        auth.currentUser?.reload()?.await()
        auth.currentUser?.getIdToken(true)?.await()
        currentAccount()
    }
    override fun logout() = auth.signOut()

    private suspend fun <T> mapped(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        throw AccountException(when (error) {
            is AccountException -> error.reason
            is FirebaseNetworkException -> AccountError.Network
            is FirebaseTooManyRequestsException -> AccountError.RateLimited
            is FirebaseAuthWeakPasswordException -> AccountError.WeakPassword
            is FirebaseAuthUserCollisionException -> AccountError.EmailInUse
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> AccountError.Credentials
            else -> AccountError.Unavailable
        })
    }
    fun currentAccount(): Account? = auth.currentUser?.let { Account(it.uid, it.email.orEmpty(), it.isEmailVerified) }
}
