package com.calaf.app

import android.content.Context
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Lets code that Hilt does not create (such as instrumented tests) reach the bound repositories. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun auth(): AuthRepository
    fun profiles(): ProfileRepository
    fun community(): CommunityRepository
    fun photoRepositories(): PhotoRepositoryFactory
    fun appMode(): AppMode
}

fun Context.appEntryPoint(): AppEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, AppEntryPoint::class.java)
