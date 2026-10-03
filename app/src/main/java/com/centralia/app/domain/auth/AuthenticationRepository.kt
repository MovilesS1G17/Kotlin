package com.centralia.app.domain.auth


interface AuthenticationRepository {

    suspend fun createAccount(email: String, password: String): VerificationChallenge

    /**
     * logea y hace Throws [AuthenticationException.EmailNotVerified]
     */
    suspend fun logIn(email: String, password: String): AuthenticatedUser


    suspend fun verifyEmail(email: String, code: String): AuthenticatedUser


    suspend fun resendVerificationCode(email: String): VerificationChallenge


    suspend fun requestPasswordReset(email: String)


    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthenticatedUser

    suspend fun signOut()


    suspend fun restoreSession(): AuthenticatedUser? = null
}
