package com.centralia.app.app

import com.centralia.app.domain.library.DeletedVideo
import com.centralia.app.domain.library.VideoDeletion
import com.centralia.app.domain.library.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class LibraryRevisions {
    private val _library = MutableStateFlow(0)
    private val _folders = MutableStateFlow(0)
    private val _deletedVideo = MutableStateFlow<DeletedVideo?>(null)


    val library: StateFlow<Int> = _library.asStateFlow()


    val folders: StateFlow<Int> = _folders.asStateFlow()


    /** A short deleted on the detail screen, with the backend's undo pop-up. */
    val deletedVideo: StateFlow<DeletedVideo?> = _deletedVideo.asStateFlow()


    fun libraryChanged() {
        _library.value += 1
    }


    fun foldersChanged() {
        _folders.value += 1
    }


    fun videoSaved() {
        _library.value += 1
        _folders.value += 1
    }


    fun videoChanged() {
        _library.value += 1
        _folders.value += 1
    }


    fun videoDeleted(video: VideoItem, deletion: VideoDeletion) {
        _deletedVideo.value = DeletedVideo(video, deletion)
        _library.value += 1
        _folders.value += 1
    }


    fun refreshAll() {
        _library.value += 1
        _folders.value += 1
    }

    fun consumeDeletedVideo() {
        _deletedVideo.value = null
    }
}
