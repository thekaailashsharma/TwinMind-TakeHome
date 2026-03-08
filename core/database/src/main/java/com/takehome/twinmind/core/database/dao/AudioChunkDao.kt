package com.takehome.twinmind.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.takehome.twinmind.core.database.entity.AudioChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    fun observeBySession(sessionId: String): Flow<List<AudioChunkEntity>>

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId AND status = :status")
    suspend fun getBySessionAndStatus(sessionId: String, status: String): List<AudioChunkEntity>

    @Query("SELECT * FROM audio_chunks WHERE id = :id")
    suspend fun getById(id: String): AudioChunkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chunk: AudioChunkEntity)

    @Query("UPDATE audio_chunks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE audio_chunks SET status = :status, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markRetry(id: String, status: String)

    @Query("DELETE FROM audio_chunks WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
}
