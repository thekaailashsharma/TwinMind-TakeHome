package com.takehome.twinmind.core.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class LiveSuggestion(
    val emoji: String,
    val text: String,
)

data class ChatStreamChunk(
    val text: String? = null,
    val thinkingSummary: String? = null,
)

@Singleton
class GeminiService @Inject constructor() {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.5-flash-lite")
    }

    private fun getModelWithThinking(modelName: String) =
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = modelName,
                generationConfig = generationConfig {
                    thinkingConfig = thinkingConfig {
                        includeThoughts = true
                    }
                },
            )

    suspend fun transcribeAudio(audioFile: File): Result<String> = runCatching {
        val audioBytes = audioFile.readBytes()
        val response = model.generateContent(
            content {
                inlineData(audioBytes, "audio/wav")
                text(TRANSCRIPTION_PROMPT)
            },
        )
        response.text.orEmpty().trim()
    }.onFailure { Timber.e(it, "Transcription failed for ${audioFile.name}") }

    fun generateSummaryStream(transcript: String, userNotes: String = ""): Flow<String> = flow {
        val prompt = buildString {
            append(SUMMARY_PROMPT)
            append("\n\n--- TRANSCRIPT ---\n")
            append(transcript)
            if (userNotes.isNotBlank()) {
                append("\n\n--- USER NOTES ---\n")
                append(userNotes)
            }
        }

        val stream = model.generateContentStream(content { text(prompt) })
        stream.collect { chunk ->
            chunk.text?.let { emit(it) }
        }
    }

    suspend fun generateSummary(transcript: String, userNotes: String = ""): Result<String> =
        runCatching {
            val prompt = buildString {
                append(SUMMARY_PROMPT)
                append("\n\n--- TRANSCRIPT ---\n")
                append(transcript)
                if (userNotes.isNotBlank()) {
                    append("\n\n--- USER NOTES ---\n")
                    append(userNotes)
                }
            }
            val response = model.generateContent(content { text(prompt) })
            response.text.orEmpty().trim()
        }.onFailure { Timber.e(it, "Summary generation failed") }

    suspend fun generateLiveSuggestions(transcript: String): Result<List<LiveSuggestion>> =
        runCatching {
            val prompt = buildString {
                append(LIVE_SUGGESTIONS_PROMPT)
                append("\n\n--- TRANSCRIPT ---\n")
                append(transcript)
            }
            val response = model.generateContent(content { text(prompt) })
            val raw = response.text.orEmpty().trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            parseLiveSuggestions(raw)
        }.onFailure { Timber.e(it, "Live suggestions generation failed") }

    private fun parseLiveSuggestions(raw: String): List<LiveSuggestion> {
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                LiveSuggestion(
                    emoji = obj.optString("emoji", "💡"),
                    text = obj.optString("text", ""),
                )
            }.filter { it.text.isNotBlank() }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse live suggestions JSON")
            emptyList()
        }
    }

    fun chatWithContext(
        userMessage: String,
        transcript: String,
        summary: String,
        chatHistory: List<Pair<String, String>>,
        modelName: String = MODEL_FLASH,
    ): Flow<ChatStreamChunk> = flow {
        val thinkingModel = getModelWithThinking(modelName)
        val chat = thinkingModel.startChat(
            history = buildList {
                add(content("user") { text(buildContextPrompt(transcript, summary)) })
                add(content("model") {
                    text("I've reviewed the meeting transcript and summary. I'm ready to answer questions about it.")
                })
                for ((role, msg) in chatHistory) {
                    add(content(role) { text(msg) })
                }
            },
        )

        val stream = chat.sendMessageStream(userMessage)
        stream.collect { chunk ->
            val thinking = chunk.thoughtSummary
            val text = chunk.text
            if (thinking != null || text != null) {
                emit(ChatStreamChunk(text = text, thinkingSummary = thinking))
            }
        }
    }

    private fun buildContextPrompt(transcript: String, summary: String): String = buildString {
        append(CHAT_SYSTEM_PROMPT)
        if (transcript.isNotBlank()) {
            append("\n\n--- TRANSCRIPT ---\n")
            append(transcript)
        }
        if (summary.isNotBlank()) {
            append("\n\n--- SUMMARY ---\n")
            append(summary)
        }
    }

    companion object {
        const val MODEL_FLASH = "gemini-2.5-flash-lite"
        const val MODEL_PRO = MODEL_FLASH

        private const val TRANSCRIPTION_PROMPT = """
Transcribe this audio recording accurately. 
Return ONLY the transcription text, no headers or formatting.
If the audio is unclear or silent, return an empty string.
"""

        private const val SUMMARY_PROMPT = """
Analyze this meeting/conversation transcript and provide a structured summary in the following JSON format:
{
  "title": "Brief descriptive title",
  "summary": "2-3 paragraph summary of key discussion points",
  "keyPoints": ["point 1", "point 2", ...],
  "actionItems": ["action 1", "action 2", ...]
}

Return ONLY valid JSON, no markdown formatting or code blocks.
If user notes are provided, incorporate them into the summary.
"""

        private const val LIVE_SUGGESTIONS_PROMPT = """
Based on this conversation transcript, generate exactly 3 contextual suggestions, insights or tips that are relevant and helpful given the discussion.

Return ONLY a JSON array with exactly 3 objects. Each object must have:
- "emoji": a single relevant emoji
- "text": a short helpful suggestion or insight (max 60 characters)

Example: [{"emoji":"💡","text":"Consider discussing next steps"},{"emoji":"📋","text":"Review action items from last meeting"},{"emoji":"🎯","text":"Set clear deadlines for deliverables"}]

Return ONLY the JSON array, no other text.
"""

        private const val CHAT_SYSTEM_PROMPT = """
You are TwinMind, an AI assistant that helps users understand and interact with their meeting notes and transcripts. You have access to the full transcript and summary from this session. Answer questions accurately based on the context provided. Be helpful, concise, and conversational. If the user asks something not covered by the transcript, let them know politely.
"""
    }
}
