package com.composebasics.ui

object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    const val MIN_PASSWORD_LENGTH = 6

    fun nameError(name: String): String? = if (name.isBlank()) "Name is required" else null

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Email is required"
        !EMAIL.matches(email.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.isEmpty() -> "Password is required"
        password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Please confirm your password"
        password != confirm -> "Passwords do not match"
        else -> null
    }
}
