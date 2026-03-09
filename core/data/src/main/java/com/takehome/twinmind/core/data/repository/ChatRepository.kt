package com.takehome.twinmind.core.data.repository

import com.takehome.twinmind.core.database.dao.ChatMessageDao
import com.takehome.twinmind.core.database.mapper.toDomain
import com.takehome.twinmind.core.database.mapper.toEntity
import com.takehome.twinmind.core.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
) {

    fun observeBySession(sessionId: String): Flow<List<ChatMessage>> =
        chatMessageDao.observeBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeAll(): Flow<List<ChatMessage>> =
        chatMessageDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getBySession(sessionId: String): List<ChatMessage> =
        chatMessageDao.getBySession(sessionId).map { it.toDomain() }

    suspend fun save(message: ChatMessage) {
        chatMessageDao.insert(message.toEntity())
    }

    suspend fun getUserMessagePreviews(sessionId: String): List<String> =
        chatMessageDao.getBySession(sessionId)
            .filter { it.role == "user" }
            .map { it.content }
}
