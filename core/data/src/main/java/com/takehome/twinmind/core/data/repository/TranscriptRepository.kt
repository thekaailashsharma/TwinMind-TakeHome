package com.takehome.twinmind.core.data.repository

import com.takehome.twinmind.core.database.dao.TranscriptDao
import com.takehome.twinmind.core.database.mapper.toDomain
import com.takehome.twinmind.core.database.mapper.toEntity
import com.takehome.twinmind.core.model.TranscriptSegment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepository @Inject constructor(
    private val transcriptDao: TranscriptDao,
) {
    fun observeBySession(sessionId: String): Flow<List<TranscriptSegment>> =
        transcriptDao.observeBySession(sessionId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun saveSegments(segments: List<TranscriptSegment>) {
        if (segments.isEmpty()) {
            Timber.tag("TM_TRANSCRIPT").w("TranscriptRepository.saveSegments called with empty list")
            return
        }
        val sessionId = segments.first().sessionId
        Timber.tag("TM_TRANSCRIPT")
            .d(
                "TranscriptRepository.saveSegments sessionId=%s count=%d chunkIds=%s previews=%s",
                sessionId,
                segments.size,
                segments.joinToString { it.chunkId },
                segments.joinToString { it.text.take(40).replace("\n", " ") },
            )
        transcriptDao.insertAll(segments.map { it.toEntity() })
    }

    suspend fun getFullTranscript(sessionId: String): String =
        transcriptDao.getFullTranscript(sessionId)
            .orEmpty()
            .also { full ->
                Timber.tag("TM_TRANSCRIPT")
                    .d(
                        "TranscriptRepository.getFullTranscript sessionId=%s length=%d preview=\"%s\"",
                        sessionId,
                        full.length,
                        full.take(80).replace("\n", " "),
                    )
            }

    suspend fun deleteBySession(sessionId: String) {
        transcriptDao.deleteBySession(sessionId)
    }
}
