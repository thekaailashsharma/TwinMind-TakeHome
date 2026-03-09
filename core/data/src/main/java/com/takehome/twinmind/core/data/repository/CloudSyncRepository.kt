package com.takehome.twinmind.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.takehome.twinmind.core.model.ChatMessage
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionRepository: SessionRepository,
    private val transcriptRepository: TranscriptRepository,
    private val summaryRepository: SummaryRepository,
    private val chatRepository: ChatRepository,
) {
    private fun userDoc() = auth.currentUser?.uid?.let { firestore.collection("users").document(it) }

    suspend fun syncSession(sessionId: String) {
        val user = auth.currentUser ?: return
        val userRef = userDoc() ?: return
        val session = sessionRepository.getById(sessionId) ?: return

        val transcript = transcriptRepository.getFullTranscript(sessionId)
        val summary = summaryRepository.getBySession(sessionId)
        val chats = chatRepository.getBySession(sessionId)

        try {
            userRef.set(
                mapOf(
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "photoUrl" to user.photoUrl?.toString(),
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()

            val sessionRef = userRef.collection("sessions").document(sessionId)
            sessionRef.set(
                mapOf(
                    "sessionId" to session.id,
                    "title" to session.title,
                    "startedAt" to session.startedAt,
                    "endedAt" to session.endedAt,
                    "status" to session.status.name,
                    "notes" to session.notes,
                    "locationName" to session.locationName,
                    "latitude" to session.latitude,
                    "longitude" to session.longitude,
                    "transcript" to transcript.takeIf { it.isNotBlank() },
                    "summaryTitle" to summary?.title,
                    "summaryText" to summary?.summaryText,
                    "keyPoints" to summary?.keyPoints,
                    "actionItems" to summary?.actionItems,
                    "summaryStatus" to summary?.status?.name,
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()

            // Chat messages stored as subcollection for size safety.
            syncChatMessagesInternal(userRef.collection("sessions").document(sessionId), chats)
        } catch (e: Exception) {
            Timber.w(e, "Cloud sync failed for sessionId=%s", sessionId)
        }
    }

    suspend fun syncNotes(sessionId: String, notes: String?) {
        val userRef = userDoc() ?: return
        try {
            userRef.collection("sessions").document(sessionId)
                .set(
                    mapOf(
                        "notes" to notes,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                    SetOptions.merge(),
                ).await()
        } catch (e: Exception) {
            Timber.w(e, "Cloud notes sync failed for sessionId=%s", sessionId)
        }
    }

    suspend fun syncChatMessage(message: ChatMessage) {
        val userRef = userDoc() ?: return
        try {
            val sessionRef = userRef.collection("sessions").document(message.sessionId)
            sessionRef.collection("chatMessages").document(message.id)
                .set(
                    mapOf(
                        "id" to message.id,
                        "sessionId" to message.sessionId,
                        "role" to message.role,
                        "content" to message.content,
                        "thinkingSummary" to message.thinkingSummary,
                        "thinkingDurationMs" to message.thinkingDurationMs,
                        "modelName" to message.modelName,
                        "createdAt" to message.createdAt,
                    ),
                    SetOptions.merge(),
                ).await()
            sessionRef.set(
                mapOf(
                    "updatedAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
        } catch (e: Exception) {
            Timber.w(e, "Cloud chat sync failed for messageId=%s", message.id)
        }
    }

    private suspend fun syncChatMessagesInternal(
        sessionRef: com.google.firebase.firestore.DocumentReference,
        messages: List<ChatMessage>,
    ) {
        if (messages.isEmpty()) return
        try {
            val batch = firestore.batch()
            val col = sessionRef.collection("chatMessages")
            messages.forEach { msg ->
                val ref = col.document(msg.id)
                batch.set(
                    ref,
                    mapOf(
                        "id" to msg.id,
                        "sessionId" to msg.sessionId,
                        "role" to msg.role,
                        "content" to msg.content,
                        "thinkingSummary" to msg.thinkingSummary,
                        "thinkingDurationMs" to msg.thinkingDurationMs,
                        "modelName" to msg.modelName,
                        "createdAt" to msg.createdAt,
                    ),
                    SetOptions.merge(),
                )
            }
            batch.commit().await()
        } catch (e: Exception) {
            Timber.w(e, "Cloud chat batch sync failed")
        }
    }
}

