package com.takehome.twinmind.feature.recording

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.audio.AudioRecorder
import com.takehome.twinmind.core.audio.RecordingService
import com.takehome.twinmind.core.audio.RecordingStateHolder
import com.takehome.twinmind.core.data.ai.GeminiService
import com.takehome.twinmind.core.data.ai.TranscriptionWorker
import com.takehome.twinmind.core.data.repository.SessionRepository
import com.takehome.twinmind.core.data.repository.TranscriptRepository
import com.takehome.twinmind.core.model.AudioChunk
import com.takehome.twinmind.core.model.Session
import com.takehome.twinmind.core.model.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
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
    val silenceWarning: Boolean = false,
    val statusText: String = "Recording...",
    val liveSuggestions: List<LiveSuggestionUi> = emptyList(),
)

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val transcriptRepository: TranscriptRepository,
    private val audioRecorder: AudioRecorder,
    private val stateHolder: RecordingStateHolder,
    private val geminiService: GeminiService,
) : ViewModel() {

    private val _sessionId = MutableStateFlow<String?>(null)
    private val _userNotes = MutableStateFlow("")
    private val _liveSuggestions = MutableStateFlow<List<LiveSuggestionUi>>(emptyList())

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

    init {
        observeTranscriptForSuggestions()
    }

    @OptIn(FlowPreview::class)
    private fun observeTranscriptForSuggestions() {
        viewModelScope.launch {
            _transcriptText
                .debounce(10_000)
                .collect { transcript ->
                    if (transcript.isNotBlank() && transcript.length > 20) {
                        fetchLiveSuggestions(transcript)
                    }
                }
        }
    }

    private suspend fun fetchLiveSuggestions(transcript: String) {
        geminiService.generateLiveSuggestions(transcript)
            .onSuccess { suggestions ->
                _liveSuggestions.value = suggestions.map {
                    LiveSuggestionUi(emoji = it.emoji, text = it.text)
                }
                Timber.d("Live suggestions updated: %d items", suggestions.size)
            }
            .onFailure {
                Timber.w(it, "Failed to fetch live suggestions")
            }
    }

    fun refreshSuggestions() {
        val transcript = _transcriptText.value
        if (transcript.isNotBlank()) {
            viewModelScope.launch { fetchLiveSuggestions(transcript) }
        }
    }

    val uiState: StateFlow<RecordingUiState> = combine(
        stateHolder.state,
        _transcriptText,
        _userNotes,
        _liveSuggestions,
    ) { recState, transcriptText, notes, suggestions ->
        RecordingUiState(
            sessionId = recState.sessionId ?: _sessionId.value,
            isRecording = recState.isRecording,
            isPaused = recState.isPaused,
            pauseReason = recState.pauseReason,
            elapsedMs = recState.elapsedMs,
            amplitude = recState.currentAmplitude,
            transcriptText = transcriptText,
            userNotes = notes,
            silenceWarning = recState.silenceDetected,
            statusText = when {
                recState.isPaused -> recState.pauseReason ?: "Paused"
                recState.isRecording -> "I'm listening and taking notes..."
                else -> "Ready"
            },
            liveSuggestions = suggestions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordingUiState(),
    )

    fun resetForNewSession() {
        _sessionId.value = null
        _userNotes.value = ""
        _liveSuggestions.value = emptyList()
        stateHolder.reset()
        Timber.d("RecordingViewModel.resetForNewSession() - state cleared")
    }

    fun startRecording() {
        viewModelScope.launch {
            val isCurrentlyRecording = stateHolder.state.value.isRecording
            if (_sessionId.value != null && isCurrentlyRecording) {
                Timber.w(
                    "RecordingViewModel.startRecording() called while already recording; existingSessionId=%s",
                    _sessionId.value,
                )
                return@launch
            }

            // If a session was already created for this screen entry, ignore duplicate calls.
            // (This prevents creating a second sessionId and breaking the transcript pipeline.)
            if (_sessionId.value != null && !isCurrentlyRecording) {
                Timber.w(
                    "RecordingViewModel.startRecording() duplicate call ignored; existingSessionId=%s",
                    _sessionId.value,
                )
                return@launch
            }

            Timber.d("RecordingViewModel.startRecording() - creating new session")
            val session = Session()
            sessionRepository.create(session)
            _sessionId.value = session.id
            Timber.d("Created new session id=%s", session.id)

            val intent = RecordingService.startIntent(appContext, session.id)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }

            viewModelScope.launch {
                audioRecorder.chunkReady.collect { chunk ->
                    Timber.d(
                        "chunkReady: sessionId=%s index=%d path=%s durationMs=%d",
                        chunk.sessionId,
                        chunk.chunkIndex,
                        chunk.filePath,
                        chunk.durationMs,
                    )
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

    fun stopRecording(): String? {
        val sessionId = _sessionId.value
        Timber.d("RecordingViewModel.stopRecording() called, sessionId=%s", sessionId)
        appContext.startService(RecordingService.stopIntent(appContext))

        if (sessionId != null) {
            viewModelScope.launch {
                sessionRepository.update(
                    sessionRepository.getById(sessionId)?.copy(
                        status = SessionStatus.COMPLETED,
                        endedAt = System.currentTimeMillis(),
                        notes = _userNotes.value.takeIf { it.isNotBlank() },
                    ) ?: return@launch,
                )
                Timber.d("Marked session %s as COMPLETED", sessionId)
            }
        }
        return sessionId
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
