package com.composebasics

import android.app.Application
import android.content.Context
import com.composebasics.data.local.AppDatabase
import com.composebasics.data.repository.AuthRepository
import com.composebasics.data.repository.PostRepository
import com.composebasics.data.repository.SessionManager

/** Manual dependency container; small enough that a DI framework isn't warranted. */
class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    val authRepository = AuthRepository(database.userDao(), SessionManager(context))
    val postRepository = PostRepository(context, database.postDao())
}

class ComposeBasicsApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
