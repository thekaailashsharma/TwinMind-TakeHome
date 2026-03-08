package com.takehome.twinmind.core.data.ai

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.takehome.twinmind.core.data.repository.SessionRepository
import com.takehome.twinmind.core.data.repository.TranscriptRepository
import com.takehome.twinmind.core.model.ChunkStatus
import com.takehome.twinmind.core.model.TranscriptSegment
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.io.File

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val geminiService: GeminiService,
    private val sessionRepository: SessionRepository,
    private val transcriptRepository: TranscriptRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val chunkId = inputData.getString(KEY_CHUNK_ID) ?: return Result.failure()
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val chunkIndex = inputData.getInt(KEY_CHUNK_INDEX, 0)

        Timber.d("Transcribing chunk $chunkIndex for session $sessionId")

        sessionRepository.updateChunkStatus(chunkId, ChunkStatus.TRANSCRIBING)

        val file = File(filePath)
        if (!file.exists()) {
            Timber.e("Audio file not found: $filePath")
            sessionRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)
            return Result.failure()
        }

        return geminiService.transcribeAudio(file).fold(
            onSuccess = { text ->
                if (text.isNotBlank()) {
                    val segment = TranscriptSegment(
                        sessionId = sessionId,
                        chunkId = chunkId,
                        segmentIndex = chunkIndex,
                        text = text,
                        timestampMs = System.currentTimeMillis(),
                    )
                    transcriptRepository.saveSegments(listOf(segment))
                }
                sessionRepository.updateChunkStatus(chunkId, ChunkStatus.COMPLETED)
                Timber.d("Transcription complete for chunk $chunkIndex")
                Result.success()
            },
            onFailure = { e ->
                Timber.e(e, "Transcription failed for chunk $chunkIndex")
                sessionRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            },
        )
    }

    companion object {
        const val KEY_CHUNK_ID = "chunk_id"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_CHUNK_INDEX = "chunk_index"
        private const val MAX_RETRIES = 3

        fun enqueue(
            context: Context,
            chunkId: String,
            sessionId: String,
            filePath: String,
            chunkIndex: Int,
        ) {
            val data = Data.Builder()
                .putString(KEY_CHUNK_ID, chunkId)
                .putString(KEY_SESSION_ID, sessionId)
                .putString(KEY_FILE_PATH, filePath)
                .putInt(KEY_CHUNK_INDEX, chunkIndex)
                .build()

            val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(data)
                .addTag("transcription_$sessionId")
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
