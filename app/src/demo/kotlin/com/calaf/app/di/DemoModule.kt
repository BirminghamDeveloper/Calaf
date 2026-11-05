package com.calaf.app.di

import android.content.Context
import com.calaf.app.AppMode
import com.calaf.app.DemoAppMode
import com.calaf.app.PreferencesWalkthrough
import com.calaf.app.WalkthroughState
import com.calaf.app.AndroidAvatarSource
import com.calaf.app.AndroidReplyScheduler
import com.calaf.app.demo.ReplyScheduler
import com.calaf.app.demo.AvatarSource
import com.calaf.app.demo.DemoAdminRepository
import com.calaf.app.demo.DemoAuthRepository
import com.calaf.app.demo.DemoCommunityRepository
import com.calaf.app.demo.DemoPhotoRepository
import com.calaf.app.demo.DemoResetter
import com.calaf.app.demo.DemoSeed
import com.calaf.app.demo.DemoProfileRepository
import com.calaf.app.demo.DemoSession
import com.calaf.app.demo.DemoStore
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/** Binds every repository to the on device demo store. No network, no Firebase. */
@Module
@InstallIn(SingletonComponent::class)
object DemoModule {
    @Provides @Singleton
    fun store(@ApplicationContext context: Context): DemoStore = DemoStore(File(context.filesDir, "calaf_demo"), seed = DemoSeed::seedEverything)

    @Provides @Singleton
    fun session(store: DemoStore): DemoSession = DemoSession(store)

    @Provides @Singleton
    fun auth(store: DemoStore, session: DemoSession): AuthRepository = DemoAuthRepository(store, session)

    @Provides @Singleton
    fun profiles(store: DemoStore, session: DemoSession): ProfileRepository = DemoProfileRepository(store, session)

    @Provides @Singleton
    fun replies(): ReplyScheduler = AndroidReplyScheduler()

    @Provides @Singleton
    fun community(store: DemoStore, session: DemoSession, replies: ReplyScheduler): CommunityRepository =
        DemoCommunityRepository(store, session, replies)

    @Provides @Singleton
    fun avatars(@ApplicationContext context: Context): AvatarSource = AndroidAvatarSource(context)

    @Provides @Singleton
    fun admin(store: DemoStore, session: DemoSession, avatars: AvatarSource): DemoAdminRepository =
        DemoAdminRepository(store, session, avatars)

    @Provides
    fun photoRepositories(store: DemoStore, session: DemoSession, avatars: AvatarSource): PhotoRepositoryFactory =
        PhotoRepositoryFactory { uid -> DemoPhotoRepository(store, session, avatars, uid) }

    @Provides @Singleton
    fun walkthrough(@ApplicationContext context: Context): WalkthroughState = PreferencesWalkthrough(context)

    @Provides @Singleton
    fun resetter(store: DemoStore, session: DemoSession, tour: WalkthroughState): DemoResetter =
        DemoResetter(store, session) { tour.seen = false }

    @Provides
    fun appMode(admin: DemoAdminRepository, resetter: DemoResetter, tour: WalkthroughState): AppMode =
        DemoAppMode(admin, resetter, tour)
}
