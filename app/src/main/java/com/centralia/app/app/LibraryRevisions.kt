package com.centralia.app.app

import com.centralia.app.domain.library.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class LibraryRevisions {
    private val _library = MutableStateFlow(0)
    private val _folders = MutableStateFlow(0)
    private val _deletedVideo = MutableStateFlow<VideoItem?>(null)


    val library: StateFlow<Int> = _library.asStateFlow()


    val folders: StateFlow<Int> = _folders.asStateFlow()


    val deletedVideo: StateFlow<VideoItem?> = _deletedVideo.asStateFlow()


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


    fun videoDeleted(video: VideoItem) {
        _deletedVideo.value = video
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
