package com.centralia.app.domain.library

import com.centralia.app.domain.UuidSerializer
import java.util.UUID
import kotlinx.serialization.Serializable

/** `LibraryFolder`. */
@Serializable
data class LibraryFolder(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val name: String,
    val symbolName: String
)
