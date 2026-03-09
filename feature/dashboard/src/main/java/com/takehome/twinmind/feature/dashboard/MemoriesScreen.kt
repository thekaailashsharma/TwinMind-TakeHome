package com.takehome.twinmind.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindLightGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal

private val ScreenBg = Color(0xFFFAF8F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    state: MemoriesUiState,
    onBackClick: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onSearchChanged: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNotes = state.selectedTab == 0
    val placeholder = if (isNotes) "Search Notes" else "Search Chats"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Memories",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TwinMindDarkNavy,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = TmIcons.Back,
                            contentDescription = "Back",
                            tint = TwinMindDarkNavy,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBg),
            )
        },
        containerColor = ScreenBg,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = TwinMindGray) },
                trailingIcon = {
                    Icon(
                        imageVector = TmIcons.Search,
                        contentDescription = null,
                        tint = TwinMindGray,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TwinMindTeal,
                    unfocusedBorderColor = TwinMindLightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SegmentedChip(
                    text = "Notes",
                    selected = state.selectedTab == 0,
                    onClick = { onTabSelected(0) },
                )
                SegmentedChip(
                    text = "Chats",
                    selected = state.selectedTab == 1,
                    onClick = { onTabSelected(1) },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val groups = if (isNotes) state.notesGroups else state.chatGroups

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                groups.forEach { group ->
                    item(key = "h_${group.header}") {
                        Text(
                            text = group.header,
                            fontSize = 12.sp,
                            color = TwinMindGray,
                            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                        )
                    }
                    items(group.items, key = { it.sessionId + "_" + it.isChat }) { item ->
                        MemoriesRow(
                            item = item,
                            onClick = {
                                if (item.isChat) onChatClick(item.sessionId) else onSessionClick(item.sessionId)
                            },
                        )
                    }
                }

                if (groups.isEmpty()) {
                    item(key = "empty") {
                        Spacer(modifier = Modifier.height(40.dp))
                        Text(
                            text = if (isNotes) "No notes yet" else "No chats yet",
                            fontSize = 14.sp,
                            color = TwinMindGray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) Color(0xFFEAF2F3) else Color.White
    val border = if (selected) Color(0xFFBFD8DC) else Color(0xFFE8E8E8)
    val fg = if (selected) TwinMindDarkNavy else TwinMindGray

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
        )
    }
}

@Composable
private fun MemoriesRow(
    item: MemoriesItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E8E8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = if (item.isChat) TmIcons.Chat else TmIcons.Folder
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F3F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TwinMindGray,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TwinMindDarkNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = TwinMindGray,
                    maxLines = if (item.isChat) 1 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!item.isChat) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F7F8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = TmIcons.Share,
                        contentDescription = "Share",
                        tint = TwinMindGray,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp),
                    )
                }
            }
        }
    }
}

