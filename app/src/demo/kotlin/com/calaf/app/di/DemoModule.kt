package com.calaf.app.di

import android.content.Context
import com.calaf.app.AppMode
import com.calaf.app.DemoAppMode
import com.calaf.app.offline.OfflineDemo
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds every repository to the on device demo store. No network, no Firebase. */
@Module
@InstallIn(SingletonComponent::class)
object DemoModule {
    @Provides @Singleton
    fun demo(@ApplicationContext context: Context): OfflineDemo = OfflineDemo(context)

    @Provides fun auth(demo: OfflineDemo): AuthRepository = demo

    @Provides fun profiles(demo: OfflineDemo): ProfileRepository = demo.drafts

    @Provides fun community(demo: OfflineDemo): CommunityRepository = demo

    @Provides fun photoRepositories(demo: OfflineDemo): PhotoRepositoryFactory =
        PhotoRepositoryFactory { uid -> demo.photos(uid) }

    @Provides fun appMode(demo: OfflineDemo): AppMode = DemoAppMode(demo)
}
