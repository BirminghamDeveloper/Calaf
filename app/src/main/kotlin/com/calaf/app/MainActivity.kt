package com.calaf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.ProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var profiles: ProfileRepository
    @Inject lateinit var community: CommunityRepository
    @Inject lateinit var photoRepositories: PhotoRepositoryFactory
    @Inject lateinit var appMode: AppMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalafTheme { CalafApp(auth, profiles, community, photoRepositories, appMode) }
        }
    }
}
