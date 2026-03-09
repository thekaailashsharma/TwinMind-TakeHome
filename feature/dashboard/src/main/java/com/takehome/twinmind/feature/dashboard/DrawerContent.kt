package com.takehome.twinmind.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.takehome.twinmind.core.designsystem.R as DesignR
import com.takehome.twinmind.core.designsystem.component.TmDrawerMenuItem
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmProBadge
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindOrange
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun DrawerContent(
    userName: String,
    userEmail: String,
    userPhotoUrl: String?,
    isPro: Boolean,
    onViewNotesClick: () -> Unit,
    onViewChatsClick: () -> Unit,
    onPersonalizationClick: () -> Unit,
    onUploadAudioClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F4F7))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Profile section
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (userPhotoUrl != null) {
                AsyncImage(
                    model = userPhotoUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
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

        // "View Notes & Chats" search bar (navigates to Memories)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable(onClick = onViewNotesClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E6EA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "View Notes & Chats",
                    fontSize = 14.sp,
                    color = TwinMindGray,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = TmIcons.Search,
                    contentDescription = null,
                    tint = TwinMindGray,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TmDrawerMenuItem(
            icon = TmIcons.Folder,
            text = "Notes",
            onClick = onViewNotesClick,
        )
        TmDrawerMenuItem(
            icon = TmIcons.Chat,
            text = "Chats",
            onClick = onViewChatsClick,
        )

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
        )
        TmDrawerMenuItem(
            icon = TmIcons.Logout,
            text = "Sign Out",
            onClick = onSignOutClick,
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
                .height(180.dp),
        ) {
            Image(
                painter = painterResource(id = DesignR.drawable.astronaut),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text(
                    text = "Gift TwinMind Pro\nto a friend to extend\nyour Pro plan",
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
