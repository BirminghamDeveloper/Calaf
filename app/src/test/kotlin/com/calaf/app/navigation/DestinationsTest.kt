package com.calaf.app.navigation

import com.calaf.feature.settings.SettingsPage
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test

/** Navigation Compose turns each destination into a route with its serializer, so every one must round trip. */
class DestinationsTest {
    private inline fun <reified T : Any> roundTrip(value: T) {
        val json = Json.encodeToString(serializer<T>(), value)
        assertEquals(value, Json.decodeFromString(serializer<T>(), json))
    }

    @Test fun `every settings page can be a destination argument`() {
        SettingsPage.entries.forEach { roundTrip(SettingsPageDestination(it)) }
    }

    @Test fun `chat destination keeps the match id`() {
        roundTrip(ChatDestination("a_b"))
    }

    @Test fun `object destinations are serializable`() {
        roundTrip(WelcomeDestination)
        roundTrip(LoginDestination)
        roundTrip(RegisterDestination)
        roundTrip(DiscoverDestination)
        roundTrip(MatchesDestination)
        roundTrip(ActivityDestination)
        roundTrip(ProfileDestination)
        roundTrip(SettingsDestination)
    }
}
