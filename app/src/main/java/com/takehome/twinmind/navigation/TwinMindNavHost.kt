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
import com.takehome.twinmind.feature.auth.SignInScreen
import com.takehome.twinmind.feature.dashboard.DashboardScreen
import com.takehome.twinmind.feature.dashboard.DashboardViewModel
import com.takehome.twinmind.feature.dashboard.PersonalizationScreen
import com.takehome.twinmind.feature.dashboard.PersonalizationViewModel
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
                    onCaptureNotesClick = { backStack.add(RecordingRoute) },
                    onViewDigestClick = {},
                    onChatClick = {},
                    onViewTasksClick = {},
                    onViewMemoriesClick = {},
                    onManageCalendarsClick = {},
                    onPersonalizationClick = { backStack.add(PersonalizationRoute) },
                    onSettingsClick = {},
                    onUploadAudioClick = {},
                    onSignOutClick = {
                        dashboardViewModel.signOut()
                        backStack.clear()
                        backStack.add(SignInRoute)
                    },
                )
            }

            entry<RecordingRoute> {
                val recordingViewModel: RecordingViewModel = hiltViewModel()
                val uiState by recordingViewModel.uiState.collectAsStateWithLifecycle()

                var audioPermissionGranted by rememberSaveable { mutableStateOf(false) }
                var permissionDenied by rememberSaveable { mutableStateOf(false) }

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
                    if (audioPermissionGranted &&
                        !uiState.isRecording &&
                        uiState.sessionId == null
                    ) {
                        recordingViewModel.startRecording()
                    }
                }

                LaunchedEffect(uiState.isReadyForSummary) {
                    if (uiState.isReadyForSummary) {
                        currentSessionId = uiState.sessionId
                        showSummarySheet = true
                        recordingViewModel.clearReadyForSummary()
                    }
                }

                val now = SimpleDateFormat("MMM dd • h:mm a", Locale.getDefault())
                    .format(Date())

                RecordingScreen(
                    elapsedTime = recordingViewModel.formatElapsedTime(uiState.elapsedMs),
                    dateTimeLocation = now,
                    transcriptText = uiState.transcriptText,
                    statusText = uiState.statusText,
                    onBackClick = {
                        if (!uiState.isStopping) {
                            recordingViewModel.stopRecording()
                            backStack.removeLastOrNull()
                        }
                    },
                    onChatClick = {},
                    onStopClick = {
                        if (!uiState.isStopping) {
                            recordingViewModel.stopRecording()
                        }
                    },
                    onNotesCardClick = {},
                    onTranscriptCardClick = {},
                    isRecording = uiState.isRecording,
                    isPaused = uiState.isPaused,
                    isStopping = uiState.isStopping,
                    silenceWarning = uiState.silenceWarning,
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
            isLoading = summaryState.isLoading || summaryState.isStreaming,
            onDismiss = {
                showSummarySheet = false
                currentSessionId = null
            },
            onShareClick = {
                val shareText = buildString {
                    if (summaryState.summaryTitle.isNotBlank()) {
                        appendLine(summaryState.summaryTitle)
                        appendLine()
                    }
                    appendLine(summaryState.summaryText)
                    if (summaryState.actionItems.isNotEmpty()) {
                        appendLine()
                        appendLine("Action Items:")
                        summaryState.actionItems.forEach { appendLine("- $it") }
                    }
                }
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Summary"))
            },
            onCopyClick = {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Summary", summaryState.summaryText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
            },
            onRegenerateClick = {
                currentSessionId?.let { summaryViewModel.regenerateSummary(it) }
            },
            onEditClick = {},
            onShareWithAttendeesClick = {},
        )
    }
}
