package com.calaf.app.navigation

import com.calaf.feature.settings.SettingsPage
import kotlinx.serialization.Serializable

/** Type safe navigation destinations. Arguments are plain serializable properties. */

@Serializable data object WelcomeDestination

@Serializable data object LoginDestination

@Serializable data object RegisterDestination

@Serializable data object DiscoverDestination

@Serializable data object MatchesDestination

@Serializable data object ActivityDestination

@Serializable data object ProfileDestination

@Serializable data object SettingsDestination

@Serializable data class SettingsPageDestination(val page: SettingsPage)

@Serializable data class ChatDestination(val matchId: String)
