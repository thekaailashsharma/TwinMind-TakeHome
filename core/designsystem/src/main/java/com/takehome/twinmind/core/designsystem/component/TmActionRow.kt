package com.takehome.twinmind.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun TmSummaryActionRow(
    onCopyClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCopyClick) {
            Icon(
                imageVector = TmIcons.Copy,
                contentDescription = "Copy",
                tint = TwinMindDarkNavy,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onRegenerateClick) {
            Icon(
                imageVector = TmIcons.Refresh,
                contentDescription = "Regenerate",
                tint = TwinMindDarkNavy,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onEditClick,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, TwinMindGray),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = TmIcons.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = TwinMindDarkNavy,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Edit",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TwinMindDarkNavy,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onShareClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TwinMindDarkNavy,
                contentColor = TwinMindWhite,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = TmIcons.Mail,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Share",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
