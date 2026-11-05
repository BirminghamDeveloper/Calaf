package com.calaf.feature.auth

/**
 * Typing [typedEmail] and [typedPassword] on the login screen signs in with [loginEmail] and [loginPassword].
 * Only the demo flavour ever creates one. Shared code just passes it along.
 */
data class AdminShortcut(
    val typedEmail: String,
    val typedPassword: String,
    val loginEmail: String,
    val loginPassword: String,
)
