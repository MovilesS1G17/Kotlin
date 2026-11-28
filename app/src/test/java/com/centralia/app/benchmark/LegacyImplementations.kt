package com.centralia.app.benchmark

import com.centralia.app.domain.search.SearchInput
import com.centralia.app.domain.search.SearchFolderFilter
import com.centralia.app.domain.library.VideoItem
import java.text.Normalizer
import java.util.Locale

// The "before" versions of the code changed by the Sprint 4 micro-optimizations,
// kept only in the test sources so the benchmark can compare them.

/** TextMatch as it was before Sprint 4 (fold always runs NFD + regex). */
object LegacyTextMatch {
    private val combiningMarks = Regex("\\p{Mn}+")
    fun fold(value: String): String =
        combiningMarks.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)
    fun contains(value: String, term: String): Boolean = fold(value).contains(fold(term))
    fun sameTag(a: String, b: String): Boolean = a.lowercase(Locale.ROOT) == b.lowercase(Locale.ROOT)
    fun hasAllTags(tags: List<String>, selected: Collection<String>): Boolean =
        selected.all { wanted -> tags.any { sameTag(it, wanted) } }
}

/** SearchFilter as it was before Sprint 4 (folds every field on every keystroke). */
object LegacySearchFilter {
    fun apply(input: SearchInput): List<VideoItem> {
        val terms = input.query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val folderNames = input.folders.associate { it.id to it.name }

        return input.videos.filter { video ->
            if (input.platform != null && video.platform != input.platform) return@filter false
            if (input.creator != null && video.creator != input.creator) return@filter false

            when (val folderFilter = input.folder) {
                SearchFolderFilter.All -> Unit
                SearchFolderFilter.Unorganized -> if (video.folderID != null) return@filter false
                is SearchFolderFilter.InFolder ->
                    if (video.folderID != folderFilter.folderID) return@filter false
            }

            if (!LegacyTextMatch.hasAllTags(video.tags, input.tags)) return@filter false
            if (terms.isEmpty()) return@filter true

            val searchable = listOf(
                video.displayTitle,
                video.creator,
                video.folderID?.let { folderNames[it] } ?: ""
            ) + video.tags

            terms.all { term -> searchable.any { LegacyTextMatch.contains(it, term) } }
        }
    }
}
