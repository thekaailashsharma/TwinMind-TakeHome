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
import com.takehome.twinmind.core.model.Session
import com.takehome.twinmind.core.model.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordingUiState(
    val sessionId: String? = null,
    val isRecording: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,
    val transcriptText: String = "",
    val userNotes: String = "",
    val isStopping: Boolean = false,
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
    private val _isStopping = MutableStateFlow(false)

    val uiState: StateFlow<RecordingUiState> = combine(
        stateHolder.state,
        _sessionId,
        _userNotes,
        _isStopping,
    ) { recState, sessionId, notes, stopping ->
        val transcriptText = sessionId?.let {
            transcriptRepository.getFullTranscript(it)
        }.orEmpty()

        RecordingUiState(
            sessionId = sessionId,
            isRecording = recState.isRecording,
            elapsedMs = recState.elapsedMs,
            amplitude = recState.currentAmplitude,
            transcriptText = transcriptText,
            userNotes = notes,
            isStopping = stopping,
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
            _isStopping.value = true
            appContext.startService(RecordingService.stopIntent(appContext))

            _sessionId.value?.let { id ->
                sessionRepository.update(
                    sessionRepository.getById(id)?.copy(
                        status = SessionStatus.COMPLETED,
                        endedAt = System.currentTimeMillis(),
                        notes = _userNotes.value.takeIf { it.isNotBlank() },
                    ) ?: return@let,
                )
            }
            _isStopping.value = false
        }
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
}
