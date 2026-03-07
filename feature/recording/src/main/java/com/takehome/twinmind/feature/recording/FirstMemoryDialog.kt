package com.takehome.twinmind.feature.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmCelebrationDialog
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmPrimaryButton
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindOrange
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun FirstMemoryDialog(
    onDismiss: () -> Unit,
    onLetsGoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TmCelebrationDialog(
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        // Icon with sparkle effect
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TwinMindTeal.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = TmIcons.Waveform,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = TwinMindTeal,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You just captured your\n1st memory!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TwinMindOrange,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try asking something to\n\"Ask TwinMind\"!",
            fontSize = 14.sp,
            color = TwinMindGray,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        TmPrimaryButton(
            text = "Let's go!",
            onClick = onLetsGoClick,
            backgroundColor = TwinMindDarkNavy,
            contentColor = TwinMindWhite,
        )
    }
}
