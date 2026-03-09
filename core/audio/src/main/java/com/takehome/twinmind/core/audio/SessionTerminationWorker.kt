package com.takehome.twinmind.core.audio

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.takehome.twinmind.core.data.ai.TranscriptionWorker
import com.takehome.twinmind.core.data.repository.SessionRepository
import com.takehome.twinmind.core.model.ChunkStatus
import com.takehome.twinmind.core.model.SessionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SessionTerminationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepository: SessionRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString("session_id") ?: return Result.failure()
        Timber.d("SessionTerminationWorker: finalizing session %s after process death", sessionId)

        val session = sessionRepository.getById(sessionId) ?: return Result.failure()
        if (session.status == SessionStatus.RECORDING) {
            sessionRepository.update(
                session.copy(
                    status = SessionStatus.COMPLETED,
                    endedAt = System.currentTimeMillis(),
                ),
            )
        }

        val pendingChunks = sessionRepository.getPendingChunks(sessionId)
        for (chunk in pendingChunks) {
            if (chunk.status == ChunkStatus.PENDING || chunk.status == ChunkStatus.FAILED) {
                Timber.d("SessionTerminationWorker: re-enqueuing transcription for chunk %s", chunk.id)
                TranscriptionWorker.enqueue(
                    context = applicationContext,
                    chunkId = chunk.id,
                    sessionId = sessionId,
                    filePath = chunk.filePath,
                    chunkIndex = chunk.chunkIndex,
                )
            }
        }

        Timber.d("SessionTerminationWorker: done for session %s", sessionId)
        return Result.success()
    }
}
