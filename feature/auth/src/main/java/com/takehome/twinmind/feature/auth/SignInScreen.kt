package com.takehome.twinmind.feature.auth

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Exact palette sampled from the original screenshot
private val GradientTop = Color(0xFF5BAFE0)
private val GradientMid = Color(0xFF3D97CB)
private val GradientLow = Color(0xFF2B80B2)
private val GradientBottom = Color(0xFF1F6E9C)
private val LeafColor = Color(0xFFFFFFFF).copy(alpha = 0.30f)
private val LeafColorInner = Color(0xFFFFFFFF).copy(alpha = 0.20f)
private val OrangeArc = Color(0xFFE8944A)
private val PeachArc = Color(0xFFF2C19A)
private val ButtonFrost = Color(0xFFFFFFFF).copy(alpha = 0.75f)
private val ButtonFrostDisabled = Color(0xFFFFFFFF).copy(alpha = 0.25f)

@Composable
fun SignInScreen(
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Full-screen gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to GradientTop,
                        0.30f to GradientMid,
                        0.60f to GradientLow,
                        0.85f to GradientBottom,
                        1.00f to GradientBottom,
                    ),
                ),
            )
        }

        // Orange-to-white bottom arc
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter),
        ) {
            // Outer warm glow
            drawOval(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to OrangeArc.copy(alpha = 0.0f),
                        0.3f to OrangeArc.copy(alpha = 0.25f),
                        0.6f to PeachArc.copy(alpha = 0.5f),
                        1.0f to Color.White,
                    ),
                ),
                topLeft = Offset(-size.width * 0.15f, -size.height * 0.1f),
                size = Size(size.width * 1.3f, size.height * 2.2f),
            )
            // Inner bright white
            drawOval(
                color = Color.White,
                topLeft = Offset(-size.width * 0.1f, size.height * 0.35f),
                size = Size(size.width * 1.2f, size.height * 2f),
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // First laurel badge — "350,000+"
            LaurelBadgeSection(
                headline = "350,000+",
                subtitle = "students & professionals",
            )

            Spacer(modifier = Modifier.height(64.dp))

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
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Terms toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.85f))) {
                            append("I accept the ")
                        }
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        ) {
                            append("Privacy Policy")
                        }
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.85f))) {
                            append("\nand ")
                        }
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        ) {
                            append("Terms of Service")
                        }
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CAF50),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.35f),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google sign-in button — frosted glass pill
            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 28.dp),
                enabled = acceptedTerms,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonFrost,
                    contentColor = Color(0xFF555555),
                    disabledContainerColor = ButtonFrostDisabled,
                    disabledContentColor = Color.White.copy(alpha = 0.5f),
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                ),
            ) {
                Text(
                    text = "G",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (acceptedTerms) Color(0xFF4285F4) else Color(0xFF4285F4).copy(alpha = 0.35f),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (acceptedTerms) Color(0xFF666666) else Color.White.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun LaurelBadgeSection(
    headline: String,
    subtitle: String,
    secondaryText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        LaurelBranch(mirrored = false)

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(
                text = headline,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            if (secondaryText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        LaurelBranch(mirrored = true)
    }
}

@Composable
private fun LaurelBranch(
    mirrored: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(56.dp, 80.dp)) {
        val w = size.width
        val h = size.height
        val leafW = 10.dp.toPx()
        val leafH = 5.5.dp.toPx()
        val stemColor = Color.White.copy(alpha = 0.18f)

        val sign = if (mirrored) -1f else 1f
        val centerX = w / 2f

        // Draw curved stem
        val stemPath = Path()
        val stemPoints = 20
        for (i in 0..stemPoints) {
            val t = i.toFloat() / stemPoints
            val y = h * 0.05f + t * h * 0.9f
            val curve = sin(t * PI.toFloat()) * w * 0.25f * sign
            val x = centerX + curve
            if (i == 0) stemPath.moveTo(x, y) else stemPath.lineTo(x, y)
        }
        drawPath(
            path = stemPath,
            color = stemColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
        )

        // Draw leaves along the stem
        val leafCount = 7
        for (i in 0 until leafCount) {
            val t = (i + 0.5f) / leafCount
            val y = h * 0.05f + t * h * 0.9f
            val curve = sin(t * PI.toFloat()) * w * 0.25f * sign
            val x = centerX + curve

            // Angle leaves outward from stem
            val tangentAngle = cos(t * PI.toFloat()) * 45f * sign
            val leafAngle = if (mirrored) -30f - tangentAngle else 30f - tangentAngle

            val alpha = 0.22f + (1f - kotlin.math.abs(t - 0.5f) * 2f) * 0.15f
            drawLeaf(x, y, leafW, leafH, leafAngle, Color.White.copy(alpha = alpha))
        }
    }
}

private fun DrawScope.drawLeaf(
    cx: Float,
    cy: Float,
    w: Float,
    h: Float,
    angleDeg: Float,
    color: Color,
) {
    rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
        val path = Path().apply {
            moveTo(cx - w / 2, cy)
            cubicTo(
                cx - w / 4, cy - h,
                cx + w / 4, cy - h,
                cx + w / 2, cy,
            )
            cubicTo(
                cx + w / 4, cy + h,
                cx - w / 4, cy + h,
                cx - w / 2, cy,
            )
            close()
        }
        drawPath(path = path, color = color, style = Fill)
    }
}
