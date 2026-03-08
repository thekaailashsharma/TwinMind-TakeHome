package com.takehome.twinmind.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object TmGradients {
    val SignIn = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color(0xFF5BAFE0),
            0.30f to Color(0xFF3D97CB),
            0.60f to Color(0xFF2B80B2),
            0.85f to Color(0xFF1F6E9C),
            1.00f to Color(0xFF1F6E9C),
        ),
    )

    val OnboardingBlue = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF5CB8D6),
            Color(0xFF3A9DC4),
            Color(0xFF2B8AB2),
        ),
    )

    val DashboardMist = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF0F4F7),
            Color(0xFFE8ECF0),
            Color(0xFFF5F5F5),
            Color.White,
        ),
    )
}

@Composable
fun TmGradientBackground(
    modifier: Modifier = Modifier,
    brush: Brush = TmGradients.SignIn,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush),
        content = content,
    )
}
