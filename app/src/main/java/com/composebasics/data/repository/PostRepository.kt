package com.composebasics.data.repository

import android.content.Context
import android.net.Uri
import com.composebasics.data.local.PostDao
import com.composebasics.data.local.PostEntity
import com.composebasics.data.local.PostWithAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PostRepository(
    private val context: Context,
    private val postDao: PostDao,
) {
    fun observePosts(): Flow<List<PostWithAuthor>> = postDao.observePosts()

    /** Copies the picked image into app-private storage (picker URIs are not durable) and saves the post. */
    suspend fun addPost(authorId: Long, text: String, imageUri: Uri?) = withContext(Dispatchers.IO) {
        val imagePath = imageUri?.let { copyImage(it) }
        postDao.insert(
            PostEntity(
                authorId = authorId,
                text = text.trim(),
                imagePath = imagePath,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    private fun copyImage(uri: Uri): String {
        val dir = File(context.filesDir, "post_images").apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        val input = requireNotNull(context.contentResolver.openInputStream(uri)) { "Cannot open image" }
        input.use { src -> target.outputStream().use { src.copyTo(it) } }
        return target.absolutePath
    }
}
