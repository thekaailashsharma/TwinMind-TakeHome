package com.takehome.twinmind.feature.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.data.ai.GeminiService
import com.takehome.twinmind.core.data.repository.ChatRepository
import com.takehome.twinmind.core.data.repository.SessionRepository
import com.takehome.twinmind.core.data.repository.SummaryRepository
import com.takehome.twinmind.core.data.repository.TranscriptRepository
import com.takehome.twinmind.core.model.ChunkStatus
import com.takehome.twinmind.core.model.Summary
import com.takehome.twinmind.core.model.SummaryStatus
import com.takehome.twinmind.core.model.TranscriptSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SessionDetailUiState(
    val sessionId: String = "",
    val title: String = "Generating summary...",
    val dateTimeLocation: String = "",
    val isGeneratingSummary: Boolean = true,
    val summaryText: String = "",
    val summaryTitle: String = "",
    val keyPoints: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val transcriptSegments: List<TranscriptSegment> = emptyList(),
    val transcriptDuration: String = "",
    val transcriptPreview: String = "",
    val userNotes: String = "",
    val chatHistory: List<String> = emptyList(),
    val streamedText: String = "",
    val error: String? = null,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val transcriptRepository: TranscriptRepository,
    private val summaryRepository: SummaryRepository,
    private val chatRepository: ChatRepository,
    private val geminiService: GeminiService,
    private val cloudSyncRepository: com.takehome.twinmind.core.data.repository.CloudSyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    private var notesSaveJob: Job? = null
    private var didCloudSyncForSession: Boolean = false
    private var loadedSessionId: String? = null
    private var generateJob: Job? = null

    fun loadSession(sessionId: String) {
        if (loadedSessionId == sessionId) {
            Timber.tag("TM_TRANSCRIPT")
                .d("SessionDetailViewModel.loadSession(%s) - already loaded, refreshing chat", sessionId)
            refreshChatHistory()
            return
        }
        loadedSessionId = sessionId
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            Timber.tag("TM_TRANSCRIPT").d("SessionDetailViewModel.loadSession(%s)", sessionId)
            _uiState.value = SessionDetailUiState(
                sessionId = sessionId,
                isGeneratingSummary = true,
                title = "Generating summary...",
            )
            didCloudSyncForSession = false

            val session = sessionRepository.getById(sessionId)
            val dateFormat = SimpleDateFormat("MMM dd · h:mm a", Locale.getDefault())
            val dateStr = session?.let { dateFormat.format(Date(it.startedAt)) } ?: ""
            val locationStr = session?.locationName ?: ""
            val fullLocation = if (locationStr.isNotBlank()) "$dateStr · $locationStr" else dateStr

            _uiState.update {
                it.copy(
                    dateTimeLocation = fullLocation,
                    userNotes = session?.notes.orEmpty(),
                )
            }

            val existing = summaryRepository.getBySession(sessionId)
            if (existing != null && existing.status == SummaryStatus.COMPLETED) {
                Timber.d("loadSession: found existing COMPLETED summary")
                loadCompletedSummary(sessionId, existing)
                return@launch
            }

            observeTranscriptsAndGenerate(sessionId)
        }
    }

    private suspend fun loadCompletedSummary(sessionId: String, summary: Summary) {
        Timber.tag("TM_TRANSCRIPT").d(
            "loadCompletedSummary sessionId=%s status=%s",
            sessionId,
            summary.status,
        )
        val segments = transcriptRepository.observeBySession(sessionId).first()
        val duration = computeDuration(segments)
        val chatPreviews = chatRepository.getUserMessagePreviews(sessionId)

        _uiState.update {
            it.copy(
                isGeneratingSummary = false,
                title = summary.title.orEmpty().ifBlank { "Meeting Notes" },
                summaryTitle = summary.title.orEmpty(),
                summaryText = summary.summaryText.orEmpty(),
                keyPoints = summary.keyPoints?.split("\n")?.filter { l -> l.isNotBlank() }.orEmpty(),
                actionItems = summary.actionItems?.split("\n")?.filter { l -> l.isNotBlank() }.orEmpty(),
                transcriptSegments = segments,
                transcriptDuration = duration,
                transcriptPreview = buildTranscriptPreview(segments),
                chatHistory = chatPreviews,
            )
        }

        if (!didCloudSyncForSession) {
            didCloudSyncForSession = true
            cloudSyncRepository.syncSession(sessionId)
        }
    }

    fun refreshChatHistory() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId
            if (sessionId.isNotBlank()) {
                val chatPreviews = chatRepository.getUserMessagePreviews(sessionId)
                _uiState.update { it.copy(chatHistory = chatPreviews) }
            }
        }
    }

    fun updateNotes(notes: String) {
        val sessionId = _uiState.value.sessionId
        _uiState.update { it.copy(userNotes = notes) }
        if (sessionId.isBlank()) return

        notesSaveJob?.cancel()
        notesSaveJob = viewModelScope.launch {
            delay(350)
            val existing = sessionRepository.getById(sessionId) ?: return@launch
            val normalized = notes.takeIf { it.isNotBlank() }
            sessionRepository.update(existing.copy(notes = normalized))
            cloudSyncRepository.syncNotes(sessionId, normalized)
        }
    }

    private suspend fun observeTranscriptsAndGenerate(sessionId: String) {
        viewModelScope.launch {
            transcriptRepository.observeBySession(sessionId).collect { segments ->
                Timber.tag("TM_TRANSCRIPT").d(
                    "observeTranscriptsAndGenerate sessionId=%s segmentsCount=%d firstPreview=\"%s\"",
                    sessionId,
                    segments.size,
                    segments.firstOrNull()?.text?.take(80)?.replace("\n", " ") ?: "",
                )
                val preview = buildTranscriptPreview(segments)
                val duration = computeDuration(segments)
                _uiState.update {
                    it.copy(
                        transcriptSegments = segments,
                        transcriptPreview = preview,
                        transcriptDuration = duration,
                    )
                }

                if (!didCloudSyncForSession && segments.isNotEmpty()) {
                    Timber.tag("TM_TRANSCRIPT").d(
                        "observeTranscriptsAndGenerate triggering initial cloud sync for sessionId=%s",
                        sessionId,
                    )
                    didCloudSyncForSession = true
                    cloudSyncRepository.syncSession(sessionId)
                }
            }
        }

        delay(3000)

        val result = withTimeoutOrNull(30_000) {
            sessionRepository.observeChunksBySession(sessionId)
                .first { chunks ->
                    chunks.isNotEmpty() && chunks.all {
                        it.status == ChunkStatus.COMPLETED || it.status == ChunkStatus.FAILED
                    }
                }
        }

        if (result == null) {
            Timber.w("Transcription wait timeout for session %s, proceeding", sessionId)
        }

        val transcript = withTimeoutOrNull(5_000) {
            transcriptRepository.observeBySession(sessionId)
                .first { it.isNotEmpty() }
                .joinToString(" ") { it.text }
        } ?: transcriptRepository.getFullTranscript(sessionId)

        Timber.tag("TM_TRANSCRIPT").d(
            "observeTranscriptsAndGenerate built transcript for sessionId=%s length=%d",
            sessionId,
            transcript.length,
        )

        if (transcript.isBlank()) {
            _uiState.update {
                it.copy(
                    isGeneratingSummary = false,
                    title = "No transcript available",
                    summaryText = "No audio was transcribed.",
                )
            }
            return
        }

        generateSummary(sessionId, transcript)
    }

    private suspend fun generateSummary(sessionId: String, transcript: String) {
        Timber.tag("TM_TRANSCRIPT")
            .d("generateSummary sessionId=%s transcriptLength=%d", sessionId, transcript.length)
        _uiState.update { it.copy(isGeneratingSummary = true) }

        val accumulated = StringBuilder()
        try {
            geminiService.generateSummaryStream(transcript, _uiState.value.userNotes)
                .collect { chunk ->
                    accumulated.append(chunk)
                    _uiState.update { it.copy(streamedText = accumulated.toString()) }
                }

            val rawText = accumulated.toString()
            val parsed = parseSummaryJson(rawText)

            val summary = Summary(
                sessionId = sessionId,
                title = parsed.title,
                summaryText = parsed.summaryText,
                keyPoints = parsed.keyPoints.joinToString("\n"),
                actionItems = parsed.actionItems.joinToString("\n"),
                status = SummaryStatus.COMPLETED,
                rawResponse = rawText,
            )
            summaryRepository.save(summary)

            val session = sessionRepository.getById(sessionId)
            if (session != null) {
                sessionRepository.update(session.copy(title = parsed.title.takeIf { it.isNotBlank() } ?: session.title))
            }

            _uiState.update {
                it.copy(
                    isGeneratingSummary = false,
                    title = parsed.title.ifBlank { "Meeting Notes" },
                    summaryTitle = parsed.title,
                    summaryText = parsed.summaryText,
                    keyPoints = parsed.keyPoints,
                    actionItems = parsed.actionItems,
                )
            }

            cloudSyncRepository.syncSession(sessionId, transcriptOverride = transcript)
        } catch (e: Exception) {
            Timber.e(e, "Summary generation failed for session %s", sessionId)
            summaryRepository.save(
                Summary(
                    sessionId = sessionId,
                    status = SummaryStatus.FAILED,
                    errorMessage = e.message,
                ),
            )
            _uiState.update {
                it.copy(
                    isGeneratingSummary = false,
                    error = e.localizedMessage ?: "Summary generation failed",
                )
            }
        }
    }

    fun regenerateSummary() {
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId
            val transcript = _uiState.value.transcriptSegments.joinToString(" ") { it.text }
            _uiState.update {
                it.copy(
                    isGeneratingSummary = true,
                    streamedText = "",
                    summaryText = "",
                    actionItems = emptyList(),
                    keyPoints = emptyList(),
                    title = "Generating summary...",
                )
            }
            generateSummary(sessionId, transcript)
        }
    }

    private fun buildTranscriptPreview(segments: List<TranscriptSegment>): String {
        if (segments.isEmpty()) return ""
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val first = segments.first()
        val time = timeFormat.format(Date(first.timestampMs))
        val text = first.text.take(80)
        return "$time  $text"
    }

    private fun computeDuration(segments: List<TranscriptSegment>): String {
        if (segments.isEmpty()) return ""
        val first = segments.minOf { it.timestampMs }
        val last = segments.maxOf { it.timestampMs }
        val diffMs = last - first
        val totalSec = (diffMs / 1000).coerceAtLeast(1)
        return if (totalSec >= 60) "${totalSec / 60}m" else "${totalSec}s"
    }

    private fun parseSummaryJson(raw: String): ParsedSummary {
        return try {
            val cleaned = raw
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(cleaned)
            ParsedSummary(
                title = json.optString("title", ""),
                summaryText = json.optString("summary", ""),
                keyPoints = json.optJSONArray("keyPoints")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                }.orEmpty(),
                actionItems = json.optJSONArray("actionItems")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                }.orEmpty(),
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse summary JSON, using raw text")
            ParsedSummary(summaryText = raw)
        }
    }

    private data class ParsedSummary(
        val title: String = "",
        val summaryText: String = "",
        val keyPoints: List<String> = emptyList(),
        val actionItems: List<String> = emptyList(),
    )
}
