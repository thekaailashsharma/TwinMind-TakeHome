package com.takehome.twinmind.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.takehome.twinmind.feature.auth.LocationPermissionScreen
import com.takehome.twinmind.feature.auth.SignInScreen
import com.takehome.twinmind.feature.dashboard.DashboardScreen
import com.takehome.twinmind.feature.dashboard.PersonalizationScreen
import com.takehome.twinmind.feature.recording.RecordingScreen
import com.takehome.twinmind.feature.summary.SummaryBottomSheet
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

    var showSummarySheet by rememberSaveable { mutableStateOf(false) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SignInRoute> {
                SignInScreen(
                    onGoogleSignInClick = {
                        backStack.clear()
                        backStack.add(DashboardRoute)
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
                DashboardScreen(
                    userName = "User",
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
                val now = SimpleDateFormat("MMM dd • h:mm a", Locale.getDefault()).format(Date())

                RecordingScreen(
                    elapsedTime = "0:00",
                    dateTimeLocation = now,
                    transcriptText = "",
                    onBackClick = { backStack.removeLastOrNull() },
                    onChatClick = {},
                    onStopClick = {
                        showSummarySheet = true
                    },
                    onNotesCardClick = {},
                    onTranscriptCardClick = {},
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

    if (showSummarySheet) {
        SummaryBottomSheet(
            summaryText = "",
            notesText = "",
            transcriptText = "",
            transcriptTime = "00:00",
            actionItems = emptyList(),
            onDismiss = { showSummarySheet = false },
            onShareClick = {},
            onCopyClick = {},
            onRegenerateClick = {},
            onEditClick = {},
            onShareWithAttendeesClick = {},
        )
    }
}
