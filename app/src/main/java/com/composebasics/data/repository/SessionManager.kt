package com.composebasics.data.repository

import android.content.Context
import androidx.core.content.edit

/** Persists the id of the logged-in user so login survives app restarts. */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    val userId: Long? get() = prefs.getLong(KEY_USER_ID, NONE).takeIf { it != NONE }

    fun save(userId: Long) = prefs.edit { putLong(KEY_USER_ID, userId) }

    fun clear() = prefs.edit { remove(KEY_USER_ID) }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val NONE = -1L
    }
}
