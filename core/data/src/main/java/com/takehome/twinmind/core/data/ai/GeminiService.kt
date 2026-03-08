package com.takehome.twinmind.core.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash")
    }

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

    companion object {
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
    }
}
