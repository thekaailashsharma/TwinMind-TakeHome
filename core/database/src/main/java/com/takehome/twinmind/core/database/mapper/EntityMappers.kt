package com.takehome.twinmind.core.database.mapper

import com.takehome.twinmind.core.database.entity.AudioChunkEntity
import com.takehome.twinmind.core.database.entity.ChatMessageEntity
import com.takehome.twinmind.core.database.entity.SessionEntity
import com.takehome.twinmind.core.database.entity.SummaryEntity
import com.takehome.twinmind.core.database.entity.TranscriptSegmentEntity
import com.takehome.twinmind.core.model.AudioChunk
import com.takehome.twinmind.core.model.ChatMessage
import com.takehome.twinmind.core.model.ChunkStatus
import com.takehome.twinmind.core.model.RecordingPauseReason
import com.takehome.twinmind.core.model.Session
import com.takehome.twinmind.core.model.SessionStatus
import com.takehome.twinmind.core.model.Summary
import com.takehome.twinmind.core.model.SummaryStatus
import com.takehome.twinmind.core.model.TranscriptSegment

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    userId = userId,
    title = title,
    startedAt = startedAt,
    endedAt = endedAt,
    status = runCatching { SessionStatus.valueOf(status) }.getOrDefault(SessionStatus.COMPLETED),
    pauseReason = pauseReason?.let {
        runCatching { RecordingPauseReason.valueOf(it) }.getOrNull()
    },
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    notes = notes,
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    userId = userId,
    title = title,
    startedAt = startedAt,
    endedAt = endedAt,
    status = status.name,
    pauseReason = pauseReason?.name,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    notes = notes,
)

fun AudioChunkEntity.toDomain(): AudioChunk = AudioChunk(
    id = id,
    sessionId = sessionId,
    chunkIndex = chunkIndex,
    filePath = filePath,
    durationMs = durationMs,
    overlapMs = overlapMs,
    status = runCatching { ChunkStatus.valueOf(status) }.getOrDefault(ChunkStatus.PENDING),
    retryCount = retryCount,
)

fun AudioChunk.toEntity(): AudioChunkEntity = AudioChunkEntity(
    id = id,
    sessionId = sessionId,
    chunkIndex = chunkIndex,
    filePath = filePath,
    durationMs = durationMs,
    overlapMs = overlapMs,
    status = status.name,
    retryCount = retryCount,
)

fun TranscriptSegmentEntity.toDomain(): TranscriptSegment = TranscriptSegment(
    id = id,
    sessionId = sessionId,
    chunkId = chunkId,
    segmentIndex = segmentIndex,
    text = text,
    timestampMs = timestampMs,
)

fun TranscriptSegment.toEntity(): TranscriptSegmentEntity = TranscriptSegmentEntity(
    id = id,
    sessionId = sessionId,
    chunkId = chunkId,
    segmentIndex = segmentIndex,
    text = text,
    timestampMs = timestampMs,
)

fun SummaryEntity.toDomain(): Summary = Summary(
    id = id,
    sessionId = sessionId,
    title = title,
    summaryText = summaryText,
    actionItems = actionItems,
    keyPoints = keyPoints,
    status = runCatching { SummaryStatus.valueOf(status) }.getOrDefault(SummaryStatus.PENDING),
    rawResponse = rawResponse,
    errorMessage = errorMessage,
)

fun Summary.toEntity(): SummaryEntity = SummaryEntity(
    id = id,
    sessionId = sessionId,
    title = title,
    summaryText = summaryText,
    actionItems = actionItems,
    keyPoints = keyPoints,
    status = status.name,
    rawResponse = rawResponse,
    errorMessage = errorMessage,
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    thinkingSummary = thinkingSummary,
    thinkingDurationMs = thinkingDurationMs,
    modelName = modelName,
    createdAt = createdAt,
)

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    thinkingSummary = thinkingSummary,
    thinkingDurationMs = thinkingDurationMs,
    modelName = modelName,
    createdAt = createdAt,
)
