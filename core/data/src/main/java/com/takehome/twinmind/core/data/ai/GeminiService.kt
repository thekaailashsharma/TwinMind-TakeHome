package com.takehome.twinmind.core.data.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ChatStreamChunk(
    val text: String? = null,
    val thinkingSummary: String? = null,
)

@Singleton
class GeminiService @Inject constructor() {

    private val apiKey: String = com.takehome.twinmind.core.data.BuildConfig.GEMINI_API_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun generateUrl(modelName: String): String =
        "$BASE_URL/models/$modelName:generateContent?key=$apiKey"

    private fun streamUrl(modelName: String): String =
        "$BASE_URL/models/$modelName:streamGenerateContent?alt=sse&key=$apiKey"

    suspend fun transcribeAudio(audioFile: File): Result<String> = runCatching {
        val audioBytes = withContext(Dispatchers.IO) { audioFile.readBytes() }
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

        val body = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray()
                    .put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "audio/wav")
                            put("data", base64Audio)
                        })
                    })
                    .put(JSONObject().put("text", TRANSCRIPTION_PROMPT))
                )
            ))
        }

        val text = executeGenerate(MODEL_FLASH, body)
        text.trim()
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

        val body = buildTextRequestBody(prompt)
        executeStream(MODEL_FLASH, body).collect { chunk ->
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
            val body = buildTextRequestBody(prompt)
            executeGenerate(MODEL_FLASH, body).trim()
        }.onFailure { Timber.e(it, "Summary generation failed") }

    fun chatWithContext(
        userMessage: String,
        transcript: String,
        summary: String,
        chatHistory: List<Pair<String, String>>,
        modelName: String = MODEL_FLASH,
    ): Flow<ChatStreamChunk> = flow {
        val contents = JSONArray()

        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", buildContextPrompt(transcript, summary))))
        })
        contents.put(JSONObject().apply {
            put("role", "model")
            put("parts", JSONArray().put(JSONObject().put("text",
                "I've reviewed the meeting transcript and summary. I'm ready to answer questions about it.")))
        })

        for ((role, msg) in chatHistory) {
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", msg)))
            })
        }

        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
        })

        val body = JSONObject().put("contents", contents)

        executeStream(modelName, body).collect { chunk ->
            emit(chunk)
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

    private fun buildTextRequestBody(prompt: String): JSONObject = JSONObject().apply {
        put("contents", JSONArray().put(
            JSONObject().put("parts", JSONArray().put(
                JSONObject().put("text", prompt)
            ))
        ))
    }

    private suspend fun executeGenerate(modelName: String, body: JSONObject): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(generateUrl(modelName))
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = tryExtractErrorMessage(responseBody) ?: "HTTP ${response.code}"
                throw GeminiApiException(response.code, errorMsg)
            }

            val json = JSONObject(responseBody)
            json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
                ?: ""
        }

    private fun executeStream(modelName: String, body: JSONObject): Flow<ChatStreamChunk> = flow {
        val request = Request.Builder()
            .url(streamUrl(modelName))
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        if (!response.isSuccessful) {
            val errorBody = withContext(Dispatchers.IO) { response.body?.string() ?: "" }
            val errorMsg = tryExtractErrorMessage(errorBody) ?: "HTTP ${response.code}"
            throw GeminiApiException(response.code, errorMsg)
        }

        val source = response.body?.byteStream() ?: return@flow
        val reader = BufferedReader(InputStreamReader(source))

        try {
            var line: String?
            while (true) {
                line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                if (!line.startsWith("data: ")) continue
                val jsonStr = line.removePrefix("data: ").trim()
                if (jsonStr.isEmpty()) continue

                try {
                    val json = JSONObject(jsonStr)
                    val text = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")

                    if (!text.isNullOrEmpty()) {
                        emit(ChatStreamChunk(text = text))
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse SSE chunk")
                }
            }
        } finally {
            withContext(Dispatchers.IO) {
                reader.close()
                response.close()
            }
        }
    }

    private fun tryExtractErrorMessage(responseBody: String): String? = try {
        val json = JSONObject(responseBody)
        json.optJSONObject("error")?.optString("message")
    } catch (_: Exception) {
        null
    }

    companion object {
        const val MODEL_FLASH = "gemini-2.5-flash"
        const val MODEL_PRO = MODEL_FLASH

        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

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

        private const val CHAT_SYSTEM_PROMPT = """
You are TwinMind, an AI assistant that helps users understand and interact with their meeting notes and transcripts. You have access to the full transcript and summary from this session. Answer questions accurately based on the context provided. Be helpful, concise, and conversational. If the user asks something not covered by the transcript, let them know politely.
"""
    }
}

class GeminiApiException(val httpCode: Int, message: String) : Exception(message)
