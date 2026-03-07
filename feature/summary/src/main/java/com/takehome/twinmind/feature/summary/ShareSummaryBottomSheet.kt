package com.takehome.twinmind.feature.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmIcons
import com.takehome.twinmind.core.designsystem.component.TmModalBottomSheet
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindLightGray
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSummaryBottomSheet(
    onDismiss: () -> Unit,
    onAddEmail: (String) -> Unit,
    onSelectAttendeesClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var emailInput by rememberSaveable { mutableStateOf("") }

    TmModalBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Close + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = TmIcons.Close,
                        contentDescription = "Close",
                        tint = TwinMindDarkNavy,
                    )
                }
                Text(
                    text = "Share Summary",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TwinMindTeal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                // Invisible spacer for centering
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mail icon
            Icon(
                imageVector = TmIcons.Mail,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = TwinMindDarkNavy,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enter emails to share",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TwinMindDarkNavy,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Enter emails below to share the summary and actio...",
                fontSize = 14.sp,
                color = TwinMindGray,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Email input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(text = "Enter email", color = TwinMindGray)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TwinMindTeal,
                        unfocusedBorderColor = TwinMindLightGray,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            onAddEmail(emailInput)
                            emailInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TwinMindLightGray),
                ) {
                    Text(
                        text = "Add",
                        color = TwinMindGray,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Select attendees button
            Button(
                onClick = onSelectAttendeesClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TwinMindDarkNavy,
                    contentColor = TwinMindWhite,
                ),
            ) {
                Icon(
                    imageVector = TmIcons.Mail,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select attendees to share",
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Copy link button
            OutlinedButton(
                onClick = onCopyLinkClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, TwinMindTeal),
            ) {
                Icon(
                    imageVector = TmIcons.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = TwinMindTeal,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Copy link",
                    color = TwinMindTeal,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
