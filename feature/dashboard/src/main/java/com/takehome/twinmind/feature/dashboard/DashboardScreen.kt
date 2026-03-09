package com.takehome.twinmind.feature.dashboard

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmDashboardInfoCard
import com.takehome.twinmind.core.designsystem.component.TmDashboardTopBar
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmMemorySearchBar
import com.takehome.twinmind.core.designsystem.component.TmPrimaryButton
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindOrange
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.R as DesignR
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    onPersonalizationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUploadAudioClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showDigestSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    onViewDigestClick = { showDigestSheet = true },
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            val bgColor = Color(0xFFF0F4F7)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TwinMindDarkNavy,
                        ) {
                            Text(
                                text = "Capture",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }

                    // Mountain image — fades on all edges into bgColor
                    Image(
                        painter = painterResource(id = DesignR.drawable.home_background),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .graphicsLayer { alpha = 0.99f }
                            .drawWithContent {
                                drawContent()
                                // Fade top edge
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black),
                                        startY = 0f,
                                        endY = size.height * 0.25f,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                                // Fade bottom edge
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = size.height * 0.65f,
                                        endY = size.height,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                                // Fade left edge
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, Color.Black),
                                        startX = 0f,
                                        endX = size.width * 0.1f,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                                // Fade right edge
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startX = size.width * 0.9f,
                                        endX = size.width,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                        contentScale = ContentScale.FillWidth,
                    )

                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        TmMemorySearchBar(onClick = onChatClick)

                        Spacer(modifier = Modifier.height(16.dp))

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
                                backgroundBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE6F4F1),
                                        Color(0xFFD3EBE5),
                                    ),
                                ),
                                iconTint = TwinMindTeal,
                                borderColor = Color(0xFFCDE6E0),
                            )
                            TmDashboardInfoCard(
                                title = "Notes & Chats",
                                subtitle = "View Memories",
                                icon = TmIcons.Folder,
                                onClick = onViewMemoriesClick,
                                modifier = Modifier.weight(1f),
                                backgroundBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFF6F0),
                                        Color(0xFFFFE8D6),
                                    ),
                                ),
                                iconTint = TwinMindOrange,
                                borderColor = Color(0xFFFFE3CC),
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Inspirational quote to fill empty space
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "\u201CYour mind is for having ideas,\nnot holding them.\u201D",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TwinMindDarkNavy,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "— David Allen",
                                    fontSize = 13.sp,
                                    color = Color(0xFF9E9E9E),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        // Bottom padding so content doesn't hide behind button
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                // Fixed bottom Capture Notes button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        bgColor.copy(alpha = 0.95f),
                                        bgColor,
                                    ),
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 20.dp),
                    ) {
                        TmPrimaryButton(
                            text = "Capture Notes",
                            onClick = onCaptureNotesClick,
                            icon = TmIcons.Mic,
                            backgroundColor = TwinMindDarkNavy,
                        )
                    }
                }
            }
        }
    }

    if (showDigestSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDigestSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            DigestSheetContent()
        }
    }
}

@Composable
private fun DigestSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = DesignR.drawable.dailydigest),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = buildAnnotatedString {
                append("Unlock your ")
                withStyle(
                    SpanStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4285F4),
                                Color(0xFF9C27B0),
                            ),
                        ),
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    append("Personalized Digest")
                }
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TwinMindDarkNavy,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Capture at least 10 minutes of audio\nto unlock your daily digest",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = TwinMindDarkNavy,
        ) {
            Text(
                text = "Start Capturing",
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
