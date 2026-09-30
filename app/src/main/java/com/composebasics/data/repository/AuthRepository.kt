package com.composebasics.data.repository

import com.composebasics.data.local.UserDao
import com.composebasics.data.local.UserEntity

class AuthRepository(
    private val userDao: UserDao,
    private val session: SessionManager,
) {
    val isLoggedIn: Boolean get() = session.userId != null
    val currentUserId: Long? get() = session.userId

    /** Seeds a demo account so the app is usable offline without a sign-up screen. */
    suspend fun ensureDemoUser() {
        if (userDao.findByEmail(DEMO_EMAIL) != null) return
        val salt = PasswordHasher.newSalt()
        userDao.insert(
            UserEntity(
                name = "Demo User",
                email = DEMO_EMAIL,
                passwordHash = PasswordHasher.hash(DEMO_PASSWORD, salt),
                salt = salt,
            )
        )
    }

    /** Creates an account and logs it in. @return false if the email is already registered. */
    suspend fun register(name: String, email: String, password: String): Boolean {
        val normalized = email.trim()
        if (userDao.findByEmail(normalized) != null) return false
        val salt = PasswordHasher.newSalt()
        val id = userDao.insert(
            UserEntity(
                name = name.trim(),
                email = normalized,
                passwordHash = PasswordHasher.hash(password, salt),
                salt = salt,
            )
        )
        session.save(id)
        return true
    }

    /** @return true on success; the session is stored. */
    suspend fun login(email: String, password: String): Boolean {
        val user = userDao.findByEmail(email.trim()) ?: return false
        if (PasswordHasher.hash(password, user.salt) != user.passwordHash) return false
        session.save(user.id)
        return true
    }

    /** Offline reset: no email/OTP verification is possible, so knowing the email is enough. */
    suspend fun resetPassword(email: String, newPassword: String): Boolean {
        val user = userDao.findByEmail(email.trim()) ?: return false
        val salt = PasswordHasher.newSalt()
        userDao.updatePassword(user.id, PasswordHasher.hash(newPassword, salt), salt)
        return true
    }

    fun logout() = session.clear()

    companion object {
        const val DEMO_EMAIL = "demo@example.com"
        const val DEMO_PASSWORD = "Password@123"
    }
}
