package com.takehome.twinmind.feature.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.data.ai.GeminiService
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal

private val CreamBg = Color(0xFFFAF8F5)
private val AiBubbleColor = Color(0xFFF6F3EF)
private val UserBubbleColor = TwinMindTeal
private val ThinkingBgColor = Color(0xFFF0ECE6)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onBackClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    onToggleMemories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = TmIcons.Back,
                            contentDescription = "Back",
                            tint = TwinMindDarkNavy,
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TmIcons.Sparkle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = TwinMindTeal,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat with ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TwinMindDarkNavy,
                        )
                        Text(
                            text = "this note",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TwinMindTeal,
                        )
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showModelMenu = true }) {
                            Text(
                                text = state.currentModelName.removePrefix("gemini-"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TwinMindTeal,
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = TmIcons.ChevronRight,
                                contentDescription = "Select model",
                                tint = TwinMindTeal,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "2.5 Flash Lite",
                                        fontWeight = if (state.currentModelName == GeminiService.MODEL_FLASH) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onModelSelected(GeminiService.MODEL_FLASH)
                                    showModelMenu = false
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBg),
            )
        },
        containerColor = CreamBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            // Memories toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Memories",
                    fontSize = 13.sp,
                    color = TwinMindGray,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = state.memoriesEnabled,
                    onCheckedChange = { onToggleMemories() },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TwinMindTeal,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = TwinMindGray.copy(alpha = 0.3f),
                    ),
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = TmIcons.Sparkle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = TwinMindTeal.copy(alpha = 0.4f),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ask anything about this note",
                                fontSize = 16.sp,
                                color = TwinMindGray,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                state.suggestedPrompts.forEach { prompt ->
                                    SuggestedPromptChip(
                                        text = prompt,
                                        onClick = { onSendMessage(prompt) },
                                    )
                                }
                            }
                        }
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    when (message.role) {
                        "user" -> UserMessageBubble(message)
                        "model" -> AiMessageBubble(message)
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Ask about this note...", color = TwinMindGray, fontSize = 14.sp)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TwinMindTeal,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !state.isStreaming) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !state.isStreaming) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !state.isStreaming) TwinMindTeal
                            else TwinMindGray.copy(alpha = 0.3f),
                        ),
                ) {
                    Icon(
                        imageVector = TmIcons.Sparkle,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestedPromptChip(
    text: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            color = TwinMindDarkNavy,
        )
    }
}

@Composable
private fun UserMessageBubble(message: ChatMessageUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = UserBubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun AiMessageBubble(message: ChatMessageUi) {
    var showThinkingDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(0.9f),
    ) {
        // Thinking indicator
        if (message.thinkingSummary != null && message.thinkingDurationMs > 0) {
            val seconds = (message.thinkingDurationMs / 1000).coerceAtLeast(1)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThinkingDetails = !showThinkingDetails },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThinkingBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TmIcons.Sparkle,
                        contentDescription = null,
                        tint = TwinMindTeal.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Thought for ${seconds}s",
                        fontSize = 12.sp,
                        color = TwinMindGray,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = TmIcons.ChevronRight,
                        contentDescription = null,
                        tint = TwinMindGray,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = showThinkingDetails,
                enter = fadeIn() + expandVertically(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThinkingBgColor.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Text(
                        text = message.thinkingSummary,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = TwinMindGray,
                        lineHeight = 18.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Response bubble
        Card(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = AiBubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isStreaming && message.content.isBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TmIcons.Sparkle,
                            contentDescription = null,
                            tint = TwinMindTeal,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thinking...",
                            fontSize = 14.sp,
                            color = TwinMindGray,
                        )
                    }
                } else {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = TwinMindDarkNavy,
                        lineHeight = 20.sp,
                    )
                }

                if (!message.isStreaming && message.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = TmIcons.Copy,
                                contentDescription = "Copy",
                                tint = TwinMindGray,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {},
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = TmIcons.Refresh,
                                contentDescription = "Regenerate",
                                tint = TwinMindGray,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        if (message.modelName != null) {
            Text(
                text = message.modelName.removePrefix("gemini-"),
                fontSize = 10.sp,
                color = TwinMindGray.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
        }
    }
}
