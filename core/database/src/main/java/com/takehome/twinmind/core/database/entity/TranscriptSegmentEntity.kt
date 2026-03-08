package com.takehome.twinmind.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("chunkId")],
)
data class TranscriptSegmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val chunkId: String,
    val segmentIndex: Int,
    val text: String,
    val timestampMs: Long,
)
