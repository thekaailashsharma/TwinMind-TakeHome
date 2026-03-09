package com.takehome.twinmind.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmCalendarEventCard
import com.takehome.twinmind.core.designsystem.component.TmDashboardInfoCard
import com.takehome.twinmind.core.designsystem.component.TmDashboardTopBar
import com.takehome.twinmind.core.designsystem.component.TmFilterChipRow
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmMemorySearchBar
import com.takehome.twinmind.core.designsystem.component.TmPrimaryButton
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindOrange
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    userName: String,
    userEmail: String,
    userPhotoUrl: String?,
    onCaptureNotesClick: () -> Unit,
    onViewDigestClick: () -> Unit,
    onChatClick: () -> Unit,
    onViewTasksClick: () -> Unit,
    onViewMemoriesClick: () -> Unit,
    onManageCalendarsClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUploadAudioClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF0F4F7),
            ) {
                DrawerContent(
                    userName = userName,
                    userEmail = userEmail,
                    userPhotoUrl = userPhotoUrl,
                    isPro = false,
                    onViewNotesClick = {
                        scope.launch { drawerState.close() }
                        onViewMemoriesClick()
                    },
                    onViewChatsClick = {
                        scope.launch { drawerState.close() }
                        onChatClick()
                    },
                    onPersonalizationClick = {
                        scope.launch { drawerState.close() }
                        onPersonalizationClick()
                    },
                    onUploadAudioClick = {
                        scope.launch { drawerState.close() }
                        onUploadAudioClick()
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    onSignOutClick = {
                        scope.launch { drawerState.close() }
                        onSignOutClick()
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TmDashboardTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onViewDigestClick = onViewDigestClick,
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF0F4F7),
                                Color(0xFFECEFF2),
                                Color(0xFFF5F5F5),
                                Color.White,
                            ),
                        ),
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding(),
                ) {
                    // Greeting
                    Text(
                        text = "Hey $userName!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TwinMindDarkNavy,
                    )
                    Text(
                        text = "Do you want me to listen and take notes?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TwinMindTeal,
                        lineHeight = 24.sp,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Capture / Later chip row
                    TmFilterChipRow(
                        tabs = listOf("Capture", "Later"),
                        selectedIndex = selectedTabIndex,
                        onTabSelected = { selectedTabIndex = it },
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mountain hero placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFE8ECF0),
                                        Color(0xFFF0F0F0),
                                    ),
                                ),
                            ),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Search bar
                    TmMemorySearchBar(onClick = onChatClick)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Info cards row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TmDashboardInfoCard(
                            title = "To-Do",
                            subtitle = "View Tasks",
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            onClick = onViewTasksClick,
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFFFFF8F2),
                            iconTint = TwinMindOrange,
                            borderColor = Color(0xFFFFE8D4),
                        )
                        TmDashboardInfoCard(
                            title = "Notes & Chats",
                            subtitle = "View Memories",
                            icon = TmIcons.Folder,
                            onClick = onViewMemoriesClick,
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFFFFF8F2),
                            iconTint = TwinMindOrange,
                            borderColor = Color(0xFFFFE8D4),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Coming Up section
                    Text(
                        text = "Coming Up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TwinMindTeal,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TmCalendarEventCard(
                        text = "No upcoming events found manage your calendars",
                        onClick = onManageCalendarsClick,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Capture Notes button
                    TmPrimaryButton(
                        text = "Capture Notes",
                        onClick = onCaptureNotesClick,
                        icon = TmIcons.Mic,
                        backgroundColor = TwinMindDarkNavy,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
