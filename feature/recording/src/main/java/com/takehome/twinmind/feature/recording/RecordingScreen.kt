package com.takehome.twinmind.feature.recording

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmBackTopBar
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmRecordingBar
import com.takehome.twinmind.core.designsystem.component.TmRecordingIndicator
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun RecordingScreen(
    elapsedTime: String,
    dateTimeLocation: String,
    transcriptText: String,
    statusText: String,
    onBackClick: () -> Unit,
    onStopClick: () -> Unit,
    onLowStorageBackClick: () -> Unit,
    onNotesCardClick: () -> Unit,
    onTranscriptCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecording: Boolean = true,
    isPaused: Boolean = false,
    silenceWarning: Boolean = false,
    errorMessage: String? = null,
    micSourceChanged: String? = null,
) {
    var userNotes by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TmBackTopBar(onBackClick = onBackClick)
        },
        bottomBar = {
            if (errorMessage == null) {
                TmRecordingBar(
                    elapsedTime = elapsedTime,
                    onStopClick = onStopClick,
                    isRecording = isRecording && !isPaused,
                )
            } else {
                LowStorageBar(
                    onFixStorageClick = onLowStorageBackClick,
                )
            }
        },
        containerColor = Color(0xFFFAF8F5),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isPaused -> Color(0xFFE67E22)
                    else -> TwinMindTeal
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateTimeLocation,
                fontSize = 13.sp,
                color = TwinMindGray,
            )

            AnimatedVisibility(
                visible = silenceWarning,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = TmIcons.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE67E22),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No audio detected for 10+ seconds. Is your microphone working?",
                            fontSize = 13.sp,
                            color = Color(0xFF5D4037),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = TmIcons.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 13.sp,
                            color = Color(0xFFB71C1C),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = micSourceChanged != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = TmIcons.Info,
                            contentDescription = null,
                            tint = Color(0xFF1565C0),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = micSourceChanged ?: "",
                            fontSize = 13.sp,
                            color = Color(0xFF0D47A1),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notes card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNotesCardClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Notes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TwinMindDarkNavy,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = TmIcons.ChevronRight,
                            contentDescription = "Expand",
                            tint = TwinMindGray,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = TmIcons.Edit,
                            contentDescription = null,
                            tint = TwinMindGray,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = userNotes,
                            onValueChange = { userNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "Write your notes here, TwinMind will use them to enhance the summary...",
                                    fontSize = 14.sp,
                                    color = TwinMindGray,
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = TwinMindDarkNavy,
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = TwinMindDarkNavy,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transcript card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTranscriptCardClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Transcript",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TwinMindDarkNavy,
                            modifier = Modifier.weight(1f),
                        )
                        if (isRecording && !isPaused) {
                            TmRecordingIndicator(elapsedTime = elapsedTime)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = TmIcons.ChevronRight,
                            contentDescription = "Expand",
                            tint = TwinMindGray,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = transcriptText.ifEmpty {
                            "Transcript will appear here as you speak…"
                        },
                        fontSize = 14.sp,
                        color = if (transcriptText.isEmpty()) TwinMindTeal else TwinMindDarkNavy,
                        lineHeight = 20.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LowStorageBar(
    onFixStorageClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = Color.White,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = TwinMindDarkNavy,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Recording stopped - Low storage",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TwinMindWhite,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Free up space, then start a new note.",
                            fontSize = 12.sp,
                            color = Color(0xFFCFD8DC),
                        )
                    }

                    Surface(
                        modifier = Modifier.clickable(onClick = onFixStorageClick),
                        shape = RoundedCornerShape(24.dp),
                        color = TwinMindWhite,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Go back",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TwinMindDarkNavy,
                            )
                        }
                    }
                }
            }
        }
    }
}
