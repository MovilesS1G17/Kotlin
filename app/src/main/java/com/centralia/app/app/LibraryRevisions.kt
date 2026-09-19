package com.centralia.app.app

import com.centralia.app.domain.library.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * `AuthenticatedAppView` keeps `libraryRevision` / `foldersRevision` counters and
 * stamps them onto its tabs with `.id(…)`, rebuilding a subtree when another tab
 * changed the data. The detail screens additionally report edits and deletions
 * straight to their parent through the `videoChanged` / `videoDeleted` closures.
 *
 * Neither mechanism survives the move to Compose Navigation: throwing away a
 * composition would lose scroll position and the `ViewModel`s that outlive
 * rotation, and a pushed destination cannot hold a closure back to a screen that
 * may be recreated. Both signals therefore travel through this one object,
 * which the container owns for the session. The trigger points and the resulting
 * refreshes match the original.
 */
class LibraryRevisions {
    private val _library = MutableStateFlow(0)
    private val _folders = MutableStateFlow(0)
    private val _deletedVideo = MutableStateFlow<VideoItem?>(null)

    /** Watched by the Library and Search screens. */
    val library: StateFlow<Int> = _library.asStateFlow()

    /** Watched by the Folders screen. */
    val folders: StateFlow<Int> = _folders.asStateFlow()

    /**
     * Set when a video is deleted from a pushed destination. The list screen that
     * comes back into view claims it, shows the undo toast, then clears it — the
     * effect the `videoDeleted` closure had on iOS.
     */
    val deletedVideo: StateFlow<VideoItem?> = _deletedVideo.asStateFlow()

    /** `libraryChanged` from the Folders tab and Smart Organization. */
    fun libraryChanged() {
        _library.value += 1
    }

    /** The Folders tab's own collection changed (rename, delete, new folder). */
    fun foldersChanged() {
        _folders.value += 1
    }

    /** A save touches both collections, as the sheet's completion did. */
    fun videoSaved() {
        _library.value += 1
        _folders.value += 1
    }

    /** `videoChanged(updatedVideo)` — an edit on a detail screen. */
    fun videoChanged() {
        _library.value += 1
        _folders.value += 1
    }

    /** `videoDeleted(deletedVideo)`. */
    fun videoDeleted(video: VideoItem) {
        _deletedVideo.value = video
        _library.value += 1
        _folders.value += 1
    }

    fun consumeDeletedVideo() {
        _deletedVideo.value = null
    }
}
