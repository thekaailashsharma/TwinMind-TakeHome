package com.takehome.twinmind.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindLightGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal

@Composable
fun TmDrawerMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentText: String? = null,
    showDivider: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = TwinMindDarkNavy,
        )
        Spacer(modifier = Modifier.width(14.dp))
        if (accentText != null) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TwinMindDarkNavy)) {
                        append(text)
                    }
                    withStyle(
                        SpanStyle(
                            color = TwinMindTeal,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append(accentText)
                    }
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TwinMindDarkNavy,
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = TwinMindLightGray,
        )
    }
}
