package com.composebasics.data.repository

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Salted SHA-256. Good enough for an offline demo; use a slow KDF (Argon2/bcrypt) for real backends. */
object PasswordHasher {
    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }
}
