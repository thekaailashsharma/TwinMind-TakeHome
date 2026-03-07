package com.takehome.twinmind.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.theme.TwinMindOrange
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun TmProBadge(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TwinMindOrange)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "PRO",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TwinMindWhite,
            letterSpacing = 0.5.sp,
        )
    }
}
