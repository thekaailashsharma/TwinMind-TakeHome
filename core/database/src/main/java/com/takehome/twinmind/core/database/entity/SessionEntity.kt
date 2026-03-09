package com.takehome.twinmind.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val title: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val pauseReason: String?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String?,
)
