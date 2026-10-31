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
import com.google.firebase.storage.FirebaseStorage
import com.calaf.app.data.FirebasePhotoRepository

// Android emulators expose the host at 10.0.2.2; USB phones use adb reverse.
internal val localBackendHost: String
    get() = if (android.os.Build.HARDWARE in setOf("ranchu", "goldfish") ||
        android.os.Build.FINGERPRINT.startsWith("generic")) "10.0.2.2" else "127.0.0.1"

private val Context.profileStore by preferencesDataStore("private_profile_drafts")
private val Context.localProfileStore by preferencesDataStore("emulator_profile_drafts")

class CalafApplication : Application() {
    val offline by lazy { com.calaf.app.offline.OfflineDemo(this) }
    private val local = BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS
    private val firebaseApp by lazy {
        if (local) FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
            .setProjectId("demo-calaf").setApplicationId("1:1234567890:android:0000000000000000000000")
            // Synthetic emulator values must still satisfy the Firebase SDK's local format checks.
            .setApiKey("AIzaSy" + "0".repeat(33)).setStorageBucket("demo-calaf.appspot.com").build(), "calaf-local")
        else FirebaseApp.getInstance()
    }
    val auth: com.calaf.core.model.AuthRepository by lazy {
        if (BuildConfig.OFFLINE_DEMO) return@lazy offline
        val firebase = FirebaseAuth.getInstance(firebaseApp)
        if (local) firebase.useEmulator(localBackendHost, 9099)
        FirebaseAuthRepository(firebase)
    }
    val profiles: com.calaf.core.model.ProfileRepository by lazy {
        if (BuildConfig.OFFLINE_DEMO) offline.drafts else RestoringProfileRepository(LocalProfileRepository(if (local) localProfileStore else profileStore), firebaseApp)
    }
    val community: com.calaf.core.model.CommunityRepository by lazy { if (BuildConfig.OFFLINE_DEMO) offline else createCommunityRepository(local, firebaseApp, localBackendHost) }
    private val storage by lazy { FirebaseStorage.getInstance(firebaseApp).apply { if (local) useEmulator(localBackendHost, 9199) } }
    fun photos(uid: String): com.calaf.core.model.PhotoRepository = if (BuildConfig.OFFLINE_DEMO) offline.photos(uid) else FirebasePhotoRepository(uid, FirebaseAuth.getInstance(firebaseApp), storage)
}
