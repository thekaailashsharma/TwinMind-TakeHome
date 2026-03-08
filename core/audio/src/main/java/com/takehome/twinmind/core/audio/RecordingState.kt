package com.takehome.twinmind.core.audio

data class RecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val sessionId: String? = null,
    val elapsedMs: Long = 0L,
    val currentAmplitude: Float = 0f,
    val chunkCount: Int = 0,
)
