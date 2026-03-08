package com.takehome.twinmind.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class AudioChunkEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val chunkIndex: Int,
    val filePath: String,
    val durationMs: Long,
    val overlapMs: Long,
    val status: String,
    val retryCount: Int,
)
