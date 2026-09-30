package com.composebasics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Query("UPDATE users SET passwordHash = :hash, salt = :salt WHERE id = :id")
    suspend fun updatePassword(id: Long, hash: String, salt: String)
}

@Dao
interface PostDao {
    @Insert
    suspend fun insert(post: PostEntity): Long

    @Query(
        """
        SELECT p.id, p.text, p.imagePath, p.createdAt, u.name AS authorName
        FROM posts p INNER JOIN users u ON u.id = p.authorId
        ORDER BY p.createdAt DESC
        """
    )
    fun observePosts(): Flow<List<PostWithAuthor>>
}
