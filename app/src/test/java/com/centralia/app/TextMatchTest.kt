package com.centralia.app

import com.centralia.app.domain.library.TextMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class TextMatchTest {
    @Test
    fun typedTextIgnoresCaseAndAccents() {
        assertTrue(TextMatch.contains("Café con leche", "cafe"))
        assertTrue(TextMatch.contains("Diseño gráfico", "DISENO"))
        assertTrue(TextMatch.contains("cafe", "Café"))
        assertTrue(TextMatch.contains("Pasta in 20 minutes", "20 min"))
        assertFalse(TextMatch.contains("Pasta", "pizza"))
    }

    @Test
    fun tagsMatchWhateverTheirCase() {
        assertTrue(TextMatch.sameTag("Pasta", "pasta"))
        assertFalse(TextMatch.sameTag("pasta", "pastas"))
        assertTrue(TextMatch.hasAllTags(listOf("Pasta", "dinner"), setOf("pasta", "DINNER")))
        assertFalse(TextMatch.hasAllTags(listOf("pasta"), setOf("pasta", "dinner")))
        assertTrue(TextMatch.hasAllTags(listOf("pasta"), emptySet()))
    }

    @Test
    fun oneChipPerTag() {
        assertEquals(
            listOf("dinner", "Pasta", "quick"),
            TextMatch.uniqueTags(listOf("Pasta", "quick", "pasta", " dinner ", "", "PASTA"))
        )
    }

    @Test
    fun selectionFollowsTheChipSpelling() {
        assertEquals(setOf("pasta"), TextMatch.reconcile(setOf("Pasta", "gone"), listOf("dinner", "pasta")))
    }
}
