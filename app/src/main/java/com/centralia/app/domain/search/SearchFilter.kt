package com.centralia.app.domain.search

import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.TextMatch
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import java.util.Locale
import java.util.UUID

/** `enum SearchFolderFilter`. */
sealed interface SearchFolderFilter {
    data object All : SearchFolderFilter
    data object Unorganized : SearchFolderFilter
    data class InFolder(val folderID: UUID) : SearchFolderFilter
}

/** Everything a search result depends on. Two equal inputs give equal results. */
data class SearchInput(
    val videos: List<VideoItem>,
    val folders: List<LibraryFolder>,
    val query: String,
    val platform: VideoPlatform?,
    val creator: String?,
    val folder: SearchFolderFilter,
    val tags: Set<String>
)

/**
 * The text of each short, already folded (lower case, no accents), built once
 * per library.
 *
 * Micro-optimization (Sprint 4): before, every keystroke folded the title,
 * creator, folder name and every tag of every short again, once per search
 * term (Unicode NFD + a regex per string), and built a list per short. Now
 * that work happens once when the library changes; a keystroke only folds the
 * query and runs `String.contains` on the prepared strings.
 */
class SearchIndex private constructor(
    val videos: List<VideoItem>,
    val folders: List<LibraryFolder>,
    private val haystacks: Array<Array<String>>,
    private val lowerTags: Array<Set<String>>
) {
    fun matchesText(index: Int, foldedTerms: List<String>): Boolean {
        val fields = haystacks[index]
        for (term in foldedTerms) {
            var found = false
            for (field in fields) if (field.contains(term)) { found = true; break }
            if (!found) return false
        }
        return true
    }

    fun hasAllTags(index: Int, lowerSelected: Collection<String>): Boolean =
        lowerTags[index].containsAll(lowerSelected)

    companion object {
        fun build(videos: List<VideoItem>, folders: List<LibraryFolder>): SearchIndex {
            val folderNames = HashMap<UUID, String>(folders.size * 2)
            for (folder in folders) folderNames[folder.id] = TextMatch.fold(folder.name)
            val haystacks = Array(videos.size) { i ->
                val video = videos[i]
                val fields = ArrayList<String>(3 + video.tags.size)
                fields.add(TextMatch.fold(video.displayTitle))
                fields.add(TextMatch.fold(video.creator))
                video.folderID?.let { id -> folderNames[id]?.let(fields::add) }
                for (tag in video.tags) fields.add(TextMatch.fold(tag))
                fields.toTypedArray()
            }
            val lowerTags = Array<Set<String>>(videos.size) { i ->
                videos[i].tags.mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
            }
            return SearchIndex(videos, folders, haystacks, lowerTags)
        }
    }
}

/**
 * The search itself, as a pure function. `SearchViewModel` runs it on
 * `Dispatchers.Default` (Sprint 3, Alex · multithreading with Flow).
 *
 * Every active facet must pass, then each whitespace-separated term must
 * appear in the title, creator, folder name or a tag (case- and
 * accent-insensitive, like iOS).
 */
object SearchFilter {
    private val WHITESPACE = Regex("\\s+")

    @Volatile
    private var cachedIndex: SearchIndex? = null

    /** The index for this library: reused while the same lists are searched. */
    fun index(videos: List<VideoItem>, folders: List<LibraryFolder>): SearchIndex {
        val cached = cachedIndex
        if (cached != null && cached.videos === videos && cached.folders === folders) return cached
        return SearchIndex.build(videos, folders).also { cachedIndex = it }
    }

    fun apply(input: SearchInput): List<VideoItem> {
        val terms = input.query.trim().split(WHITESPACE).filter { it.isNotEmpty() }.map(TextMatch::fold)
        val selectedTags = input.tags.map { it.lowercase(Locale.ROOT) }
        val index = index(input.videos, input.folders)

        val results = ArrayList<VideoItem>()
        input.videos.forEachIndexed { i, video ->
            if (input.platform != null && video.platform != input.platform) return@forEachIndexed
            if (input.creator != null && video.creator != input.creator) return@forEachIndexed

            when (val folderFilter = input.folder) {
                SearchFolderFilter.All -> Unit
                SearchFolderFilter.Unorganized -> if (video.folderID != null) return@forEachIndexed
                is SearchFolderFilter.InFolder ->
                    if (video.folderID != folderFilter.folderID) return@forEachIndexed
            }

            if (selectedTags.isNotEmpty() && !index.hasAllTags(i, selectedTags)) return@forEachIndexed
            if (terms.isEmpty() || index.matchesText(i, terms)) results.add(video)
        }
        return results
    }
}
