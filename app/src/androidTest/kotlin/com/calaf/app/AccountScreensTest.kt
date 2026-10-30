package com.calaf.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.calaf.core.designsystem.CalafTheme
import com.calaf.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class AccountScreensTest {
    @get:Rule val compose = createComposeRule()
    private val auth = FakeAuth()
    private val profiles = object : ProfileRepository {
        private var draft = ProfileDraft()
        override suspend fun load(userId: String) = draft
        override suspend fun save(userId: String, profile: ProfileDraft) { draft = profile }
    }
    private fun start() {
        compose.setContent { CalafTheme { CalafApp(auth, profiles) } }
    }
    private fun continueIntro() {
        compose.onNodeWithText("Continue").performClick()
    }
    @Test fun creditsAndAccountActionsAreVisible() {
        compose.mainClock.autoAdvance = false
        start()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("App idea by Ahmed").assertExists()
        compose.onNodeWithText("Developed by Mustafa").assertExists()
        continueIntro()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("Create account").assertIsDisplayed()
        compose.onNodeWithText("Log in").assertIsDisplayed()
    }
    @Test fun registrationLeadsToVerificationThenEditableProfile() {
        start()
        compose.mainClock.advanceTimeBy(3000)
        compose.waitForIdle()
        compose.onNodeWithText("Create account").performClick()
        compose.onNodeWithText("Email address").performTextInput("test@example.org")
        compose.onNodeWithText("Password", substring = false).performTextInput("test-password")
        compose.onNodeWithText("Confirm password", substring = false).performScrollTo().performTextInput("test-password")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onAllNodesWithText("Create account").onLast().performScrollTo().performClick()
        compose.onNodeWithText("Verify your email").assertExists()
        compose.onNodeWithText("I have verified my email").performClick()
        compose.onNodeWithText("Display name").performTextInput("Amina")
        compose.onNodeWithText("Date of birth").performTextInput("1995-02-03")
        compose.onNodeWithText("City").performScrollTo().performTextInput("London")
        compose.onNodeWithText("Country").performScrollTo().performTextInput("UK")
        compose.onNodeWithText("Save and review profile").performScrollTo().performClick()
        compose.onNodeWithText("Your profile").assertExists()
        compose.onNodeWithText("Amina").assertExists()
        compose.onNodeWithText("Edit profile").performScrollTo().performClick()
        compose.onNodeWithText("Display name").assertExists()
    }
    @Test fun mismatchedPasswordsStayOnRegistration() {
        start()
        compose.mainClock.advanceTimeBy(3000)
        compose.waitForIdle()
        compose.onNodeWithText("Create account").performClick()
        compose.onNodeWithText("Email address").performTextInput("test@example.org")
        compose.onNodeWithText("Password", substring = false).performTextInput("test-password")
        compose.onNodeWithText("Confirm password", substring = false).performScrollTo().performTextInput("different-password")
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onAllNodesWithText("Create account").onLast().performScrollTo().performClick()
        compose.onNodeWithText("The passwords do not match. Please enter them again.").assertExists()
        org.junit.Assert.assertNull(auth.accounts.value)
    }
    private class FakeAuth : AuthRepository {
        override val accounts = MutableStateFlow<Account?>(null)
        override suspend fun register(email: String, password: String) { accounts.value = Account("test", email, false) }
        override suspend fun login(email: String, password: String) = register(email, password)
        override suspend fun resetPassword(email: String) = Unit
        override suspend fun sendVerification() = Unit
        override suspend fun refresh() = accounts.value?.copy(emailVerified = true)
        override fun logout() { accounts.value = null }
    }
}
