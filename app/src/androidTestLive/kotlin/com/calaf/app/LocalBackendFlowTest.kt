package com.calaf.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Requires Start-LocalBackend.ps1 and Seed-LocalBackend.ps1; never touches production.
 *
 * The sign in test uses an account created by `backend/scripts/seed.mjs` (emulator only). Nothing is hardcoded here:
 * pass the account as instrumentation arguments, for example
 * `-Pandroid.testInstrumentationRunnerArguments.calafTestEmail=... .calafTestPassword=...`.
 * The seed script prints the exact values, and `scripts/Test-Local.ps1` passes them for you.
 */
@HiltAndroidTest
class LocalBackendFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Before fun inject() = hilt.inject()


    @Test fun registrationAndLocalVerificationReachProfile() {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
        compose.mainClock.advanceTimeBy(3000)
        compose.waitUntil(30000) { compose.onAllNodesWithText("Create account").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        val email = "ui-${System.currentTimeMillis()}@calaf.test"
        compose.onNodeWithText("Email address").performTextInput(email)
        compose.onNodeWithText("Password", substring = false).performTextInput("LocalCheck!2026")
        compose.onNodeWithText("Confirm password", substring = false).performScrollTo().performTextInput("LocalCheck!2026")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onAllNodesWithText("Create account").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Verify local demo email").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Verify local demo email").performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Display name").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Display name").assertExists()
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
        compose.waitUntil(30000) { compose.onAllNodesWithText("Log in").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput(email)
        compose.onNodeWithText("Password", substring = false).performTextInput("LocalCheck!2026")
        compose.onAllNodesWithText("Log in").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Display name").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Display name").assertExists()
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
    }

    @Test fun localAccountDiscoversMatchesAndSendsMessage() {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        val arguments = InstrumentationRegistry.getArguments()
        val email = arguments.getString(EMAIL_ARGUMENT)
        val password = arguments.getString(PASSWORD_ARGUMENT)
        assumeTrue("Pass $EMAIL_ARGUMENT and $PASSWORD_ARGUMENT from the backend seed", email != null && password != null)
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
        compose.mainClock.advanceTimeBy(3000)
        compose.waitUntil(30000) { compose.onAllNodesWithText("Log in").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput(email!!)
        compose.onNodeWithText("Password", substring = false).performTextInput(password!!)
        compose.onAllNodesWithText("Log in").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Matches").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Matches").performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Open conversation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Open conversation").performScrollTo().performClick()
        val message = "Local emulator test ${System.currentTimeMillis()}"
        compose.onNodeWithText("Write a respectful message").performTextInput(message)
        compose.onNodeWithText("Send").performClick()
        try {
            compose.waitUntil(30000) { compose.onAllNodesWithText("Message sent.").fetchSemanticsNodes().isNotEmpty() }
        } catch (error: Throwable) {
            compose.onRoot().printToLog("CalafUiTest")
            throw error
        }
        val deliveredMessage = hasText(message) and hasAnyAncestor(hasTestTag("chat_history"))
        compose.waitUntil(30000) { compose.onAllNodes(deliveredMessage, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(deliveredMessage, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Discover").performClick()
        // Discovery is a stack of swipe cards, so the top card is a seeded fictional member.
        compose.waitUntil(30000) { compose.onAllNodesWithText("(Demo)", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("discovery_list").assertIsDisplayed()
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
    }

    private companion object {
        const val EMAIL_ARGUMENT = "calafTestEmail"
        const val PASSWORD_ARGUMENT = "calafTestPassword"
    }
}
