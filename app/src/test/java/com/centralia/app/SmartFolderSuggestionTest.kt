package com.centralia.app

import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.smart.SmartFolderSuggestion
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class SmartFolderSuggestionTest {
    private val recipes = LibraryFolder(UUID.randomUUID(), "Recipes", "fork.knife")

    @Test
    fun offersToCreateAMissingFolderWithAFittingIcon() {
        val suggestion = SmartFolderSuggestion.make("Fitness", listOf(recipes), null)!!
        assertTrue(suggestion.needsNewFolder)
        assertEquals(FolderSymbol.HEART, suggestion.symbol)
        assertEquals("Create “Fitness”", suggestion.actionTitle)
    }

    @Test
    fun offersAnExistingFolderCaseInsensitively() {
        val suggestion = SmartFolderSuggestion.make("recipes", listOf(recipes), null)!!
        assertEquals(recipes.id, suggestion.existingFolderID)
        assertEquals("Use “Recipes”", suggestion.actionTitle)
    }

    @Test
    fun staysQuietWhenAlreadySelectedOrNothingToSuggest() {
        assertNull(SmartFolderSuggestion.make("Recipes", listOf(recipes), recipes.id))
        assertNull(SmartFolderSuggestion.make(null, listOf(recipes), null))
        assertNull(SmartFolderSuggestion.make("  ", emptyList(), null))
    }

    @Test
    fun unknownNamesGetThePlainFolderIcon() {
        assertEquals(FolderSymbol.FOLDER, SmartFolderSuggestion.symbolFor("Inspiration"))
        assertEquals(FolderSymbol.FORK_AND_KNIFE, SmartFolderSuggestion.symbolFor("Recipes"))
    }
}
