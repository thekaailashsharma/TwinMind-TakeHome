package com.takehome.twinmind.navigation

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.takehome.twinmind.feature.auth.AuthViewModel
import com.takehome.twinmind.feature.auth.LocationPermissionScreen
import com.takehome.twinmind.feature.auth.OnboardingScreen
import com.takehome.twinmind.feature.auth.SignInScreen
import com.takehome.twinmind.feature.auth.SplashScreen
import com.takehome.twinmind.feature.dashboard.DashboardScreen
import com.takehome.twinmind.feature.dashboard.DashboardViewModel
import com.takehome.twinmind.feature.dashboard.MemoriesScreen
import com.takehome.twinmind.feature.dashboard.MemoriesViewModel
import com.takehome.twinmind.feature.dashboard.PersonalizationScreen
import com.takehome.twinmind.feature.dashboard.PersonalizationViewModel
import com.takehome.twinmind.feature.recording.RecordingScreen
import com.takehome.twinmind.feature.recording.RecordingViewModel
import com.takehome.twinmind.feature.recording.TranscriptDetailSheet
import com.takehome.twinmind.feature.recording.TranscriptEntry
import com.takehome.twinmind.feature.summary.ActionItem
import com.takehome.twinmind.feature.summary.ActionItemsReviewSheet
import com.takehome.twinmind.feature.summary.ChatScreen
import com.takehome.twinmind.feature.summary.ChatViewModel
import com.takehome.twinmind.feature.summary.SessionDetailScreen
import com.takehome.twinmind.feature.summary.SessionDetailViewModel
import com.takehome.twinmind.feature.summary.SummaryBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwinMindNavHost(
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val startRoute = SplashRoute
    val backStack = rememberNavBackStack(startRoute)
    val context = LocalContext.current

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry<SplashRoute> {
                SplashScreen(
                    onFinished = {
                        backStack.clear()
                        if (isLoggedIn) {
                            backStack.add(DashboardRoute)
                        } else {
                            backStack.add(OnboardingRoute)
                        }
                    },
                )
            }

            entry<OnboardingRoute> {
                OnboardingScreen(
                    onGetStarted = {
                        backStack.clear()
                        backStack.add(SignInRoute)
                    },
                )
            }

            entry<SignInRoute> {
                val authViewModel: AuthViewModel = hiltViewModel()
                val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

                // Only navigate when user actively signs in (transition from false -> true)
                var hasNavigated by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(uiState.isSignedIn) {
                    if (uiState.isSignedIn && !hasNavigated) {
                        // Double-check Firebase state to prevent stale ViewModel cache
                        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                            hasNavigated = true
                            backStack.clear()
                            backStack.add(DashboardRoute)
                        }
                    }
                }

                SignInScreen(
                    onGoogleSignInClick = {
                        authViewModel.signInWithGoogle(context)
                    },
                )
            }

            entry<LocationPermissionRoute> {
                var permissionRequested by rememberSaveable { mutableStateOf(false) }

                val locationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { results ->
                    val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        Toast.makeText(context, "Location enabled", Toast.LENGTH_SHORT).show()
                    }
                    backStack.removeLastOrNull()
                    backStack.add(DashboardRoute)
                }

                LocationPermissionScreen(
                    onContinueClick = {
                        if (!permissionRequested) {
                            permissionRequested = true
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
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
                    userEmail = uiState.userEmail,
                    userPhotoUrl = uiState.userPhotoUrl,
                    onCaptureNotesClick = { backStack.add(RecordingRoute(recordingId = UUID.randomUUID().toString())) },
                    onViewDigestClick = {},
                    onChatClick = { backStack.add(MemoriesRoute(initialTab = 1)) },
                    onViewTasksClick = {},
                    onViewMemoriesClick = { backStack.add(MemoriesRoute(initialTab = 0)) },
                    onPersonalizationClick = { backStack.add(PersonalizationRoute) },
                    onSettingsClick = {},
                    onUploadAudioClick = {},
                    onSignOutClick = {
                        dashboardViewModel.signOut()
                        backStack.clear()
                        backStack.add(OnboardingRoute)
                    },
                )
            }

            entry<RecordingRoute> {
                val recordingViewModel: RecordingViewModel = hiltViewModel()
                val uiState by recordingViewModel.uiState.collectAsStateWithLifecycle()

                var audioPermissionGranted by rememberSaveable { mutableStateOf(false) }
                var permissionDenied by rememberSaveable { mutableStateOf(false) }
                var showTranscriptSheet by rememberSaveable { mutableStateOf(false) }
                var hasStartedRecording by rememberSaveable { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { results ->
                    val audioGranted =
                        results[Manifest.permission.RECORD_AUDIO] == true
                    if (audioGranted) {
                        audioPermissionGranted = true
                    } else {
                        permissionDenied = true
                        Toast.makeText(
                            context,
                            "Microphone permission is required to record",
                            Toast.LENGTH_LONG,
                        ).show()
                        backStack.removeLastOrNull()
                    }
                }

                LaunchedEffect(Unit) {
                    recordingViewModel.resetForNewSession()
                    hasStartedRecording = false

                    val hasAudioPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasAudioPerm) {
                        audioPermissionGranted = true
                    } else if (!permissionDenied) {
                        val perms = buildList {
                            add(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                }

                LaunchedEffect(audioPermissionGranted) {
                    if (audioPermissionGranted && !hasStartedRecording) {
                        hasStartedRecording = true
                        Timber.d("TwinMindNavHost: starting new recording")
                        recordingViewModel.startRecording()
                    }
                }

                val now = SimpleDateFormat("MMM dd · h:mm a", Locale.getDefault())
                    .format(Date())

                RecordingScreen(
                    elapsedTime = recordingViewModel.formatElapsedTime(uiState.elapsedMs),
                    dateTimeLocation = now,
                    transcriptText = uiState.transcriptText,
                    statusText = uiState.statusText,
                    onBackClick = {
                        val sessionId = recordingViewModel.stopRecording()
                        backStack.removeLastOrNull()
                        if (sessionId != null) {
                            backStack.add(SessionDetailRoute(sessionId))
                        }
                    },
                    onStopClick = {
                        val sessionId = recordingViewModel.stopRecording()
                        if (sessionId != null) {
                            backStack.removeLastOrNull()
                            backStack.add(SessionDetailRoute(sessionId))
                        }
                    },
                    onLowStorageBackClick = {
                        // For low storage, just leave the recording flow and return to dashboard
                        backStack.clear()
                        backStack.add(DashboardRoute)
                    },
                    onNotesCardClick = {},
                    onTranscriptCardClick = { showTranscriptSheet = true },
                    isRecording = uiState.isRecording,
                    isPaused = uiState.isPaused,
                    silenceWarning = uiState.silenceWarning,
                    errorMessage = uiState.errorMessage,
                    micSourceChanged = uiState.micSourceChanged,
                )

                if (showTranscriptSheet) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val transcriptParts = uiState.transcriptText.split(". ")
                        .filter { it.isNotBlank() }
                    val entries = if (transcriptParts.isEmpty()) {
                        emptyList()
                    } else {
                        listOf(
                            TranscriptEntry(
                                timestamp = timeFormat.format(Date()),
                                text = uiState.transcriptText,
                            ),
                        )
                    }

                    TranscriptDetailSheet(
                        entries = entries,
                        userNotes = uiState.userNotes,
                        onDismiss = { showTranscriptSheet = false },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Transcript", uiState.transcriptText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Transcript copied", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }

            entry<SessionDetailRoute> { route ->
                val sessionDetailViewModel: SessionDetailViewModel = hiltViewModel()
                val state by sessionDetailViewModel.uiState.collectAsStateWithLifecycle()

                var showSummarySheet by rememberSaveable { mutableStateOf(false) }
                var summarySheetInitialTab by rememberSaveable { mutableStateOf(0) }
                var showActionItemsSheet by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(route.sessionId) {
                    sessionDetailViewModel.loadSession(route.sessionId)
                }

                SessionDetailScreen(
                    state = state,
                    onBackClick = {
                        backStack.removeLastOrNull()
                    },
                    onSummaryCardClick = {
                        summarySheetInitialTab = 0
                        showSummarySheet = true
                    },
                    onTranscriptCardClick = {
                        summarySheetInitialTab = 2
                        showSummarySheet = true
                    },
                    onTasksCardClick = {
                        showActionItemsSheet = true
                    },
                    onChatClick = {
                        backStack.add(ChatRoute(route.sessionId))
                    },
                    onMoreClick = {},
                    onEditClick = {
                        summarySheetInitialTab = 1
                        showSummarySheet = true
                    },
                    onChatHistoryClick = {
                        backStack.add(ChatRoute(route.sessionId))
                    },
                )

                if (showSummarySheet) {
                    SummaryBottomSheet(
                        summaryText = state.summaryText,
                        keyPoints = state.keyPoints,
                        notesText = state.userNotes,
                        transcriptSegments = state.transcriptSegments,
                        actionItems = state.actionItems.map { ActionItem(it) },
                        isLoading = state.isGeneratingSummary,
                        initialTab = summarySheetInitialTab,
                        onDismiss = { showSummarySheet = false },
                        onNotesChanged = { sessionDetailViewModel.updateNotes(it) },
                        onShareClick = {
                            val shareText = buildString {
                                if (state.summaryTitle.isNotBlank()) {
                                    appendLine(state.summaryTitle)
                                    appendLine()
                                }
                                appendLine(state.summaryText)
                                if (state.actionItems.isNotEmpty()) {
                                    appendLine()
                                    appendLine("Action Items:")
                                    state.actionItems.forEach { appendLine("- $it") }
                                }
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Summary"))
                        },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Summary", state.summaryText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
                        },
                        onRegenerateClick = {
                            sessionDetailViewModel.regenerateSummary()
                        },
                        onEditClick = {},
                        onShareWithAttendeesClick = {},
                        onCopyTranscript = {
                            val transcript = state.transcriptSegments.joinToString("\n") { it.text }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Transcript", transcript)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Transcript copied", Toast.LENGTH_SHORT).show()
                        },
                        onSplitTranscript = {},
                    )
                }

                if (showActionItemsSheet) {
                    ActionItemsReviewSheet(
                        actionItems = state.actionItems,
                        onDismiss = { showActionItemsSheet = false },
                        onClearAll = { showActionItemsSheet = false },
                    )
                }
            }

            entry<ChatRoute> { route ->
                val chatViewModel: ChatViewModel = hiltViewModel()
                val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(route.sessionId) {
                    chatViewModel.loadSession(route.sessionId)
                }

                ChatScreen(
                    state = chatState,
                    onBackClick = { backStack.removeLastOrNull() },
                    onSendMessage = { chatViewModel.sendMessage(it) },
                    onModelSelected = { chatViewModel.setModel(it) },
                    onToggleMemories = { chatViewModel.toggleMemories() },
                )
            }

            entry<PersonalizationRoute> {
                val personalizationViewModel: PersonalizationViewModel = hiltViewModel()

                PersonalizationScreen(
                    onCancelClick = { backStack.removeLastOrNull() },
                    onSaveClick = { name, role, language, _, additionalInfo ->
                        personalizationViewModel.save(name, role, language, additionalInfo)
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<MemoriesRoute> { route ->
                val memoriesViewModel: MemoriesViewModel = hiltViewModel()
                val state by memoriesViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(route.initialTab) {
                    memoriesViewModel.setInitialTab(route.initialTab)
                }

                MemoriesScreen(
                    state = state,
                    onBackClick = { backStack.removeLastOrNull() },
                    onTabSelected = { idx -> memoriesViewModel.selectTab(idx) },
                    onSearchChanged = { q -> memoriesViewModel.setSearch(q) },
                    onSessionClick = { sessionId ->
                        backStack.add(SessionDetailRoute(sessionId))
                    },
                    onChatClick = { sessionId ->
                        backStack.add(ChatRoute(sessionId))
                    },
                )
            }
        },
    )
}
