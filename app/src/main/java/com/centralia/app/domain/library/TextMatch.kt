package com.centralia.app.domain.library

import java.text.Collator
import java.text.Normalizer
import java.util.Locale


object TextMatch {
    private val combiningMarks = Regex("\\p{Mn}+")


    fun fold(value: String): String =
        combiningMarks.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)


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
