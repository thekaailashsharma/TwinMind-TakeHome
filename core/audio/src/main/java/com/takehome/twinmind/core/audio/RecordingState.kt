package com.takehome.twinmind.core.audio

data class RecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val pauseReason: String? = null,
    val sessionId: String? = null,
    val elapsedMs: Long = 0L,
    val currentAmplitude: Float = 0f,
    val chunkCount: Int = 0,
    val silenceDetected: Boolean = false,
    val errorMessage: String? = null,
    val micSourceChanged: String? = null,
)
