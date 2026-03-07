package com.takehome.twinmind.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindRecordingRed
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun TmRecordingBar(
    elapsedTime: String,
    onChatClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRecording: Boolean = true,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.White,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Chat button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onChatClick),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFE0E0E0),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = TmIcons.Sparkle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TwinMindDarkNavy,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Chat",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TwinMindDarkNavy,
                    )
                }
            }

            // Center timer with waveform
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = TwinMindDarkNavy,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isRecording) {
                        WaveformBars()
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = elapsedTime,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TwinMindWhite,
                    )
                }
            }

            // Stop button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onStopClick),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFE0E0E0),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(TwinMindRecordingRed),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stop",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TwinMindDarkNavy,
                    )
                }
            }
        }
    }
}

@Composable
private fun WaveformBars(
    modifier: Modifier = Modifier,
    barCount: Int = 4,
) {
    val transition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(barCount) { index ->
            val height by transition.animateFloat(
                initialValue = 6f,
                targetValue = 18f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (index * 100),
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar_$index",
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(TwinMindWhite),
            )
        }
    }
}
