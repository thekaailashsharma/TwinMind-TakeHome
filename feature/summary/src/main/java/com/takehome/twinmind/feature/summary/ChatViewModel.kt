package com.takehome.twinmind.feature.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.data.ai.GeminiService
import com.takehome.twinmind.core.data.repository.ChatRepository
import com.takehome.twinmind.core.data.repository.SummaryRepository
import com.takehome.twinmind.core.data.repository.TranscriptRepository
import com.takehome.twinmind.core.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ChatUiState(
    val sessionId: String = "",
    val messages: List<ChatMessageUi> = emptyList(),
    val isStreaming: Boolean = false,
    val currentModelName: String = GeminiService.MODEL_FLASH,
    val suggestedPrompts: List<String> = DEFAULT_PROMPTS,
    val memoriesEnabled: Boolean = false,
) {
    companion object {
        val DEFAULT_PROMPTS = listOf(
            "Summarize the key decisions made",
            "What are the next steps?",
            "List all action items with owners",
        )
    }
}

data class ChatMessageUi(
    val id: String,
    val role: String,
    val content: String,
    val thinkingSummary: String? = null,
    val thinkingDurationMs: Long = 0,
    val modelName: String? = null,
    val isStreaming: Boolean = false,
    val showThinking: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val chatRepository: ChatRepository,
    private val transcriptRepository: TranscriptRepository,
    private val summaryRepository: SummaryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var transcript = ""
    private var summaryText = ""

    fun loadSession(sessionId: String) {
        if (_uiState.value.sessionId == sessionId) return
        _uiState.update { it.copy(sessionId = sessionId) }

        viewModelScope.launch {
            transcript = transcriptRepository.getFullTranscript(sessionId)
            val summary = summaryRepository.getBySession(sessionId)
            summaryText = summary?.summaryText.orEmpty()

            val existing = chatRepository.getBySession(sessionId)
            val messageUis = existing.map { msg ->
                ChatMessageUi(
                    id = msg.id,
                    role = msg.role,
                    content = msg.content,
                    thinkingSummary = msg.thinkingSummary,
                    thinkingDurationMs = msg.thinkingDurationMs,
                    modelName = msg.modelName,
                )
            }
            _uiState.update { it.copy(messages = messageUis) }
        }
    }

    fun setModel(modelName: String) {
        _uiState.update { it.copy(currentModelName = modelName) }
    }

    fun toggleMemories() {
        _uiState.update { it.copy(memoriesEnabled = !it.memoriesEnabled) }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isStreaming) return

        val userMessage = ChatMessage(
            sessionId = _uiState.value.sessionId,
            role = "user",
            content = text.trim(),
            modelName = _uiState.value.currentModelName,
        )
        val userUi = ChatMessageUi(
            id = userMessage.id,
            role = "user",
            content = userMessage.content,
        )

        val aiPlaceholder = ChatMessageUi(
            id = "streaming_${System.currentTimeMillis()}",
            role = "model",
            content = "",
            isStreaming = true,
            modelName = _uiState.value.currentModelName,
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userUi + aiPlaceholder,
                isStreaming = true,
            )
        }

        viewModelScope.launch {
            chatRepository.save(userMessage)

            val chatHistory = _uiState.value.messages
                .filter { it.role == "user" || (it.role == "model" && !it.isStreaming) }
                .dropLast(1)
                .map { it.role to it.content }

            val responseBuilder = StringBuilder()
            val thinkingBuilder = StringBuilder()
            val thinkingStartMs = System.currentTimeMillis()
            var hasThinking = false

            try {
                geminiService.chatWithContext(
                    userMessage = text.trim(),
                    transcript = transcript,
                    summary = summaryText,
                    chatHistory = chatHistory,
                    modelName = _uiState.value.currentModelName,
                ).collect { chunk ->
                    chunk.thinkingSummary?.let { thinking ->
                        hasThinking = true
                        thinkingBuilder.append(thinking)
                        updateStreamingMessage(
                            content = responseBuilder.toString(),
                            thinking = thinkingBuilder.toString(),
                            thinkingDurationMs = System.currentTimeMillis() - thinkingStartMs,
                        )
                    }
                    chunk.text?.let { text ->
                        responseBuilder.append(text)
                        updateStreamingMessage(
                            content = responseBuilder.toString(),
                            thinking = thinkingBuilder.toString(),
                            thinkingDurationMs = if (hasThinking) System.currentTimeMillis() - thinkingStartMs else 0,
                        )
                    }
                }

                val thinkingDuration = if (hasThinking) System.currentTimeMillis() - thinkingStartMs else 0

                val aiMessage = ChatMessage(
                    sessionId = _uiState.value.sessionId,
                    role = "model",
                    content = responseBuilder.toString(),
                    thinkingSummary = thinkingBuilder.toString().ifBlank { null },
                    thinkingDurationMs = thinkingDuration,
                    modelName = _uiState.value.currentModelName,
                )
                chatRepository.save(aiMessage)

                finalizeStreamingMessage(
                    aiMessage.id,
                    responseBuilder.toString(),
                    thinkingBuilder.toString().ifBlank { null },
                    thinkingDuration,
                )
            } catch (e: Exception) {
                Timber.e(e, "Chat error")
                finalizeStreamingMessage(
                    "error_${System.currentTimeMillis()}",
                    "Sorry, I encountered an error. Please try again.",
                    null,
                    0,
                )
            }
        }
    }

    private fun updateStreamingMessage(
        content: String,
        thinking: String,
        thinkingDurationMs: Long,
    ) {
        _uiState.update { state ->
            val updated = state.messages.toMutableList()
            val lastIdx = updated.lastIndex
            if (lastIdx >= 0 && updated[lastIdx].isStreaming) {
                updated[lastIdx] = updated[lastIdx].copy(
                    content = content,
                    thinkingSummary = thinking.ifBlank { null },
                    thinkingDurationMs = thinkingDurationMs,
                )
            }
            state.copy(messages = updated)
        }
    }

    private fun finalizeStreamingMessage(
        id: String,
        content: String,
        thinking: String?,
        thinkingDurationMs: Long,
    ) {
        _uiState.update { state ->
            val updated = state.messages.toMutableList()
            val lastIdx = updated.lastIndex
            if (lastIdx >= 0 && updated[lastIdx].isStreaming) {
                updated[lastIdx] = updated[lastIdx].copy(
                    id = id,
                    content = content,
                    thinkingSummary = thinking,
                    thinkingDurationMs = thinkingDurationMs,
                    isStreaming = false,
                    modelName = state.currentModelName,
                )
            }
            state.copy(messages = updated, isStreaming = false)
        }
    }
}
