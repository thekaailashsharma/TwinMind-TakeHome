package com.takehome.twinmind.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmDrawerMenuItem
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmProBadge
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindLightGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindOrange
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun DrawerContent(
    userName: String,
    userEmail: String,
    isPro: Boolean,
    onPersonalizationClick: () -> Unit,
    onUploadAudioClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile section
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TwinMindTeal),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TwinMindWhite,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TwinMindDarkNavy,
                    )
                    if (isPro) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TmProBadge()
                    }
                }
                if (userEmail.isNotEmpty()) {
                    Text(
                        text = userEmail,
                        fontSize = 13.sp,
                        color = TwinMindGray,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = {
                Text(
                    text = "View Notes & Chats",
                    color = TwinMindGray,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = TmIcons.Search,
                    contentDescription = "Search",
                    tint = TwinMindDarkNavy,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TwinMindDarkNavy,
                unfocusedBorderColor = TwinMindLightGray,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Menu items
        TmDrawerMenuItem(
            icon = TmIcons.Personalize,
            text = "Personalization & Language",
            onClick = onPersonalizationClick,
        )
        TmDrawerMenuItem(
            icon = TmIcons.Chat,
            text = "Contact us on Discord",
            onClick = {},
        )
        TmDrawerMenuItem(
            icon = TmIcons.Desktop,
            text = "Get TwinMind for ",
            accentText = "Desktop",
            onClick = {},
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Promo card
        PromoCard(
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        TmDrawerMenuItem(
            icon = TmIcons.Upload,
            text = "Upload Audio Files",
            onClick = onUploadAudioClick,
        )
        TmDrawerMenuItem(
            icon = TmIcons.Settings,
            text = "Settings",
            onClick = onSettingsClick,
            showDivider = false,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PromoCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1A3A4A),
                            Color(0xFF2A5A6A),
                            Color(0xFF3A7A8A),
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = "Gift TwinMind Pro to a friend to extend your Pro plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TwinMindWhite,
                    lineHeight = 22.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                    ),
                    border = BorderStroke(1.dp, TwinMindOrange.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "🎁",
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Give & Get Pro →",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TwinMindDarkNavy,
                        )
                    }
                }
            }
        }
    }
}
