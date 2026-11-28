package com.centralia.app.domain.library

/**
 * One rule for tags, shared by every screen that edits them and by the API
 * (`MAX_TAG_LENGTH` in `app/schemas.py`).
 *
 * TA feedback: a very long tag pushed the screen's content out of bounds. Tags
 * are now capped at [MAX_LENGTH] characters while typing, and every chip that
 * shows a tag keeps it on one line with an ellipsis.
 *
 * Plain Kotlin so it can be unit tested on the JVM.
 */
object TagRules {
    const val MAX_LENGTH = 30

    /** Trims and collapses inner spaces, the same way the server does. */
    fun clean(raw: String): String = raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

    /** What the text field keeps while the user types: never more than [MAX_LENGTH] characters. */
    fun limitInput(raw: String): String = if (raw.length <= MAX_LENGTH) raw else raw.take(MAX_LENGTH)

    /** The "12/30" counter under a tag field. */
    fun counter(raw: String): String = "${clean(raw).length}/$MAX_LENGTH"

    /** A tag that can be added: not blank, within the limit. */
    fun isValid(raw: String): Boolean = clean(raw).let { it.isNotEmpty() && it.length <= MAX_LENGTH }

    /**
     * Adds [raw] to [tags] if it's valid and not already there (ignoring case).
     * Used both by the "Add" button and by "Done"/"Save", so a tag typed but not
     * yet added is never lost (TA feedback: "Done should save what you just did").
     */
    fun addPending(tags: List<String>, raw: String): List<String> {
        val tag = clean(raw)
        if (!isValid(tag) || tags.any { it.equals(tag, ignoreCase = true) }) return tags
        return tags + tag
    }
}
