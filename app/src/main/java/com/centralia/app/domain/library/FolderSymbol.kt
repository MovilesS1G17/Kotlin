package com.centralia.app.domain.library

/**
 * `FolderSymbol`. The raw values stay as the original SF Symbol names because
 * they are what gets written into the folder store; [folderSymbolIcon] resolves
 * them to Material icons at render time.
 */
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
        /** `FolderSymbol(rawValue:) ?? .folder`. */
        fun fromRawValue(rawValue: String?): FolderSymbol =
            entries.firstOrNull { it.rawValue == rawValue } ?: FOLDER
    }
}
