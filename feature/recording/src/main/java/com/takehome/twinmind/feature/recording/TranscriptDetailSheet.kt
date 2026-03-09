package com.takehome.twinmind.feature.recording

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmBottomSheetHeader
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmModalBottomSheet
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal

data class TranscriptEntry(
    val timestamp: String,
    val text: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptDetailSheet(
    entries: List<TranscriptEntry>,
    userNotes: String,
    onDismiss: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(1) }
    val tabs = listOf("Notes", "Transcript")

    TmModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                    0 -> {
                        Text(
                            text = userNotes.ifEmpty { "No notes captured." },
                            fontSize = 15.sp,
                            color = TwinMindDarkNavy,
                            lineHeight = 22.sp,
                        )
                    }
                    1 -> {
                        if (entries.isEmpty()) {
                            Text(
                                text = "Transcript will appear here as you speak...",
                                fontSize = 15.sp,
                                color = TwinMindGray,
                            )
                        } else {
                            entries.forEachIndexed { index, entry ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = Color(0xFFF0F0F0),
                                    )
                                }
                                Text(
                                    text = entry.timestamp,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TwinMindTeal,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = entry.text,
                                    fontSize = 15.sp,
                                    color = TwinMindDarkNavy,
                                    lineHeight = 22.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedTabIndex == 1) {
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = TmIcons.Copy,
                        contentDescription = "Copy transcript",
                        tint = TwinMindDarkNavy,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
