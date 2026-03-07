package com.takehome.twinmind.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = TwinMindTeal,
    onPrimary = TwinMindWhite,
    primaryContainer = Color(0xFFD0E8EF),
    onPrimaryContainer = TwinMindDarkNavy,
    secondary = TwinMindOrange,
    onSecondary = TwinMindWhite,
    background = TwinMindWhite,
    onBackground = TwinMindDarkNavy,
    surface = TwinMindWhite,
    onSurface = TwinMindDarkNavy,
    surfaceVariant = TwinMindOffWhite,
    onSurfaceVariant = TwinMindDarkGray,
    outline = TwinMindLightGray,
    error = TwinMindError,
    onError = TwinMindWhite,
)

private val DarkColorScheme = darkColorScheme(
    primary = TwinMindTeal,
    onPrimary = TwinMindWhite,
    primaryContainer = TwinMindDarkNavy,
    onPrimaryContainer = Color(0xFFD0E8EF),
    secondary = TwinMindOrange,
    onSecondary = TwinMindWhite,
    background = Color(0xFF121212),
    onBackground = TwinMindWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = TwinMindWhite,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = TwinMindLightGray,
    outline = TwinMindDarkGray,
    error = Color(0xFFEF5350),
    onError = TwinMindWhite,
)

@Composable
fun TwinMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TwinMindTypography,
        content = content,
    )
}
