package com.calaf.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Requires Start-LocalBackend.ps1 and Seed-LocalBackend.ps1; never touches production. */
class LocalBackendFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun registrationAndLocalVerificationReachProfile() {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        compose.runOnIdle { (compose.activity.application as CalafApplication).auth.logout() }
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
        compose.runOnIdle { (compose.activity.application as CalafApplication).auth.logout() }
        compose.waitUntil(30000) { compose.onAllNodesWithText("Log in").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput(email)
        compose.onNodeWithText("Password", substring = false).performTextInput("LocalCheck!2026")
        compose.onAllNodesWithText("Log in").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Display name").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Display name").assertExists()
        compose.runOnIdle { (compose.activity.application as CalafApplication).auth.logout() }
    }

    @Test fun localAccountDiscoversMatchesAndSendsMessage() {
        assumeTrue(BuildConfig.DEBUG && BuildConfig.USE_FIREBASE_EMULATORS)
        compose.runOnIdle { (compose.activity.application as CalafApplication).auth.logout() }
        compose.mainClock.advanceTimeBy(3000)
        compose.waitUntil(30000) { compose.onAllNodesWithText("Log in").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput("admin")
        compose.onNodeWithText("Password", substring = false).performTextInput("admin")
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
        compose.onNodeWithText("Refresh").performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Hassan (Demo)", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("community_list").performScrollToNode(hasText("Hodan (Demo)", substring = true))
        compose.onNodeWithText("Hodan (Demo)", substring = true).assertIsDisplayed()
        compose.runOnIdle { (compose.activity.application as CalafApplication).auth.logout() }
    }
}
