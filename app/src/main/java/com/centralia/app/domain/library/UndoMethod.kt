package com.centralia.app.domain.library

/**
 * How the user undid a delete. Sent to the backend as `X-Undo-Method` so the
 * Sprint 3 business question (shake vs. button) can be answered. Mirrors
 * `UndoMethod` in the iOS app.
 */
enum class UndoMethod(val wireValue: String) {
    SHAKE("shake"),
    BUTTON("button")
}
