package com.centralia.app

import com.centralia.app.domain.auth.AuthenticatedUser
import com.centralia.app.domain.library.ContentAnalysisStatus
import com.centralia.app.domain.library.FolderSymbol
import com.centralia.app.domain.library.VideoItem
import com.centralia.app.domain.library.VideoPlatform
import com.centralia.app.domain.profile.MembershipStatus
import com.centralia.app.domain.profile.ProfileStorageUsage
import com.centralia.app.domain.profile.UserProfile
import com.centralia.app.feature.auth.AuthenticationValidation
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Checks the pieces of the port whose behaviour is easy to get subtly wrong:
 * string formatting, fallback chains, and validation rules. Each expectation is
 * what the Swift original produces.
 */
class TranslationParityTest {

    private fun video(
        customTitle: String? = null,
        generatedSummary: String? = null,
        sourceCaption: String? = null,
        creator: String = "@creator",
        durationSeconds: Int = 0
    ) = VideoItem(
        id = UUID.randomUUID(),
        sourceURL = "https://www.tiktok.com/@creator/video/1",
        platform = VideoPlatform.TIKTOK,
        creator = creator,
        durationSeconds = durationSeconds,
        sourceCaption = sourceCaption,
        transcript = null,
        extractedOnScreenText = null,
        generatedSummary = generatedSummary,
        customTitle = customTitle,
        folderID = null,
        tags = emptyList(),
        note = null,
        savedAt = Instant.EPOCH,
        analysisStatus = ContentAnalysisStatus.COMPLETED
    )

    @Test
    fun `displayTitle prefers custom title then summary then caption then creator`() {
        assertEquals(
            "Custom",
            video(customTitle = "Custom", generatedSummary = "Summary", sourceCaption = "Caption")
                .displayTitle
        )
        assertEquals(
            "Summary",
            video(generatedSummary = "Summary", sourceCaption = "Caption").displayTitle
        )
        assertEquals("Caption", video(sourceCaption = "Caption").displayTitle)
        assertEquals("Short by @roomreset", video(creator = "@roomreset").displayTitle)
    }

    @Test
    fun `displayTitle treats whitespace-only values as absent`() {
        assertEquals(
            "Summary",
            video(customTitle = "   ", generatedSummary = "Summary").displayTitle
        )
    }

    @Test
    fun `formattedDuration pads seconds to two digits`() {
        assertEquals("0:28", video(durationSeconds = 28).formattedDuration)
        assertEquals("0:54", video(durationSeconds = 54).formattedDuration)
        assertEquals("1:05", video(durationSeconds = 65).formattedDuration)
        assertEquals("10:00", video(durationSeconds = 600).formattedDuration)
    }

    @Test
    fun `email validation matches the Swift rules`() {
        assertNull(AuthenticationValidation.emailError("you@example.com"))
        assertNull(AuthenticationValidation.emailError("  YOU@Example.COM  "))

        // No domain dot, leading/trailing dot, empty local part, wrong @ count.
        assertNotNull(AuthenticationValidation.emailError("you@example"))
        assertNotNull(AuthenticationValidation.emailError("you@.example.com"))
        assertNotNull(AuthenticationValidation.emailError("you@example.com."))
        assertNotNull(AuthenticationValidation.emailError("@example.com"))
        assertNotNull(AuthenticationValidation.emailError("you@@example.com"))
        assertNotNull(AuthenticationValidation.emailError("you.example.com"))
    }

    @Test
    fun `normalizedEmail trims and lowercases`() {
        assertEquals("you@example.com", AuthenticationValidation.normalizedEmail(" YOU@Example.com "))
    }

    @Test
    fun `password must be at least eight characters`() {
        assertNotNull(AuthenticationValidation.passwordError("1234567"))
        assertNull(AuthenticationValidation.passwordError("12345678"))
    }

    @Test
    fun `profile initials take the first letter of up to two words`() {
        fun profile(name: String) = UserProfile(
            id = UUID.randomUUID(),
            displayName = name,
            email = "a@b.com",
            membershipStatus = MembershipStatus.CENTRALIA_MEMBER
        )

        assertEquals("DC", profile("David Caro").initials)
        assertEquals("DC", profile("David Caro Perez").initials)
        assertEquals("D", profile("David").initials)
        assertEquals("C", profile("   ").initials)
    }

    @Test
    fun `storage summary formats one fraction digit and a whole capacity`() {
        assertEquals("2.4 GB of 5 GB (48%)", ProfileStorageUsage.mock.summary)
        assertEquals(48, ProfileStorageUsage.mock.percentage)
    }

    @Test
    fun `storage fraction is clamped and safe at zero capacity`() {
        assertEquals(0.0, ProfileStorageUsage(1.0, 0.0).fractionUsed, 0.0001)
        assertEquals(1.0, ProfileStorageUsage(9.0, 5.0).fractionUsed, 0.0001)
    }

    @Test
    fun `folder symbol raw values survive a round trip and fall back to folder`() {
        FolderSymbol.entries.forEach { symbol ->
            assertEquals(symbol, FolderSymbol.fromRawValue(symbol.rawValue))
        }
        assertEquals(FolderSymbol.FOLDER, FolderSymbol.fromRawValue("not.a.symbol"))
        assertEquals(FolderSymbol.FOLDER, FolderSymbol.fromRawValue(null))
        // The persisted names are SF Symbol strings shared with the iOS store.
        assertEquals("sun.max", FolderSymbol.SUN.rawValue)
        assertEquals("fork.knife", FolderSymbol.FORK_AND_KNIFE.rawValue)
    }

    @Test
    fun `platform filter names match the library chips`() {
        assertEquals("TikTok", VideoPlatform.TIKTOK.filterName)
        assertEquals("Reels", VideoPlatform.INSTAGRAM_REEL.filterName)
        assertEquals("Shorts", VideoPlatform.YOUTUBE_SHORT.filterName)
        assertEquals("Instagram Reel", VideoPlatform.INSTAGRAM_REEL.displayName)
    }

    @Test
    fun `authenticated user carries the profile identity`() {
        val id = UUID.randomUUID()
        val profile = UserProfile(
            id = id,
            displayName = "David Caro",
            email = "david@example.com",
            membershipStatus = MembershipStatus.CENTRALIA_MEMBER
        )
        assertEquals(
            AuthenticatedUser(id = id, displayName = "David Caro", email = "david@example.com"),
            profile.authenticatedUser
        )
    }
}
