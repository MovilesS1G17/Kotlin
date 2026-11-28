package com.centralia.app.domain.library

import java.text.Collator
import java.text.Normalizer
import java.util.Locale


object TextMatch {
    private val combiningMarks = Regex("\\p{Mn}+")


    /**
     * Case- and accent-insensitive form of [value].
     *
     * Micro-optimization (Sprint 4): almost every title, creator and tag is
     * plain ASCII, which has no accents to remove, so it skips the Unicode
     * decomposition (NFD) and the regex and only lowercases. Accented text
     * still goes the full way, so the result is identical.
     */
    fun fold(value: String): String {
        if (isAscii(value)) return value.lowercase(Locale.ROOT)
        return combiningMarks.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)
    }

    private fun isAscii(value: String): Boolean {
        for (i in value.indices) if (value[i].code >= 0x80) return false
        return true
    }


    fun contains(value: String, term: String): Boolean = fold(value).contains(fold(term))

    fun sameTag(a: String, b: String): Boolean = a.lowercase(Locale.ROOT) == b.lowercase(Locale.ROOT)


    fun hasAllTags(tags: List<String>, selected: Collection<String>): Boolean =
        selected.all { wanted -> tags.any { sameTag(it, wanted) } }


    fun uniqueTags(tags: List<String>): List<String> =
        tags.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(alphabetical())


    private fun alphabetical(): Comparator<String> {
        val collator = Collator.getInstance().apply { strength = Collator.SECONDARY }
        return Comparator { a, b -> collator.compare(a, b) }
    }


    fun reconcile(selected: Set<String>, available: List<String>): Set<String> =
        available.filter { offered -> selected.any { sameTag(it, offered) } }.toSet()
}
