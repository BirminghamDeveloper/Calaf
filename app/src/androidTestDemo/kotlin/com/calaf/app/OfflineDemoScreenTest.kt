package com.calaf.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class OfflineDemoScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun adminLoginOpensPrivilegedPanel() {
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
        compose.mainClock.advanceTimeBy(3000)
        compose.waitUntil(30000) { compose.onAllNodesWithText("Log in").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput("admin")
        compose.onNodeWithText("Password", substring = false).performTextInput("admin")
        compose.onAllNodesWithText("Log in").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Open administrator panel").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Open administrator panel").performScrollTo().performClick()
        compose.onNodeWithText("Members (", substring = true).performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Suspend member").onFirst().performScrollTo().assertIsDisplayed()
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
    }
    @Test fun registerThenLogBackInWithoutEmailOrBackend() {
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
        compose.mainClock.advanceTimeBy(3000)
        compose.waitUntil(30000) { compose.onAllNodesWithText("Create account").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        val email = "offline-ui-${System.currentTimeMillis()}@example.test"
        compose.onNodeWithText("Email address").performTextInput(email)
        compose.onNodeWithText("Password", substring = false).performTextInput("OfflinePass!2026")
        compose.onNodeWithText("Confirm password").performScrollTo().performTextInput("OfflinePass!2026")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onAllNodesWithText("Create account").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Display name").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Open administrator panel").assertDoesNotExist()
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
        compose.waitUntil(30000) { compose.onAllNodesWithText("Log in").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput(email)
        compose.onNodeWithText("Password", substring = false).performTextInput("OfflinePass!2026")
        compose.onAllNodesWithText("Log in").onLast().performScrollTo().performClick()
        compose.waitUntil(30000) { compose.onAllNodesWithText("Display name").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { compose.activity.appEntryPoint().auth().logout() }
    }
}
