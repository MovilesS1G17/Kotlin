package com.centralia.app.feature.auth

import java.util.Locale

/** `enum AuthenticationValidation` — the shared form rules, unchanged. */
object AuthenticationValidation {

    fun normalizedEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    /**
     * Requires exactly one `@`, a non-empty local part, and a domain that
     * contains a dot but neither starts nor ends with one.
     *
     * Swift's `split(separator:omittingEmptySubsequences: false)` keeps empty
     * pieces, so `split("@", -1)` is used here to match — Kotlin's default
     * `split` already keeps them, but the limit is spelled out for clarity.
     */
    fun emailError(email: String): String? {
        val normalized = normalizedEmail(email)
        val components = normalized.split("@")

        val isValid = components.size == 2 &&
            components[0].isNotEmpty() &&
            components[1].contains(".") &&
            !components[1].startsWith(".") &&
            !components[1].endsWith(".")

        return if (isValid) null else "Enter a valid email address."
    }

    fun passwordError(password: String): String? =
        if (password.length >= 8) null else "Use at least 8 characters."
}
