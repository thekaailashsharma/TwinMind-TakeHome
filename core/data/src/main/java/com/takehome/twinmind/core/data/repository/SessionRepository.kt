package com.takehome.twinmind.core.data.repository

import com.takehome.twinmind.core.database.dao.AudioChunkDao
import com.takehome.twinmind.core.database.dao.SessionDao
import com.takehome.twinmind.core.database.mapper.toDomain
import com.takehome.twinmind.core.database.mapper.toEntity
import com.takehome.twinmind.core.model.AudioChunk
import com.takehome.twinmind.core.model.ChunkStatus
import com.takehome.twinmind.core.model.Session
import com.takehome.twinmind.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val audioChunkDao: AudioChunkDao,
) {
    fun observeAll(): Flow<List<Session>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeById(id: String): Flow<Session?> =
        sessionDao.observeById(id).map { it?.toDomain() }

    fun observeActive(): Flow<List<Session>> =
        sessionDao.observeByStatus(SessionStatus.RECORDING.name)
            .map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Session? =
        sessionDao.getById(id)?.toDomain()

    suspend fun create(session: Session): Session {
        sessionDao.upsert(session.toEntity())
        return session
    }

    suspend fun update(session: Session) {
        sessionDao.upsert(session.toEntity())
    }

    suspend fun delete(id: String) {
        sessionDao.deleteById(id)
    }

    suspend fun saveAudioChunk(chunk: AudioChunk) {
        audioChunkDao.upsert(chunk.toEntity())
    }

    suspend fun getPendingChunks(sessionId: String): List<AudioChunk> =
        audioChunkDao.getBySessionAndStatus(sessionId, ChunkStatus.PENDING.name)
            .map { it.toDomain() }

    fun observeChunksBySession(sessionId: String): Flow<List<AudioChunk>> =
        audioChunkDao.observeBySession(sessionId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun updateChunkStatus(chunkId: String, status: ChunkStatus) {
        audioChunkDao.updateStatus(chunkId, status.name)
    }
}
