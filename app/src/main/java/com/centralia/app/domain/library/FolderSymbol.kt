package com.centralia.app.domain.library


enum class FolderSymbol(val rawValue: String, val accessibilityName: String) {
    FOLDER("folder", "Folder"),
    HOUSE("house", "Home"),
    PAINT_PALETTE("paintpalette", "Design"),
    FORK_AND_KNIFE("fork.knife", "Food"),
    BOOKS("books.vertical", "Books"),
    LIGHTBULB("lightbulb", "Ideas"),
    BRIEFCASE("briefcase", "Work"),
    HEART("heart", "Favorites"),
    GRADUATION_CAP("graduationcap", "Learning"),
    CAMERA("camera", "Photography"),
    MUSIC_NOTE("music.note", "Music"),
    SUN("sun.max", "Wellbeing");

    companion object {

        fun fromRawValue(rawValue: String?): FolderSymbol =
            entries.firstOrNull { it.rawValue == rawValue } ?: FOLDER
    }
}
