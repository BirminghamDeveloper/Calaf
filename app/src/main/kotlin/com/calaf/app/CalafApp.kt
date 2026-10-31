package com.calaf.app

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.*
import com.calaf.app.auth.*
import com.calaf.app.profile.*
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.ProfileRepository
import com.calaf.core.model.CommunityRepository
import com.calaf.feature.community.CommunityScreen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext

@Composable
fun CalafApp(authRepository: AuthRepository, profileRepository: ProfileRepository, communityRepository: CommunityRepository? = null, localMode: Boolean = false) {
    Column(Modifier.fillMaxSize()) {
        if (BuildConfig.OFFLINE_DEMO) Text("OFFLINE DEMO · Fictional people, local accounts and simulated matches. No messages leave this device.", Modifier.fillMaxWidth().statusBarsPadding(), style = MaterialTheme.typography.labelSmall)
        else if (localMode) Text(stringResource(R.string.local_development), Modifier.fillMaxWidth().statusBarsPadding(), style = MaterialTheme.typography.labelSmall)
        Box(Modifier.weight(1f)) { CalafRoutes(authRepository, profileRepository, communityRepository) }
    }
}

@Composable
private fun CalafRoutes(authRepository: AuthRepository, profileRepository: ProfileRepository, communityRepository: CommunityRepository?) {
    val auth: AuthViewModel = viewModel(factory = viewModelFactory { initializer {
        AuthViewModel(authRepository, allowLocalAdmin = BuildConfig.DEBUG && (BuildConfig.USE_FIREBASE_EMULATORS || BuildConfig.OFFLINE_DEMO))
    } })
    val state by auth.state.collectAsStateWithLifecycle()
    var introFinished by rememberSaveable { mutableStateOf(false) }
    if (!introFinished || !state.ready) {
        IntroScreen { introFinished = true }
        return
    }
    val account = state.account
    if (account == null) {
        val navigation = rememberNavController()
        NavHost(navigation, startDestination = "welcome") {
            composable("welcome") {
                WelcomeScreen(onRegister = { auth.clearMessage(); navigation.navigate("register") },
                    onLogin = { auth.clearMessage(); navigation.navigate("login") })
            }
            for (route in listOf("login", "register")) {
                composable(route) {
                    AuthScreen(state, register = route == "register",
                        onSubmit = { email, password, adult -> auth.submit(email, password, route == "register", adult) },
                        onReset = auth::reset, onBack = { auth.clearMessage(); navigation.popBackStack() })
                }
            }
        }
    } else if (!account.emailVerified) {
        VerificationScreen(state, auth::sendVerification, auth::refresh, auth::logout)
    } else {
        key(account.id) {
            val profile: ProfileViewModel = viewModel(key = "profile_${account.id}", factory = viewModelFactory {
                initializer { ProfileViewModel(account.id, profileRepository) }
            })
            val profileState by profile.state.collectAsStateWithLifecycle()
            if (communityRepository == null) {
                ProfileScreen(profileState, profile::edit, profile::save, profile::editAgain, profile::load, auth::logout)
            } else {
                val application = LocalContext.current.applicationContext as CalafApplication
                val photos = remember(account.id) { application.photos(account.id) }
                CommunityScreen(account.id, communityRepository) { publish ->
                    ProfileScreen(profileState, profile::edit, profile::save, profile::editAgain, profile::load, auth::logout,
                        onPublish = { publish(profileState.draft) }, photos = {
                            if (BuildConfig.OFFLINE_DEMO) com.calaf.app.offline.OfflineAdminSection(application.offline)
                            PhotoSection(account.id, photos)
                        })
                }
            }
        }
    }
}
