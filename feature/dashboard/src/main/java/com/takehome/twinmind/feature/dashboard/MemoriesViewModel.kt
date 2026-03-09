package com.takehome.twinmind.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.data.repository.AuthRepository
import com.takehome.twinmind.core.data.repository.ChatRepository
import com.takehome.twinmind.core.data.repository.SessionRepository
import com.takehome.twinmind.core.model.ChatMessage
import com.takehome.twinmind.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MemoriesUiState(
    val selectedTab: Int = 0, // 0 = Notes, 1 = Chats
    val searchQuery: String = "",
    val notesGroups: List<MemoriesGroup> = emptyList(),
    val chatGroups: List<MemoriesGroup> = emptyList(),
)

data class MemoriesGroup(
    val header: String,
    val items: List<MemoriesItem>,
)

data class MemoriesItem(
    val sessionId: String,
    val title: String,
    val subtitle: String,
    val isChat: Boolean,
)

@HiltViewModel
class MemoriesViewModel @Inject constructor(
    authRepository: AuthRepository,
    sessionRepository: SessionRepository,
    chatRepository: ChatRepository,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(0)
    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessions = authRepository.currentUserId.flatMapLatest { uid ->
        if (uid != null) sessionRepository.observeByUser(uid) else flowOf(emptyList())
    }
    private val allChatMessages = chatRepository.observeAll()

    val uiState: StateFlow<MemoriesUiState> = combine(
        selectedTab,
        searchQuery,
        sessions,
        allChatMessages,
    ) { tab, query, sess, chats ->
        val trimmed = query.trim()

        val notesGroups = buildNotesGroups(sess, trimmed)
        val chatGroups = buildChatGroups(sess, chats, trimmed)

        MemoriesUiState(
            selectedTab = tab,
            searchQuery = trimmed,
            notesGroups = notesGroups,
            chatGroups = chatGroups,
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), MemoriesUiState())

    fun setInitialTab(tab: Int) {
        selectedTab.value = tab.coerceIn(0, 1)
    }

    fun selectTab(tab: Int) {
        selectedTab.value = tab.coerceIn(0, 1)
    }

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    private fun buildNotesGroups(
        sessions: List<Session>,
        query: String,
    ): List<MemoriesGroup> {
        val filtered = sessions
            .sortedByDescending { it.startedAt }
            .filter { s ->
                if (query.isBlank()) return@filter true
                val hay = listOfNotNull(s.title, s.notes).joinToString(" ").lowercase(Locale.getDefault())
                hay.contains(query.lowercase(Locale.getDefault()))
            }

        return groupByDay(filtered) { s ->
            MemoriesItem(
                sessionId = s.id,
                title = s.title?.takeIf { it.isNotBlank() } ?: "Untitled Meeting",
                subtitle = buildSessionSubtitle(s),
                isChat = false,
            )
        }
    }

    private fun buildChatGroups(
        sessions: List<Session>,
        chats: List<ChatMessage>,
        query: String,
    ): List<MemoriesGroup> {
        val sessionById = sessions.associateBy { it.id }

        val userSessionIds = sessionById.keys
        val latestUserMessageBySession = chats
            .asSequence()
            .filter { it.role == "user" && it.sessionId in userSessionIds }
            .groupBy { it.sessionId }
            .mapValues { (_, msgs) -> msgs.maxByOrNull { it.createdAt } }
            .mapNotNull { (sessionId, msg) -> msg?.let { sessionId to it } }
            .sortedByDescending { (_, msg) -> msg.createdAt }

        val filtered = latestUserMessageBySession.filter { (_, msg) ->
            if (query.isBlank()) return@filter true
            msg.content.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))
        }

        val items = filtered.map { (sessionId, msg) ->
            val session = sessionById[sessionId]
            MemoriesItem(
                sessionId = sessionId,
                title = session?.title?.takeIf { it.isNotBlank() } ?: "Untitled Meeting",
                subtitle = msg.content,
                isChat = true,
            )
        }

        // We group chats by session startedAt if we have it; otherwise by message time.
        val dated = items.map { item ->
            val startedAt = sessionById[item.sessionId]?.startedAt
            (startedAt ?: System.currentTimeMillis()) to item
        }.sortedByDescending { it.first }

        return groupByDayWithTimestamps(dated) { it.second }
    }

    private fun buildSessionSubtitle(session: Session): String {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(session.startedAt))
        val durationMin = ((session.endedAt ?: session.startedAt) - session.startedAt).coerceAtLeast(0L) / 60_000L
        val durationStr = "${durationMin}m"
        return "$time · $durationStr"
    }

    private fun <T> groupByDay(list: List<T>, mapper: (T) -> MemoriesItem): List<MemoriesGroup> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val yesterdayStart = todayStart - 24L * 60L * 60L * 1000L

        val today = mutableListOf<MemoriesItem>()
        val yesterday = mutableListOf<MemoriesItem>()
        val older = mutableListOf<MemoriesItem>()

        list.forEach { t ->
            val item = mapper(t)
            val ts = when (t) {
                is Session -> t.startedAt
                else -> System.currentTimeMillis()
            }
            when {
                ts >= todayStart -> today.add(item)
                ts >= yesterdayStart -> yesterday.add(item)
                else -> older.add(item)
            }
        }

        return buildList {
            if (today.isNotEmpty()) add(MemoriesGroup(header = "Today", items = today))
            if (yesterday.isNotEmpty()) add(MemoriesGroup(header = "Yesterday", items = yesterday))
            if (older.isNotEmpty()) add(MemoriesGroup(header = "Earlier", items = older))
        }
    }

    private fun <T> groupByDayWithTimestamps(
        list: List<Pair<Long, T>>,
        mapper: (Pair<Long, T>) -> MemoriesItem,
    ): List<MemoriesGroup> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val yesterdayStart = todayStart - 24L * 60L * 60L * 1000L

        val today = mutableListOf<MemoriesItem>()
        val yesterday = mutableListOf<MemoriesItem>()
        val older = mutableListOf<MemoriesItem>()

        list.forEach { pair ->
            val ts = pair.first
            val item = mapper(pair)
            when {
                ts >= todayStart -> today.add(item)
                ts >= yesterdayStart -> yesterday.add(item)
                else -> older.add(item)
            }
        }

        return buildList {
            if (today.isNotEmpty()) add(MemoriesGroup(header = "Today", items = today))
            if (yesterday.isNotEmpty()) add(MemoriesGroup(header = "Yesterday", items = yesterday))
            if (older.isNotEmpty()) add(MemoriesGroup(header = "Earlier", items = older))
        }
    }

    private fun startOfDay(timeMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

