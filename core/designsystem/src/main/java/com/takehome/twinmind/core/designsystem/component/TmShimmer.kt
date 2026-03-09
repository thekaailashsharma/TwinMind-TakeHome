package com.takehome.twinmind.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TmShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE8E8E8),
            Color(0xFFF5F5F5),
            Color(0xFFE8E8E8),
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim + 200f, 0f),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(shimmerBrush),
    )
}

@Composable
fun TmShimmerCard(
    modifier: Modifier = Modifier,
    lineCount: Int = 3,
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        repeat(lineCount) { index ->
            TmShimmerBox(
                modifier = Modifier.fillMaxWidth(if (index == lineCount - 1) 0.6f else 1f),
                height = 14.dp,
            )
            if (index < lineCount - 1) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
