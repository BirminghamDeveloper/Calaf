package com.calaf.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calaf.app.navigation.LoginDestination
import com.calaf.app.navigation.RegisterDestination
import com.calaf.app.navigation.WelcomeDestination
import com.calaf.core.model.AuthRepository
import com.calaf.core.model.CommunityRepository
import com.calaf.core.model.PhotoRepositoryFactory
import com.calaf.core.model.ProfileRepository
import com.calaf.feature.auth.AuthScreen
import com.calaf.feature.auth.AuthViewModel
import com.calaf.feature.auth.VerificationScreen
import com.calaf.feature.onboarding.ProfileScreen
import com.calaf.feature.onboarding.ProfileViewModel

@Composable
fun CalafApp(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    communityRepository: CommunityRepository? = null,
    photoRepositories: PhotoRepositoryFactory? = null,
    appMode: AppMode = PlainAppMode,
) {
    Column(Modifier.fillMaxSize()) {
        appMode.bannerText?.let {
            Text(
                stringResource(it), Modifier.fillMaxWidth().statusBarsPadding(),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Box(Modifier.weight(1f)) {
            CalafRoutes(authRepository, profileRepository, communityRepository, photoRepositories, appMode)
        }
    }
}

@Composable
private fun CalafRoutes(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    communityRepository: CommunityRepository?,
    photoRepositories: PhotoRepositoryFactory?,
    appMode: AppMode,
) {
    val auth: AuthViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AuthViewModel(authRepository, adminShortcut = appMode.adminShortcut) }
        },
    )
    val state by auth.state.collectAsStateWithLifecycle()
    var introFinished by rememberSaveable { mutableStateOf(false) }
    if (!introFinished || !state.ready) {
        IntroScreen { introFinished = true }
        return
    }
    val tour = appMode.walkthrough
    var tourSeen by rememberSaveable { mutableStateOf(tour?.seen ?: true) }
    if (!tourSeen && state.account == null) {
        WalkthroughScreen { tour?.seen = true; tourSeen = true }
        return
    }
    val account = state.account
    if (account == null) {
        SignedOutNavigation(auth, state, appMode)
    } else if (!account.emailVerified) {
        VerificationScreen(
            state, auth::sendVerification, auth::refresh, auth::logout, appMode.usesLocalEmulators,
        )
    } else {
        key(account.id) {
            val profile: ProfileViewModel = viewModel(
                key = "profile_${account.id}",
                factory = viewModelFactory { initializer { ProfileViewModel(account.id, profileRepository) } },
            )
            val profileState by profile.state.collectAsStateWithLifecycle()
            if (communityRepository == null || photoRepositories == null) {
                // Used by tests that only exercise accounts and profiles.
                ProfileScreen(
                    profileState, profile::edit, profile::save, profile::editAgain, profile::load, auth::logout,
                )
            } else {
                SignedInApp(
                    account.id, profile, profileState, auth::logout,
                    remember(account.id) { photoRepositories.create(account.id) }, appMode,
                )
            }
        }
    }
}

@Composable
private fun SignedOutNavigation(auth: AuthViewModel, state: com.calaf.feature.auth.AuthUiState, appMode: AppMode) {
    val navigation = rememberNavController()
    NavHost(navigation, startDestination = WelcomeDestination) {
        composable<WelcomeDestination> {
            WelcomeScreen(
                onRegister = { auth.clearMessage(); navigation.navigate(RegisterDestination) },
                onLogin = { auth.clearMessage(); navigation.navigate(LoginDestination) },
                onTryDemo = appMode.quickLogin?.let { login -> { auth.quickLogin(login.email, login.password) } },
            )
        }
        composable<LoginDestination> {
            AuthDestination(auth, state, appMode, register = false) { navigation.popBackStack() }
        }
        composable<RegisterDestination> {
            AuthDestination(auth, state, appMode, register = true) { navigation.popBackStack() }
        }
    }
}

@Composable
private fun AuthDestination(
    auth: AuthViewModel,
    state: com.calaf.feature.auth.AuthUiState,
    appMode: AppMode,
    register: Boolean,
    onBack: () -> Unit,
) {
    AuthScreen(
        state, register = register,
        notice = appMode.authNotice, canResetPassword = appMode.supportsPasswordReset,
        localEmulators = appMode.usesLocalEmulators,
        onSubmit = { email, password, adult -> auth.submit(email, password, register, adult) },
        onReset = auth::reset,
        onBack = { auth.clearMessage(); onBack() },
    )
}
