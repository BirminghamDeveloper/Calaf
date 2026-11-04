package com.calaf.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.calaf.app.AppMode
import com.calaf.app.BuildConfig
import com.calaf.app.LiveAppMode
import com.calaf.app.localBackendHost
import com.calaf.app.data.FirebaseAuthRepository
import com.calaf.app.data.FirebasePhotoRepository
import com.calaf.app.data.LocalProfileRepository
import com.calaf.core.data.RestoringProfileRepository
import com.calaf.core.data.createCommunityRepository
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.ProfileRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.profileStore by preferencesDataStore("private_profile_drafts")
private val Context.localProfileStore by preferencesDataStore("emulator_profile_drafts")

/** Binds every repository to Firebase. Emulators are used only by debug builds when `calaf.emulators=true`. */
@Module
@InstallIn(SingletonComponent::class)
object LiveModule {
    private val local = BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS

    @Provides @Singleton
    fun firebaseApp(@ApplicationContext context: Context): FirebaseApp =
        if (local) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId("demo-calaf")
                    .setApplicationId("1:1234567890:android:0000000000000000000000")
                    // Synthetic emulator values must still satisfy the Firebase SDK's local format checks.
                    .setApiKey("AIzaSy" + "0".repeat(33))
                    .setStorageBucket("demo-calaf.appspot.com")
                    .build(),
                "calaf-local",
            )
        } else {
            FirebaseApp.getInstance()
        }

    @Provides @Singleton
    fun firebaseAuth(app: FirebaseApp): FirebaseAuth =
        FirebaseAuth.getInstance(app).also { if (local) it.useEmulator(localBackendHost, 9099) }

    @Provides @Singleton
    fun firebaseStorage(app: FirebaseApp): FirebaseStorage =
        FirebaseStorage.getInstance(app).also { if (local) it.useEmulator(localBackendHost, 9199) }

    @Provides @Singleton
    fun auth(firebaseAuth: FirebaseAuth): AuthRepository = FirebaseAuthRepository(firebaseAuth)

    @Provides @Singleton
    fun profiles(@ApplicationContext context: Context, app: FirebaseApp): ProfileRepository =
        RestoringProfileRepository(
            LocalProfileRepository(if (local) context.localProfileStore else context.profileStore),
            app,
        )

    @Provides @Singleton
    fun community(app: FirebaseApp): CommunityRepository = createCommunityRepository(local, app, localBackendHost)

    @Provides @Singleton
    fun photoRepositories(firebaseAuth: FirebaseAuth, storage: FirebaseStorage): PhotoRepositoryFactory =
        PhotoRepositoryFactory { uid -> FirebasePhotoRepository(uid, firebaseAuth, storage) }

    @Provides @Singleton
    fun appMode(): AppMode = LiveAppMode(local)
}
