package com.centralia.app.domain.imports

/**
 * What the Save screen shows when a pasted link can't be saved
 * (TA feedback: "when saving an inadmissible link, make it more telling").
 *
 * The API explains every rejected link in two parts: a short [title] that says
 * what is wrong ("This TikTok link isn't a video") and a [message] that says how
 * to get a link that works. Older servers send no title, and some failures
 * never reach the server at all, so this picks a sensible title for those too.
 *
 * Plain Kotlin (no Android types) so it can be unit tested on the JVM.
 */
data class LinkProblem(val title: String, val message: String) {
    companion object {
        const val DEFAULT_TITLE = "This link can't be saved"
        const val OFFLINE_TITLE = "Can't check this link right now"
        const val OFFLINE_MESSAGE =
            "You're offline. Checking a link needs a connection. Try again when you're back online."

        /**
         * @param code the API error code, or "unreachable" when the request
         *   never reached the server.
         * @param title the API's own title, when it sent one.
         * @param detail the API's explanation.
         */
        fun from(code: String?, title: String?, detail: String?): LinkProblem {
            if (code == "unreachable" || code == "offline") {
                return LinkProblem(OFFLINE_TITLE, OFFLINE_MESSAGE)
            }
            val fallbackTitle = when (code) {
                "invalid_url" -> "That isn't a complete link"
                "unsupported_youtube_video" -> "This is a regular YouTube video"
                "unsupported_instagram_post" -> "This is an Instagram post, not a Reel"
                "duplicate_video" -> "Already in your library"
                else -> DEFAULT_TITLE
            }
            val message = detail?.takeIf { it.isNotBlank() }
                ?: "Centralia saves TikTok videos, Instagram Reels and YouTube Shorts. " +
                "Open the short in its app, tap Share → Copy link, and paste it here."
            return LinkProblem(title?.takeIf { it.isNotBlank() } ?: fallbackTitle, message)
        }
    }
}
