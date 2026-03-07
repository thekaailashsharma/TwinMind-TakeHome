package com.takehome.twinmind.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.takehome.twinmind.core.designsystem.theme.TwinMindLightGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun TmCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = TwinMindWhite,
    borderColor: Color = TwinMindLightGray,
    elevation: Dp = 0.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val colors = CardDefaults.cardColors(containerColor = backgroundColor)
    val border = BorderStroke(1.dp, borderColor)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        ) {
            Column(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        ) {
            Column(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}

@Composable
fun TmAccentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    content: @Composable ColumnScope.() -> Unit,
) {
    TmCard(
        modifier = modifier,
        onClick = onClick,
        backgroundColor = accentColor.copy(alpha = 0.08f),
        borderColor = accentColor.copy(alpha = 0.2f),
        content = content,
    )
}
