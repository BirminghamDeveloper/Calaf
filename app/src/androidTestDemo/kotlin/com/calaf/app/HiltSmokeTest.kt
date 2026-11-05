package com.calaf.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onLast
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Launches the real [MainActivity] with the demo flavour's Hilt graph and opens each tab, so a missing binding or a
 * ViewModel that Hilt cannot create fails here instead of on a phone. Uses the demo only admin/admin shortcut.
 */
@HiltAndroidTest
class HiltSmokeTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun inject() = hilt.inject()

    private fun waitForText(text: String) =
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test fun launchesSignsInAndShowsEveryTab() {
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
        compose.mainClock.advanceTimeBy(INTRO_MILLIS)
        waitForText("Log in")
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput("admin")
        compose.onNodeWithText("Password", substring = false).performTextInput("admin")
        compose.onAllNodesWithText("Log in").onLast().performScrollTo().performClick()

        waitForText("Discover")
        compose.onNodeWithText("Discover").performClick()
        compose.onNodeWithTag("discovery_list").assertIsDisplayed()
        compose.onNodeWithText("Matches").performClick()
        compose.onNodeWithTag("matches_list").assertIsDisplayed()
        compose.onNodeWithText("Activity").performClick()
        compose.onNodeWithTag("activity_list").assertIsDisplayed()
        compose.onNodeWithText("Profile").performClick()
        waitForText("Open administrator panel")
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
    }

    private companion object {
        const val WAIT_MILLIS = 30_000L
        const val INTRO_MILLIS = 3_000L
    }
}
