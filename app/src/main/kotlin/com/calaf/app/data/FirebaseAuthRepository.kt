package com.calaf.app.data

import com.calaf.core.model.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override val accounts = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebase ->
            trySend(firebase.currentUser?.let { Account(it.uid, it.email.orEmpty(), it.isEmailVerified) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    override suspend fun register(email: String, password: String) = mapped {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        Unit
    }
    override suspend fun login(email: String, password: String) = mapped {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }
    override suspend fun resetPassword(email: String) = mapped {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }
    override suspend fun sendVerification() = mapped {
        val user = auth.currentUser ?: throw AccountException(AccountError.Credentials)
        user.sendEmailVerification().await()
        Unit
    }
    override suspend fun refresh() = mapped {
        auth.currentUser?.reload()?.await()
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
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException,
            is FirebaseAuthUserCollisionException -> AccountError.Credentials
            else -> AccountError.Unavailable
        })
    }
    fun currentAccount(): Account? = auth.currentUser?.let { Account(it.uid, it.email.orEmpty(), it.isEmailVerified) }
}
