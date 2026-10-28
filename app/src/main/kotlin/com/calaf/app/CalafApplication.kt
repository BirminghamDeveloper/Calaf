package com.calaf.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.calaf.app.data.FirebaseAuthRepository
import com.calaf.app.data.LocalProfileRepository
import com.google.firebase.auth.FirebaseAuth

private val Context.profileStore by preferencesDataStore("private_profile_drafts")

class CalafApplication : Application() {
    val auth by lazy { FirebaseAuthRepository(FirebaseAuth.getInstance()) }
    val profiles by lazy { LocalProfileRepository(profileStore) }
}
