package com.takehome.twinmind.core.model

import java.util.UUID

enum class SessionStatus {
    RECORDING,
    PAUSED,
    COMPLETED,
    INTERRUPTED,
}

enum class RecordingPauseReason {
    PHONE_CALL,
    AUDIO_FOCUS_LOST,
    USER_PAUSED,
}

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val title: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val status: SessionStatus = SessionStatus.RECORDING,
    val pauseReason: RecordingPauseReason? = null,
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
)

enum class ChunkStatus {
    PENDING,
    TRANSCRIBING,
    COMPLETED,
    FAILED,
}

data class AudioChunk(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val chunkIndex: Int,
    val filePath: String,
    val durationMs: Long = 0,
    val overlapMs: Long = 0,
    val status: ChunkStatus = ChunkStatus.PENDING,
    val retryCount: Int = 0,
)

data class TranscriptSegment(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val chunkId: String,
    val segmentIndex: Int,
    val text: String,
    val timestampMs: Long = 0,
)

enum class SummaryStatus {
    PENDING,
    GENERATING,
    COMPLETED,
    FAILED,
}

data class Summary(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val title: String? = null,
    val summaryText: String? = null,
    val actionItems: String? = null,
    val keyPoints: String? = null,
    val status: SummaryStatus = SummaryStatus.PENDING,
    val rawResponse: String? = null,
    val errorMessage: String? = null,
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String,
    val content: String,
    val thinkingSummary: String? = null,
    val thinkingDurationMs: Long = 0,
    val modelName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
