package com.calaf.app

import com.calaf.core.ui.R as CoreR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.calaf.app.navigation.ActivityDestination
import com.calaf.app.navigation.ChatDestination
import com.calaf.app.navigation.DiscoverDestination
import com.calaf.app.navigation.MatchesDestination
import com.calaf.app.navigation.ProfileDestination
import com.calaf.app.navigation.SettingsDestination
import com.calaf.app.navigation.SettingsPageDestination
import com.calaf.core.designsystem.CalafIcon
import com.calaf.core.designsystem.CalafIcons
import com.calaf.core.designsystem.CalafSpacing
import com.calaf.core.model.PhotoRepository
import com.calaf.feature.chat.ChatRoute
import com.calaf.feature.discovery.DiscoveryRoute
import com.calaf.feature.matches.ActivityScreen
import com.calaf.feature.matches.ActivityViewModel
import com.calaf.feature.matches.MatchesRoute
import com.calaf.feature.onboarding.ProfileViewModel
import com.calaf.feature.onboarding.ProfileUiState
import com.calaf.feature.settings.SettingsMenuScreen
import com.calaf.feature.settings.SettingsPage
import com.calaf.feature.settings.SettingsPageRoute

private class Tab(val destination: Any, val label: Int, val icon: CalafIcon)

private val tabs = listOf(
    Tab(DiscoverDestination, CoreR.string.discover, CalafIcons.Discover),
    Tab(MatchesDestination, CoreR.string.matches, CalafIcons.Matches),
    Tab(ActivityDestination, CoreR.string.activity_title, CalafIcons.Activity),
    Tab(ProfileDestination, R.string.profile, CalafIcons.Profile),
)

/** Everything a signed in member sees: a header, the four tabs and the pages reached from them. */
@Composable
internal fun SignedInApp(
    uid: String,
    profile: ProfileViewModel,
    profileState: ProfileUiState,
    onLogout: () -> Unit,
    photos: PhotoRepository,
    appMode: AppMode,
) {
    val navigation = rememberNavController()
    val backStack by navigation.currentBackStackEntryAsState()
    val current = backStack?.destination
    // The Activity ViewModel lives here because the navigation bar shows its unread count on every tab.
    val activity = hiltViewModel<ActivityViewModel, ActivityViewModel.Factory>(
        key = "activity_$uid",
        creationCallback = { it.create(uid) },
    )
    val activityState by activity.state.collectAsStateWithLifecycle()
    DisposableEffect(activity) {
        activity.observe()
        onDispose { activity.stop() }
    }
    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEach { tab ->
                val selected = current.isOnTab(tab)
                val label = stringResource(tab.label)
                NavigationBarItem(
                    selected = selected,
                    onClick = { navigation.openTab(tab) },
                    icon = {
                        if (tab.destination == ActivityDestination && activityState.unreadCount > 0) {
                            BadgedBox(badge = { Badge { Text(activityState.unreadCount.toString()) } }) {
                                CalafIcon(tab.icon, label, selected = selected)
                            }
                        } else {
                            CalafIcon(tab.icon, label, selected = selected)
                        }
                    },
                    label = { Text(label) },
                )
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Header(onTab = current.isOnAnyTab(), navigation)
            Text(
                stringResource(CoreR.string.purpose), Modifier.padding(horizontal = CalafSpacing.medium),
                style = MaterialTheme.typography.bodySmall,
            )
            NavHost(navigation, startDestination = ProfileDestination, modifier = Modifier.weight(1f)) {
                composable<DiscoverDestination> {
                    DiscoveryRoute(
                        uid,
                        onPreferences = { navigation.navigate(SettingsPageDestination(SettingsPage.PREFERENCES)) },
                        onOpenChat = { matchId -> navigation.navigate(ChatDestination(matchId)) },
                    )
                }
                composable<MatchesDestination> {
                    MatchesRoute(uid) { matchId -> navigation.navigate(ChatDestination(matchId)) }
                }
                composable<ActivityDestination> {
                    ActivityScreen(
                        activityState, onMarkRead = activity::markRead,
                        onOpenConversation = { match -> navigation.navigate(ChatDestination(match.id)) },
                        onRetry = activity::observe,
                    )
                }
                composable<ProfileDestination> {
                    ProfileTab(uid, profile, profileState, onLogout, photos, appMode)
                }
                composable<ChatDestination> { entry ->
                    val matchId = entry.toRoute<ChatDestination>().matchId
                    ChatRoute(
                        uid, matchId, onClosed = { navigation.popBackStack() }, onBack = { navigation.popBackStack() },
                    )
                }
                composable<SettingsDestination> {
                    SettingsMenuScreen { page -> navigation.navigate(SettingsPageDestination(page)) }
                }
                composable<SettingsPageDestination> { entry ->
                    SettingsPageRoute(uid, entry.toRoute<SettingsPageDestination>().page)
                }
            }
        }
    }
}

@Composable
private fun Header(onTab: Boolean, navigation: NavHostController) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CalafSpacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.brand), style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onTab) {
            IconButton(onClick = { navigation.navigate(SettingsDestination) }) {
                CalafIcon(CalafIcons.Settings, stringResource(CoreR.string.settings))
            }
        } else {
            IconButton(onClick = { navigation.popBackStack() }) {
                CalafIcon(CalafIcons.Back, stringResource(CoreR.string.back))
            }
        }
    }
}

/** A conversation belongs to the Matches tab. Settings pages belong to no tab. */
private fun NavDestination?.isOnTab(tab: Tab): Boolean {
    val destination = this ?: return false
    val inChat = destination.hierarchy.any { it.hasRoute<ChatDestination>() }
    if (tab.destination == MatchesDestination && inChat) return true
    return when (tab.destination) {
        DiscoverDestination -> destination.hierarchy.any { it.hasRoute<DiscoverDestination>() }
        MatchesDestination -> destination.hierarchy.any { it.hasRoute<MatchesDestination>() }
        ActivityDestination -> destination.hierarchy.any { it.hasRoute<ActivityDestination>() }
        else -> destination.hierarchy.any { it.hasRoute<ProfileDestination>() }
    }
}

private fun NavDestination?.isOnAnyTab(): Boolean = tabs.any { isOnTab(it) }

private fun NavHostController.openTab(tab: Tab) {
    navigate(tab.destination) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
