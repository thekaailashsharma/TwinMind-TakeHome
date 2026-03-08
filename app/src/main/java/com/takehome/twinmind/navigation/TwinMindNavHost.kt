package com.takehome.twinmind.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.takehome.twinmind.feature.auth.AuthViewModel
import com.takehome.twinmind.feature.auth.LocationPermissionScreen
import com.takehome.twinmind.feature.auth.SignInScreen
import com.takehome.twinmind.feature.dashboard.DashboardScreen
import com.takehome.twinmind.feature.dashboard.DashboardViewModel
import com.takehome.twinmind.feature.dashboard.PersonalizationScreen
import com.takehome.twinmind.feature.recording.RecordingScreen
import com.takehome.twinmind.feature.recording.RecordingViewModel
import com.takehome.twinmind.feature.summary.ActionItem
import com.takehome.twinmind.feature.summary.SummaryBottomSheet
import com.takehome.twinmind.feature.summary.SummaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwinMindNavHost(
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val startRoute = if (isLoggedIn) DashboardRoute else SignInRoute
    val backStack = rememberNavBackStack(startRoute)
    val context = LocalContext.current

    var showSummarySheet by rememberSaveable { mutableStateOf(false) }
    var currentSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<SignInRoute> {
                val authViewModel: AuthViewModel = hiltViewModel()
                val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.isSignedIn) {
                    if (uiState.isSignedIn) {
                        backStack.clear()
                        backStack.add(DashboardRoute)
                    }
                }

                SignInScreen(
                    onGoogleSignInClick = {
                        authViewModel.signInWithGoogle(context)
                    },
                )
            }

            entry<LocationPermissionRoute> {
                LocationPermissionScreen(
                    onContinueClick = {
                        backStack.removeLastOrNull()
                        backStack.add(DashboardRoute)
                    },
                    onSkipClick = {
                        backStack.removeLastOrNull()
                        backStack.add(DashboardRoute)
                    },
                )
            }

            entry<DashboardRoute> {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

                DashboardScreen(
                    userName = uiState.userName,
                    onCaptureNotesClick = { backStack.add(RecordingRoute) },
                    onViewDigestClick = {},
                    onChatClick = {},
                    onViewTasksClick = {},
                    onViewMemoriesClick = {},
                    onManageCalendarsClick = {},
                    onPersonalizationClick = { backStack.add(PersonalizationRoute) },
                    onSettingsClick = {},
                    onUploadAudioClick = {},
                )
            }

            entry<RecordingRoute> {
                val recordingViewModel: RecordingViewModel = hiltViewModel()
                val uiState by recordingViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    if (!uiState.isRecording && uiState.sessionId == null) {
                        recordingViewModel.startRecording()
                    }
                }

                val now = SimpleDateFormat("MMM dd • h:mm a", Locale.getDefault())
                    .format(Date())

                RecordingScreen(
                    elapsedTime = recordingViewModel.formatElapsedTime(uiState.elapsedMs),
                    dateTimeLocation = now,
                    transcriptText = uiState.transcriptText,
                    onBackClick = {
                        recordingViewModel.stopRecording()
                        backStack.removeLastOrNull()
                    },
                    onChatClick = {},
                    onStopClick = {
                        recordingViewModel.stopRecording()
                        currentSessionId = uiState.sessionId
                        showSummarySheet = true
                    },
                    onNotesCardClick = {},
                    onTranscriptCardClick = {},
                    isRecording = uiState.isRecording,
                )
            }

            entry<PersonalizationRoute> {
                PersonalizationScreen(
                    onCancelClick = { backStack.removeLastOrNull() },
                    onSaveClick = { _, _, _, _, _ ->
                        backStack.removeLastOrNull()
                    },
                )
            }
        },
    )

    if (showSummarySheet && currentSessionId != null) {
        val summaryViewModel: SummaryViewModel = hiltViewModel()
        val summaryState by summaryViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(currentSessionId) {
            currentSessionId?.let { summaryViewModel.loadSummary(it) }
        }

        SummaryBottomSheet(
            summaryText = summaryState.summaryText.ifBlank { summaryState.streamedText },
            notesText = summaryState.userNotes,
            transcriptText = summaryState.transcriptText,
            transcriptTime = "00:00",
            actionItems = summaryState.actionItems.map { ActionItem(it) },
            onDismiss = {
                showSummarySheet = false
                currentSessionId = null
            },
            onShareClick = {},
            onCopyClick = {},
            onRegenerateClick = {
                currentSessionId?.let { summaryViewModel.regenerateSummary(it) }
            },
            onEditClick = {},
            onShareWithAttendeesClick = {},
        )
    }
}
