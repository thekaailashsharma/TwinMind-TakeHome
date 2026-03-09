package com.takehome.twinmind.feature.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmBottomSheetHeader
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmModalBottomSheet
import com.takehome.twinmind.core.designsystem.component.TmShareBanner
import com.takehome.twinmind.core.designsystem.component.TmSummaryActionRow
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.model.TranscriptSegment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryBottomSheet(
    summaryText: String,
    keyPoints: List<String>,
    notesText: String,
    transcriptSegments: List<TranscriptSegment>,
    actionItems: List<ActionItem>,
    onDismiss: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareWithAttendeesClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    initialTab: Int = 0,
    onCopyTranscript: () -> Unit = {},
    onSplitTranscript: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTab) }
    val tabs = listOf("Summary", "Notes", "Transcript")

    TmModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            TmBottomSheetHeader(
                onClose = onDismiss,
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTabIndex) {
                    0 -> SummaryTabContent(
                        summaryText = summaryText,
                        keyPoints = keyPoints,
                        actionItems = actionItems,
                        isLoading = isLoading,
                        onShareWithAttendeesClick = onShareWithAttendeesClick,
                    )
                    1 -> NotesTabContent(notesText = notesText)
                    2 -> TranscriptTabContent(segments = transcriptSegments)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            when (selectedTabIndex) {
                0 -> TmSummaryActionRow(
                    onCopyClick = onCopyClick,
                    onRegenerateClick = onRegenerateClick,
                    onEditClick = onEditClick,
                    onShareClick = onShareClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                2 -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onCopyTranscript) {
                        Icon(
                            imageVector = TmIcons.Copy,
                            contentDescription = "Copy transcript",
                            tint = TwinMindDarkNavy,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = onSplitTranscript,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, TwinMindGray.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = TmIcons.Scissors,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TwinMindDarkNavy,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Split",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TwinMindDarkNavy,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTabContent(
    summaryText: String,
    keyPoints: List<String>,
    actionItems: List<ActionItem>,
    isLoading: Boolean,
    onShareWithAttendeesClick: () -> Unit,
) {
    TmShareBanner(
        text = "Share summary with attendees",
        onShareClick = onShareWithAttendeesClick,
        onDismiss = {},
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (isLoading && summaryText.isBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = TwinMindTeal,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Generating summary...",
                fontSize = 15.sp,
                color = TwinMindGray,
            )
        }
    } else {
        Text(
            text = summaryText.ifEmpty { "Summary will appear here..." },
            fontSize = 15.sp,
            color = TwinMindDarkNavy,
            lineHeight = 22.sp,
        )

        if (keyPoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            keyPoints.forEach { point ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "•",
                        fontSize = 15.sp,
                        color = TwinMindDarkNavy,
                        modifier = Modifier.padding(end = 8.dp, top = 0.dp),
                    )
                    Text(
                        text = point,
                        fontSize = 14.sp,
                        color = TwinMindDarkNavy,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = TwinMindGray.copy(alpha = 0.3f))
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Action Items",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TwinMindDarkNavy,
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (actionItems.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = false,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(uncheckedColor = TwinMindGray),
            )
            Text(
                text = "No action items identified",
                fontSize = 14.sp,
                color = TwinMindGray,
            )
        }
    } else {
        actionItems.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = TwinMindTeal),
                )
                Text(
                    text = item.text,
                    fontSize = 14.sp,
                    color = TwinMindDarkNavy,
                )
            }
        }
    }
}

@Composable
private fun NotesTabContent(notesText: String) {
    Text(
        text = notesText.ifEmpty { "No notes captured." },
        fontSize = 15.sp,
        color = TwinMindDarkNavy,
        lineHeight = 22.sp,
    )
}

@Composable
private fun TranscriptTabContent(segments: List<TranscriptSegment>) {
    if (segments.isEmpty()) {
        Text(
            text = "No transcript available.",
            fontSize = 15.sp,
            color = TwinMindGray,
        )
        return
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    segments.forEachIndexed { index, segment ->
        if (index > 0) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = TwinMindGray.copy(alpha = 0.2f),
            )
        }
        Text(
            text = timeFormat.format(Date(segment.timestampMs)),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TwinMindTeal,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = segment.text,
            fontSize = 15.sp,
            color = TwinMindDarkNavy,
            lineHeight = 22.sp,
        )
    }
}

data class ActionItem(
    val text: String,
    val isCompleted: Boolean = false,
)
