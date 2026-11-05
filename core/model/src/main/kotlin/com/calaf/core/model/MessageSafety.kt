package com.calaf.core.model

/**
 * Flags text that is risky to receive from someone you have just met: links, contact details and money requests.
 * These are the same three rules as `messageWarnings` in `backend/src/domain.js`. Flagged text is delivered with the
 * warnings attached so the receiver can be careful. It is never silently altered.
 */
object MessageSafety {
    private val link = Regex("https?://|www\\.", RegexOption.IGNORE_CASE)
    private val personal = Regex("(?:\\+?\\d[\\s-]?){8,}|\\S+@\\S+\\.\\S+")
    private val money = Regex(
        "\\b(send money|bank transfer|gift card|crypto|loan|wire transfer)\\b", RegexOption.IGNORE_CASE,
    )

    fun warnings(text: String): List<SafetyWarning> = buildList {
        if (link.containsMatchIn(text)) add(SafetyWarning.LINK)
        if (personal.containsMatchIn(text)) add(SafetyWarning.PERSONAL_INFORMATION)
        if (money.containsMatchIn(text)) add(SafetyWarning.MONEY_REQUEST)
    }
}

/** Rules for the note that can go with a like. */
object Compliments {
    const val MAX_LENGTH = 140

    /** The trimmed note if it is 1 to [MAX_LENGTH] characters, otherwise null. An empty note means "no note". */
    fun clean(note: String): String? = note.trim().takeIf { it.length in 1..MAX_LENGTH }
}
