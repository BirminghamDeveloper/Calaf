package com.calaf.app.demo

/** Runs [action] after [delayMillis]. The Android build uses the main thread, tests run actions by hand. */
interface ReplyScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit)

    companion object {
        /** Never replies. The default for tests that do not care about scripted replies. */
        val None = object : ReplyScheduler {
            override fun schedule(delayMillis: Long, action: () -> Unit) = Unit
        }
    }
}

/**
 * The scripted lines fictional seeded members reply with, in order, then round again. Every reply is stored with a
 * demo flag so the chat can label it as simulated. They are plain, kind and never ask for personal details or money.
 */
object DemoReplies {
    const val DELAY_MILLIS = 2_500L

    private val lines = listOf(
        "Walaykum salaam! Lovely to hear from you. How is your week going?",
        "That sounds good. What are you looking for in a marriage?",
        "Family and honesty matter a lot to me. What matters most to you?",
        "Thank you for sharing that. I would be happy to involve my family when we are both ready.",
        "It was nice talking. Let us continue soon, inshaAllah.",
    )

    /** The reply for a conversation that already has [alreadySent] scripted replies. */
    fun line(alreadySent: Int): String = lines[alreadySent % lines.size]
}
