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
import kotlinx.coroutines.delay
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

        Timber.tag("TM_TRANSCRIPT").d(
            "TranscriptionWorker.doWork start chunkIndex=%d sessionId=%s filePath=%s",
            chunkIndex,
            sessionId,
            filePath,
        )

        sessionRepository.updateChunkStatus(chunkId, ChunkStatus.TRANSCRIBING)

        val file = File(filePath)
        if (!file.exists()) {
            Timber.tag("TM_TRANSCRIPT").e("Audio file not found: %s", filePath)
            sessionRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)
            return Result.failure()
        }

        return geminiService.transcribeAudio(file).fold(
            onSuccess = { text ->
                Timber.tag("TM_TRANSCRIPT").d(
                    "TranscriptionWorker.onSuccess chunkIndex=%d sessionId=%s textLength=%d",
                    chunkIndex,
                    sessionId,
                    text.length,
                )
                if (text.isNotBlank()) {
                    val segment = TranscriptSegment(
                        sessionId = sessionId,
                        chunkId = chunkId,
                        segmentIndex = chunkIndex,
                        text = text,
                        timestampMs = System.currentTimeMillis(),
                    )
                    Timber.tag("TM_TRANSCRIPT").d(
                        "Saving TranscriptSegment sessionId=%s chunkId=%s index=%d preview=\"%s\"",
                        sessionId,
                        chunkId,
                        chunkIndex,
                        text.take(80),
                    )
                    transcriptRepository.saveSegments(listOf(segment))
                } else {
                    Timber.tag("TM_TRANSCRIPT").w(
                        "Empty transcript text for chunkIndex=%d sessionId=%s",
                        chunkIndex,
                        sessionId,
                    )
                }
                sessionRepository.updateChunkStatus(chunkId, ChunkStatus.COMPLETED)
                Timber.tag("TM_TRANSCRIPT").d(
                    "Transcription complete for chunkIndex=%d sessionId=%s",
                    chunkIndex,
                    sessionId,
                )
                Result.success()
            },
            onFailure = { e ->
                Timber.tag("TM_TRANSCRIPT").e(e, "Transcription failed for chunkIndex=%d sessionId=%s", chunkIndex, sessionId)
                sessionRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)

                if (e is GeminiApiException && e.httpCode == 429) {
                    Timber.w("Rate limited (429) — waiting 15s before retry")
                    delay(15_000)
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                } else {
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                }
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
