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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmModalBottomSheet
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal

private val ActionItemYellow = Color(0xFFFFF9E6)
private val ActionItemYellowBorder = Color(0xFFE8DFB8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionItemsReviewSheet(
    actionItems: List<String>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val checkedStates = remember(actionItems) {
        mutableStateListOf(*Array(actionItems.size) { false })
    }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = TmIcons.CheckboxBlank,
                    contentDescription = null,
                    tint = TwinMindTeal,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tasks to Review",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TwinMindDarkNavy,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = TmIcons.Close,
                        contentDescription = "Close",
                        tint = TwinMindDarkNavy,
                    )
                }
            }

            HorizontalDivider(color = TwinMindGray.copy(alpha = 0.2f))

            // Action items list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                actionItems.forEachIndexed { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ActionItemYellow),
                        border = BorderStroke(1.dp, ActionItemYellowBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checkedStates.getOrElse(index) { false },
                                onCheckedChange = { checked ->
                                    if (index < checkedStates.size) {
                                        checkedStates[index] = checked
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = TwinMindTeal,
                                    uncheckedColor = TwinMindGray,
                                ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                fontSize = 14.sp,
                                color = TwinMindDarkNavy,
                                lineHeight = 20.sp,
                                textDecoration = if (checkedStates.getOrElse(index) { false }) {
                                    TextDecoration.LineThrough
                                } else {
                                    TextDecoration.None
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Add task button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TmIcons.Add,
                        contentDescription = null,
                        tint = TwinMindTeal,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add task",
                        fontSize = 14.sp,
                        color = TwinMindTeal,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Bottom bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, TwinMindGray.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = "Clear All",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TwinMindDarkNavy,
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TwinMindTeal),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = "See To-do List",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
