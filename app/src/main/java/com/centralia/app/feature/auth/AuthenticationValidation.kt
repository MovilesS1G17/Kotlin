package com.centralia.app.feature.auth

import java.util.Locale


object AuthenticationValidation {

    const val WEAK_PASSWORD = "Use at least 8 characters, with letters and numbers."
    const val COMMON_PASSWORD = "This password is too common. Choose another one."
    const val PASSWORD_HINT = "8+ characters, letters and numbers"

    /** The most used passwords that would otherwise pass the rules. */
    val COMMON_PASSWORDS = setOf(
        "password1", "password12", "password123", "password1234", "passw0rd", "qwerty123", "qwerty1234",
        "abc12345", "abcd1234", "1q2w3e4r", "1qaz2wsx", "iloveyou1", "admin123", "welcome1", "welcome123",
        "letmein1", "monkey123", "dragon123", "football1", "baseball1", "sunshine1", "princess1", "123456789a",
        "a123456789", "contraseña1", "contrasena1", "contrasena123", "centralia1", "centralia123"
    )

    fun normalizedEmail(email: String): String = email.trim().lowercase(Locale.ROOT)


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


    fun passwordError(password: String): String? = when {
        password.length < 8 || password.length > 128 ||
            password.none { it.isLetter() } || password.none { it.isDigit() } -> WEAK_PASSWORD
        password.lowercase(Locale.ROOT) in COMMON_PASSWORDS -> COMMON_PASSWORD
        else -> null
    }


    fun normalizedCode(code: String): String = code.filter { it.isDigit() }.take(6)

    fun codeError(code: String): String? =
        if (normalizedCode(code).length == 6) null else "Enter the 6-digit code from the email."
}
