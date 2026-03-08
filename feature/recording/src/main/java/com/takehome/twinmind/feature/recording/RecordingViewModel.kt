package com.takehome.twinmind.feature.recording

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.audio.AudioRecorder
import com.takehome.twinmind.core.audio.RecordingService
import com.takehome.twinmind.core.audio.RecordingStateHolder
import com.takehome.twinmind.core.data.ai.TranscriptionWorker
import com.takehome.twinmind.core.data.repository.SessionRepository
import com.takehome.twinmind.core.data.repository.TranscriptRepository
import com.takehome.twinmind.core.model.AudioChunk
import com.takehome.twinmind.core.model.ChunkStatus
import com.takehome.twinmind.core.model.Session
import com.takehome.twinmind.core.model.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class RecordingUiState(
    val sessionId: String? = null,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val pauseReason: String? = null,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,
    val transcriptText: String = "",
    val userNotes: String = "",
    val isStopping: Boolean = false,
    val isReadyForSummary: Boolean = false,
    val silenceWarning: Boolean = false,
    val statusText: String = "Recording...",
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val transcriptRepository: TranscriptRepository,
    private val audioRecorder: AudioRecorder,
    private val stateHolder: RecordingStateHolder,
) : ViewModel() {

    private val _sessionId = MutableStateFlow<String?>(null)
    private val _userNotes = MutableStateFlow("")
    private val _stopState = MutableStateFlow(StopState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _transcriptText: StateFlow<String> = _sessionId
        .flatMapLatest { id ->
            if (id != null) {
                transcriptRepository.observeBySession(id)
                    .map { segments -> segments.joinToString(" ") { it.text } }
            } else {
                flowOf("")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val uiState: StateFlow<RecordingUiState> = combine(
        stateHolder.state,
        _transcriptText,
        _userNotes,
        _stopState,
    ) { recState, transcriptText, notes, stopState ->
        RecordingUiState(
            sessionId = recState.sessionId ?: _sessionId.value,
            isRecording = recState.isRecording,
            isPaused = recState.isPaused,
            pauseReason = recState.pauseReason,
            elapsedMs = recState.elapsedMs,
            amplitude = recState.currentAmplitude,
            transcriptText = transcriptText,
            userNotes = notes,
            isStopping = stopState.isStopping,
            isReadyForSummary = stopState.isReadyForSummary,
            silenceWarning = recState.silenceDetected,
            statusText = when {
                stopState.isStopping -> "Processing transcription..."
                recState.isPaused -> recState.pauseReason ?: "Paused"
                recState.isRecording -> "I'm listening and taking notes..."
                else -> "Ready"
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordingUiState(),
    )

    fun startRecording() {
        viewModelScope.launch {
            val session = Session()
            sessionRepository.create(session)
            _sessionId.value = session.id

            val intent = RecordingService.startIntent(appContext, session.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }

            viewModelScope.launch {
                audioRecorder.chunkReady.collect { chunk ->
                    val audioChunk = AudioChunk(
                        sessionId = chunk.sessionId,
                        chunkIndex = chunk.chunkIndex,
                        filePath = chunk.filePath,
                        durationMs = chunk.durationMs,
                    )
                    sessionRepository.saveAudioChunk(audioChunk)

                    TranscriptionWorker.enqueue(
                        context = appContext,
                        chunkId = audioChunk.id,
                        sessionId = chunk.sessionId,
                        filePath = chunk.filePath,
                        chunkIndex = chunk.chunkIndex,
                    )
                }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            _stopState.value = StopState(isStopping = true)
            appContext.startService(RecordingService.stopIntent(appContext))

            val sessionId = _sessionId.value
            if (sessionId != null) {
                sessionRepository.update(
                    sessionRepository.getById(sessionId)?.copy(
                        status = SessionStatus.COMPLETED,
                        endedAt = System.currentTimeMillis(),
                        notes = _userNotes.value.takeIf { it.isNotBlank() },
                    ) ?: return@launch,
                )

                waitForTranscriptionCompletion(sessionId)
            } else {
                _stopState.value = StopState(isStopping = false)
            }
        }
    }

    private suspend fun waitForTranscriptionCompletion(sessionId: String) {
        delay(3000)

        withTimeoutOrNull(120_000) {
            sessionRepository.observeChunksBySession(sessionId)
                .first { chunks ->
                    chunks.isNotEmpty() && chunks.all {
                        it.status == ChunkStatus.COMPLETED || it.status == ChunkStatus.FAILED
                    }
                }
        }

        _stopState.value = StopState(isStopping = false, isReadyForSummary = true)
    }

    fun clearReadyForSummary() {
        _stopState.value = StopState()
    }

    fun updateNotes(notes: String) {
        _userNotes.value = notes
    }

    fun formatElapsedTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    private data class StopState(
        val isStopping: Boolean = false,
        val isReadyForSummary: Boolean = false,
    )
}
