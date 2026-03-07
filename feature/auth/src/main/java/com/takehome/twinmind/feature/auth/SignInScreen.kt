package com.takehome.twinmind.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.takehome.twinmind.core.designsystem.component.TmGradientBackground
import com.takehome.twinmind.core.designsystem.component.TmGradients
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy
import com.takehome.twinmind.core.designsystem.theme.TwinMindOrange
import com.takehome.twinmind.core.designsystem.theme.TwinMindTeal
import com.takehome.twinmind.core.designsystem.theme.TwinMindWhite

@Composable
fun SignInScreen(
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    TmGradientBackground(
        modifier = modifier,
        brush = TmGradients.SignIn,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // First laurel badge — "350,000+"
            LaurelBadgeSection(
                headline = "350,000+",
                subtitle = "students & professionals",
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Second laurel badge — "#1 Privacy"
            LaurelBadgeSection(
                headline = "#1 Privacy",
                subtitle = "we don't use your data for training",
                secondaryText = "HIPAA & SOC2 compliant",
            )

            Spacer(modifier = Modifier.weight(1f))

            // Sign In section
            Text(
                text = "Sign In to Continue",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TwinMindWhite,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Terms toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("I accept the ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Privacy Policy")
                        }
                        append("\nand ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Terms of Service")
                        }
                    },
                    fontSize = 13.sp,
                    color = TwinMindWhite.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TwinMindWhite,
                        checkedTrackColor = TwinMindTeal,
                        uncheckedThumbColor = TwinMindWhite,
                        uncheckedTrackColor = TwinMindWhite.copy(alpha = 0.3f),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Google sign-in button
            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                enabled = acceptedTerms,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TwinMindWhite.copy(alpha = 0.85f),
                    contentColor = TwinMindDarkNavy,
                    disabledContainerColor = TwinMindWhite.copy(alpha = 0.3f),
                    disabledContentColor = TwinMindDarkNavy.copy(alpha = 0.4f),
                ),
            ) {
                Text(
                    text = "G",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (acceptedTerms) Color(0xFF4285F4) else Color(0xFF4285F4).copy(alpha = 0.4f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Decorative bottom arc
            BottomArc()
        }
    }
}

@Composable
private fun LaurelBadgeSection(
    headline: String,
    subtitle: String,
    secondaryText: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Left laurel
            LaurelWreath(mirrored = false)

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = headline,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TwinMindWhite,
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TwinMindWhite.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
                if (secondaryText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = secondaryText,
                        fontSize = 14.sp,
                        color = TwinMindWhite.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right laurel
            LaurelWreath(mirrored = true)
        }
    }
}

@Composable
private fun LaurelWreath(
    mirrored: Boolean,
    modifier: Modifier = Modifier,
) {
    val leafColor = TwinMindWhite.copy(alpha = 0.35f)

    Canvas(modifier = modifier.size(48.dp, 64.dp)) {
        val centerX = if (mirrored) size.width * 0.3f else size.width * 0.7f
        val leafCount = 6

        for (i in 0 until leafCount) {
            val fraction = i.toFloat() / (leafCount - 1)
            val y = size.height * 0.1f + fraction * size.height * 0.8f

            val curveOffsetX = if (mirrored) {
                size.width * 0.4f * (1f - (fraction - 0.5f) * (fraction - 0.5f) * 4f)
            } else {
                -size.width * 0.4f * (1f - (fraction - 0.5f) * (fraction - 0.5f) * 4f)
            }

            drawOval(
                color = leafColor,
                topLeft = Offset(
                    x = centerX + curveOffsetX - 6.dp.toPx(),
                    y = y - 4.dp.toPx(),
                ),
                size = Size(12.dp.toPx(), 8.dp.toPx()),
                style = Fill,
            )
        }
    }
}

@Composable
private fun BottomArc() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
    ) {
        drawOval(
            color = Color.White,
            topLeft = Offset(-size.width * 0.1f, 0f),
            size = Size(size.width * 1.2f, size.height * 2f),
        )
    }
}
