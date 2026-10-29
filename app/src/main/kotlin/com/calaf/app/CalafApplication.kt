package com.calaf.app

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.calaf.app.data.FirebaseAuthRepository
import com.calaf.app.data.LocalProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.calaf.core.data.createCommunityRepository
import com.calaf.core.data.RestoringProfileRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

private val Context.profileStore by preferencesDataStore("private_profile_drafts")
private val Context.localProfileStore by preferencesDataStore("emulator_profile_drafts")

class CalafApplication : Application() {
    private val local = BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS
    private val firebaseApp by lazy {
        if (local) FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
            .setProjectId("demo-calaf").setApplicationId("1:1234567890:android:calaf")
            .setApiKey("demo-calaf-local-api-key").setStorageBucket("demo-calaf.appspot.com").build(), "calaf-local")
        else FirebaseApp.getInstance()
    }
    val auth by lazy {
        val firebase = FirebaseAuth.getInstance(firebaseApp)
        if (local) firebase.useEmulator("10.0.2.2", 9099)
        FirebaseAuthRepository(firebase)
    }
    val profiles by lazy { RestoringProfileRepository(LocalProfileRepository(if (local) localProfileStore else profileStore), firebaseApp) }
    val community by lazy { createCommunityRepository(local, firebaseApp) }
}
