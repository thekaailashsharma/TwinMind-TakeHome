package com.takehome.twinmind.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takehome.twinmind.core.data.repository.AuthRepository
import com.takehome.twinmind.core.data.repository.SessionRepository
import com.takehome.twinmind.core.data.repository.UserPrefsRepository
import com.takehome.twinmind.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "User",
    val userEmail: String = "",
    val sessions: List<Session> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val userPrefsRepository: UserPrefsRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        userPrefsRepository.userName,
        sessionRepository.observeAll(),
    ) { name, sessions ->
        DashboardUiState(
            userName = name.ifBlank {
                authRepository.displayName ?: "User"
            },
            userEmail = authRepository.email.orEmpty(),
            sessions = sessions,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    fun signOut() {
        authRepository.signOut()
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.delete(sessionId)
        }
    }
}
