package com.composebasics.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
)

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("authorId")],
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: Long,
    val text: String,
    /** Absolute path of an image copied into app-private storage, or null for text-only posts. */
    val imagePath: String?,
    val createdAt: Long,
)

/** Result of the posts + author join used by the home feed. */
data class PostWithAuthor(
    val id: Long,
    val text: String,
    val imagePath: String?,
    val createdAt: Long,
    val authorName: String,
)
