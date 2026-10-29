package com.example.runway.domain.validation

enum class EmailIssue { Empty, Invalid }
enum class PasswordIssue { Empty, TooShort }
enum class NameIssue { Empty }

/**
 * Pure Kotlin, no Android. Returns null when the input is fine, otherwise the problem.
 * The UI maps each issue to a string, so this stays testable and language-free.
 */
object CredentialValidator {
    const val MIN_PASSWORD_LENGTH = 6 // same limit as Firebase Auth

    private val emailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateEmail(input: String): EmailIssue? {
        val email = input.trim()
        return when {
            email.isEmpty() -> EmailIssue.Empty
            !emailShape.matches(email) -> EmailIssue.Invalid
            else -> null
        }
    }

    /** Sign-in only checks the field is filled, so an older account is never blocked by our form. */
    fun validateSignInPassword(input: String): PasswordIssue? =
        if (input.isEmpty()) PasswordIssue.Empty else null

    /** New passwords: at least [MIN_PASSWORD_LENGTH] characters. Not trimmed: spaces are allowed. */
    fun validateNewPassword(input: String): PasswordIssue? = when {
        input.isEmpty() -> PasswordIssue.Empty
        input.length < MIN_PASSWORD_LENGTH -> PasswordIssue.TooShort
        else -> null
    }

    fun validateName(input: String): NameIssue? =
        if (input.isBlank()) NameIssue.Empty else null
}
