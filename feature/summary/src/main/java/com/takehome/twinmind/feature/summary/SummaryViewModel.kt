package com.takehome.twinmind.feature.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.data.ai.GeminiService
import com.takehome.twinmind.core.data.repository.SummaryRepository
import com.takehome.twinmind.core.data.repository.TranscriptRepository
import com.takehome.twinmind.core.model.Summary
import com.takehome.twinmind.core.model.SummaryStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

data class SummaryUiState(
    val isLoading: Boolean = false,
    val summaryTitle: String = "",
    val summaryText: String = "",
    val keyPoints: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val transcriptText: String = "",
    val userNotes: String = "",
    val error: String? = null,
    val isStreaming: Boolean = false,
    val streamedText: String = "",
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val summaryRepository: SummaryRepository,
    private val transcriptRepository: TranscriptRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    fun loadSummary(sessionId: String, userNotes: String = "") {
        viewModelScope.launch {
            Timber.d("SummaryViewModel.loadSummary(sessionId=%s)", sessionId)
            _uiState.update { it.copy(isLoading = true, error = null) }

            val existing = summaryRepository.getBySession(sessionId)
            if (existing != null && existing.status == SummaryStatus.COMPLETED) {
                Timber.d("loadSummary: found existing COMPLETED summary for sessionId=%s", sessionId)
                val transcript = transcriptRepository.getFullTranscript(sessionId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        transcriptText = transcript,
                        userNotes = userNotes,
                        summaryTitle = existing.title.orEmpty(),
                        summaryText = existing.summaryText.orEmpty(),
                        keyPoints = existing.keyPoints
                            ?.split("\n")
                            ?.filter { line -> line.isNotBlank() }
                            .orEmpty(),
                        actionItems = existing.actionItems
                            ?.split("\n")
                            ?.filter { line -> line.isNotBlank() }
                            .orEmpty(),
                    )
                }
                return@launch
            }

            Timber.d("loadSummary: no completed summary, waiting for transcript for sessionId=%s", sessionId)
            val transcript = withTimeoutOrNull(30_000) {
                transcriptRepository.observeBySession(sessionId)
                    .map { segments -> segments.joinToString(" ") { it.text } }
                    .filter { it.isNotBlank() }
                    .first()
            } ?: run {
                Timber.w("loadSummary: transcript flow timeout for sessionId=%s, falling back to getFullTranscript", sessionId)
                transcriptRepository.getFullTranscript(sessionId)
            }

            Timber.d("loadSummary: obtained transcript for sessionId=%s, length=%d", sessionId, transcript.length)

            _uiState.update { it.copy(transcriptText = transcript, userNotes = userNotes) }
            generateSummary(sessionId, transcript, userNotes)
        }
    }

    fun regenerateSummary(sessionId: String) {
        viewModelScope.launch {
            Timber.d("SummaryViewModel.regenerateSummary(sessionId=%s)", sessionId)
            val transcript = _uiState.value.transcriptText
            val notes = _uiState.value.userNotes
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    streamedText = "",
                    summaryText = "",
                    actionItems = emptyList(),
                    keyPoints = emptyList(),
                )
            }
            generateSummary(sessionId, transcript, notes)
        }
    }

    private suspend fun generateSummary(
        sessionId: String,
        transcript: String,
        userNotes: String,
    ) {
        Timber.d(
            "generateSummary(sessionId=%s) called, transcriptLength=%d, notesLength=%d",
            sessionId,
            transcript.length,
            userNotes.length,
        )
        if (transcript.isBlank()) {
            Timber.w("generateSummary: transcript blank for sessionId=%s", sessionId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    summaryText = "No transcript available to summarize.",
                )
            }
            return
        }

        _uiState.update { it.copy(isStreaming = true) }

        val accumulated = StringBuilder()
        try {
            geminiService.generateSummaryStream(transcript, userNotes).collect { chunk ->
                accumulated.append(chunk)
                _uiState.update { it.copy(streamedText = accumulated.toString()) }
            }

            val rawText = accumulated.toString()
            Timber.d("generateSummary: received streamed summary for sessionId=%s, length=%d", sessionId, rawText.length)
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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isStreaming = false,
                    summaryTitle = parsed.title,
                    summaryText = parsed.summaryText,
                    keyPoints = parsed.keyPoints,
                    actionItems = parsed.actionItems,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Summary generation failed")
            val summary = Summary(
                sessionId = sessionId,
                status = SummaryStatus.FAILED,
                errorMessage = e.message,
            )
            summaryRepository.save(summary)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isStreaming = false,
                    error = e.localizedMessage ?: "Summary generation failed",
                )
            }
        }
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
