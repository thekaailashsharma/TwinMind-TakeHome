package com.takehome.twinmind.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
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
data class SummaryEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val title: String?,
    val summaryText: String?,
    val actionItems: String?,
    val keyPoints: String?,
    val status: String,
    val rawResponse: String?,
    val errorMessage: String?,
)
