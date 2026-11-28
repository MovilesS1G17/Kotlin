package com.centralia.app.benchmark

import com.centralia.app.domain.library.ContentAnalysisStatus
import com.centralia.app.domain.library.LibraryFolder
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.search.SearchFilter
import com.centralia.app.domain.search.SearchFolderFilter
import com.centralia.app.domain.search.SearchInput
import com.centralia.app.domain.search.SearchIndex
import java.time.Instant
import java.util.UUID
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Before/after numbers for the Sprint 4 micro-optimizations (wiki, point 3).
 * Runs on the JVM: `./gradlew testDebugUnitTest --tests "*MicroOptimizationBenchmark*" -i`
 * prints the table. The Android Studio Profiler captures (CPU / System Trace,
 * section "search_filter") show the same change on the phone.
 */
class MicroOptimizationBenchmark {

    private val folders = listOf("Recipes", "Gym", "Travel", "Design", "Tech", "Café").map {
        LibraryFolder(UUID.randomUUID(), it, "folder")
    }
    private val words = listOf("pasta", "workout", "beach", "typography", "swiftui", "morning", "routine",
        "crème", "brûlée", "jalapeño", "interiors", "tips", "budget", "camera", "street", "food")

    private fun library(size: Int, random: Random = Random(7)) = List(size) {
        VideoItem(
            id = UUID(random.nextLong(), random.nextLong()),
            sourceURL = "https://www.tiktok.com/@x/video/$it",
            platform = VideoPlatform.entries[random.nextInt(3)],
            creator = "@creator${random.nextInt(60)}",
            durationSeconds = 30,
            sourceCaption = List(6) { words[random.nextInt(words.size)] }.joinToString(" "),
            folderID = if (random.nextBoolean()) folders[random.nextInt(folders.size)].id else null,
            tags = List(random.nextInt(4)) { words[random.nextInt(words.size)] },
            savedAt = Instant.parse("2026-11-01T00:00:00Z"),
            analysisStatus = ContentAnalysisStatus.COMPLETED
        )
    }

    /** Median ns per call over [rounds] rounds of [calls] calls, after a warm-up. */
    private inline fun measure(rounds: Int = 15, calls: Int, block: () -> Unit): Double {
        repeat(calls * 3) { block() }
        val times = DoubleArray(rounds) {
            val start = System.nanoTime()
            repeat(calls) { block() }
            (System.nanoTime() - start).toDouble() / calls
        }
        times.sort()
        return times[rounds / 2]
    }

    private fun row(name: String, before: Double, after: Double, unit: String = "µs"): String {
        val scale = if (unit == "µs") 1_000.0 else 1.0
        return "| %-38s | %10.1f %s | %10.1f %s | %5.1fx |".format(name, before / scale, unit, after / scale, unit, before / after)
    }

    @Test
    fun searchKeystroke() {
        val lines = mutableListOf("| Case | Before | After | Speed-up |", "|---|---|---|---|")
        for (size in listOf(200, 1000)) {
            val videos = library(size)
            // Typing "pasta bea" one key at a time, like the debounced search pipeline.
            val queries = "pasta bea".indices.map { "pasta bea".substring(0, it + 1) }
            val inputs = queries.map { SearchInput(videos, folders, it, null, null, SearchFolderFilter.All, emptySet()) }
            for (input in inputs) assertEquals(LegacySearchFilter.apply(input), SearchFilter.apply(input))
            val before = measure(calls = 5) { inputs.forEach { LegacySearchFilter.apply(it) } } / inputs.size
            val after = measure(calls = 5) { inputs.forEach { SearchFilter.apply(it) } } / inputs.size
            lines += row("Search, 1 keystroke, $size shorts", before, after)
        }
        val tagged = library(1000)
        val tagInput = SearchInput(tagged, folders, "", null, null, SearchFolderFilter.All, setOf("pasta", "beach"))
        assertEquals(LegacySearchFilter.apply(tagInput), SearchFilter.apply(tagInput))
        lines += row(
            "Tag filter (2 tags), 1000 shorts",
            measure(calls = 20) { LegacySearchFilter.apply(tagInput) },
            measure(calls = 20) { SearchFilter.apply(tagInput) }
        )
        // The one-off cost moved out of the keystroke: building the index when the library changes.
        val build = measure(calls = 5) { SearchIndex.build(tagged, folders) }
        lines += "| Index build, once per library change (1000 shorts) | — | %.1f µs | — |".format(build / 1_000)
        println(lines.joinToString("\n"))
    }
}
