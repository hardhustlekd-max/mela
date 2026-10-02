package com.mela.ussdrunner.domain

object UssdValidator {
    private val allowed = Regex("^[0-9*#]+$")

    fun normalize(code: String): String = code.trim().replace(" ", "")

    fun validate(code: String): ValidationResult {
        val trimmed = normalize(code)
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Enter a USSD code such as *804#")
        }
        if (trimmed.length < 2) {
            return ValidationResult.Invalid("USSD codes are usually at least 2 characters")
        }
        if (trimmed.length > 80) {
            return ValidationResult.Invalid("USSD code is too long")
        }
        if (!allowed.matches(trimmed)) {
            return ValidationResult.Invalid("Use only digits, * and #")
        }
        if (!trimmed.any { it == '*' || it == '#' }) {
            return ValidationResult.Invalid("A USSD code should include * or #")
        }
        return ValidationResult.Valid(trimmed)
    }

    fun isValid(code: String): Boolean = validate(code) is ValidationResult.Valid

    sealed class ValidationResult {
        data class Valid(val normalized: String) : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }
}
