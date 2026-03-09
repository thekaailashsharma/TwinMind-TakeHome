package com.takehome.twinmind.feature.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmShimmerCard
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    state: SessionDetailUiState,
    onBackClick: () -> Unit,
    onSummaryCardClick: () -> Unit,
    onTranscriptCardClick: () -> Unit,
    onTasksCardClick: () -> Unit,
    onChatClick: () -> Unit,
    onMoreClick: () -> Unit,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onChatHistoryClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = TmIcons.Back,
                            contentDescription = "Back",
                            tint = TwinMindDarkNavy,
                        )
                    }
                },
                actions = {
                    if (!state.isGeneratingSummary) {
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                imageVector = TmIcons.More,
                                contentDescription = "More",
                                tint = TwinMindDarkNavy,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAF8F5),
                ),
            )
        },
        bottomBar = {
            ChatWithNoteButton(
                onClick = onChatClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
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
            // Title
            Text(
                text = state.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TwinMindDarkNavy,
                lineHeight = 28.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.dateTimeLocation,
                fontSize = 13.sp,
                color = TwinMindGray,
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state.isGeneratingSummary) {
                GeneratingContent(state)
            } else {
                CompletedContent(
                    state = state,
                    onSummaryCardClick = onSummaryCardClick,
                    onTranscriptCardClick = onTranscriptCardClick,
                    onTasksCardClick = onTasksCardClick,
                    onChatHistoryClick = onChatHistoryClick,
                    onEditClick = onEditClick,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GeneratingContent(state: SessionDetailUiState) {
    // Notes & Summary card - generating state
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Notes & Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TwinMindDarkNavy,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Generating",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TwinMindTeal,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = TmIcons.ChevronRight,
                    contentDescription = null,
                    tint = TwinMindGray,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.Top,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = TwinMindTeal,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Twinmind is generating the summary and enhancing your notes...",
                    fontSize = 14.sp,
                    color = TwinMindGray,
                    lineHeight = 20.sp,
                )
            }

            if (state.streamedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                TmShimmerCard(lineCount = 2)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Transcript card
    if (state.transcriptPreview.isNotBlank()) {
        TranscriptPreviewCard(
            duration = state.transcriptDuration,
            preview = state.transcriptPreview,
            onClick = {},
        )
    }
}

@Composable
private fun CompletedContent(
    state: SessionDetailUiState,
    onSummaryCardClick: () -> Unit,
    onTranscriptCardClick: () -> Unit,
    onTasksCardClick: () -> Unit,
    onChatHistoryClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
) {
    // Notes & Summary card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSummaryCardClick),
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
                    text = "Notes & Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TwinMindDarkNavy,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = TmIcons.ChevronRight,
                    contentDescription = null,
                    tint = TwinMindGray,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (state.summaryTitle.isNotBlank()) {
                Text(
                    text = state.summaryTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TwinMindDarkNavy,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = state.summaryText.take(200) + if (state.summaryText.length > 200) "..." else "",
                fontSize = 14.sp,
                color = TwinMindDarkNavy,
                lineHeight = 20.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )

            if (state.keyPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                state.keyPoints.take(2).forEach { point ->
                    Text(
                        text = "• $point",
                        fontSize = 13.sp,
                        color = TwinMindGray,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Edit / Share floating row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TwinMindDarkNavy,
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Icon(
                        imageVector = TmIcons.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Edit", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TwinMindDarkNavy,
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Icon(
                        imageVector = TmIcons.Mail,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Transcript card
    AnimatedVisibility(
        visible = state.transcriptPreview.isNotBlank(),
        enter = fadeIn(),
    ) {
        TranscriptPreviewCard(
            duration = state.transcriptDuration,
            preview = state.transcriptPreview,
            onClick = onTranscriptCardClick,
        )
    }

    // Tasks to Review card
    if (state.actionItems.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTasksCardClick),
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
                    Icon(
                        imageVector = TmIcons.CheckboxBlank,
                        contentDescription = null,
                        tint = TwinMindTeal,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.actionItems.size} Task${if (state.actionItems.size > 1) "s" else ""} to Review",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TwinMindDarkNavy,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = TmIcons.ChevronRight,
                        contentDescription = null,
                        tint = TwinMindGray,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = TmIcons.Person,
                        contentDescription = null,
                        tint = TwinMindTeal,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.actionItems.first(),
                        fontSize = 14.sp,
                        color = TwinMindGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    // Chat History
    if (state.chatHistory.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onChatHistoryClick),
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
                    Icon(
                        imageVector = TmIcons.Chat,
                        contentDescription = null,
                        tint = TwinMindTeal,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chat History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TwinMindDarkNavy,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${state.chatHistory.size} message${if (state.chatHistory.size > 1) "s" else ""}",
                        fontSize = 13.sp,
                        color = TwinMindGray,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = TmIcons.ChevronRight,
                        contentDescription = null,
                        tint = TwinMindGray,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                state.chatHistory.take(3).forEach { msg ->
                    Text(
                        text = "\"$msg\"",
                        fontSize = 14.sp,
                        color = TwinMindGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptPreviewCard(
    duration: String,
    preview: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                if (duration.isNotBlank()) {
                    Text(
                        text = duration,
                        fontSize = 13.sp,
                        color = TwinMindGray,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = TmIcons.ChevronRight,
                    contentDescription = null,
                    tint = TwinMindGray,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = preview,
                fontSize = 14.sp,
                color = TwinMindGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ChatWithNoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = TwinMindDarkNavy,
        ),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Icon(
            imageVector = TmIcons.Sparkle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TwinMindTeal,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Chat with ",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TwinMindDarkNavy,
        )
        Text(
            text = "this note",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TwinMindTeal,
        )
    }
}
