package com.centralia.app.domain.smart

import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.LibraryFolder
import java.util.UUID


data class SmartFolderSuggestion(
    val folderName: String,
    val existingFolderID: UUID?,
    val symbol: FolderSymbol
) {
    val needsNewFolder: Boolean get() = existingFolderID == null

    val actionTitle: String
        get() = if (needsNewFolder) "Create “$folderName”" else "Use “$folderName”"

    val message: String
        get() = if (needsNewFolder) {
            "This short looks like $folderName. Create the folder and save it there?"
        } else {
            "This short looks like $folderName."
        }

    companion object {
        const val TITLE = "Smart suggestion"


        fun make(
            suggestedFolderName: String?,
            folders: List<LibraryFolder>,
            selectedFolderID: UUID?
        ): SmartFolderSuggestion? {
            val name = suggestedFolderName?.trim().orEmpty()
            if (name.isEmpty()) return null
            val existing = folders.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (existing != null && existing.id == selectedFolderID) return null
            return SmartFolderSuggestion(
                folderName = existing?.name ?: name,
                existingFolderID = existing?.id,
                symbol = symbolFor(name)
            )
        }


        fun symbolFor(folderName: String): FolderSymbol =
            when (folderName.trim().lowercase()) {
                "recipes", "food" -> FolderSymbol.FORK_AND_KNIFE
                "fitness", "comedy" -> FolderSymbol.HEART
                "wellbeing" -> FolderSymbol.SUN
                "design" -> FolderSymbol.PAINT_PALETTE
                "spaces" -> FolderSymbol.HOUSE
                "tech" -> FolderSymbol.LIGHTBULB
                "travel" -> FolderSymbol.CAMERA
                "music" -> FolderSymbol.MUSIC_NOTE
                "learning" -> FolderSymbol.GRADUATION_CAP
                else -> FolderSymbol.FOLDER
            }
    }
}
