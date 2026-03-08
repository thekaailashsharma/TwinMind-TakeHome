package com.takehome.twinmind.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Pixel-perfect palette based on Image 1
private val SkyBlueTop = Color(0xFF007AFF) // Vibrant Apple-like blue
private val SkyBlueMid = Color(0xFF3FA9F5)
private val SkyBlueBottom = Color(0xFF6DD5FA) // Lighter cyan-blue

private val SunsetOrangeGlow = Color(0xFFFF9F5B) // Warm orange glow
private val SunsetPeachGlow = Color(0xFFFFC896)

@Composable
fun SignInScreen(
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Vibrant Gradient Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SkyBlueTop,
                            SkyBlueMid,
                            SkyBlueBottom,
                        )
                    )
                )
        )

        // 2. Sunset Glow & Arc
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .align(Alignment.BottomCenter),
        ) {
            // Strong Orange Glow
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        SunsetOrangeGlow.copy(alpha = 0.8f),
                        SunsetPeachGlow,
                        Color.White
                    ),
                    startY = 0f,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height * 0.2f),
                size = Size(size.width, size.height)
            )

            // Crisp White Arc
            val arcHeight = 90.dp.toPx()
            val arcWidth = size.width * 1.4f
            drawOval(
                color = Color.White,
                topLeft = Offset((size.width - arcWidth) / 2, size.height - arcHeight),
                size = Size(arcWidth, arcHeight * 2.5f)
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
            Spacer(modifier = Modifier.height(60.dp))

            // First laurel badge
            LaurelBadgeSection(
                headline = "350,000+",
                subtitle = "students & professionals",
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Second laurel badge
            LaurelBadgeSection(
                headline = "#1 Privacy",
                subtitle = "we don't use your data for training",
                secondaryText = "HIPAA & SOC2 compliant",
            )

            Spacer(modifier = Modifier.weight(1f))

            // Sign In Header
            Text(
                text = "Sign In to Continue",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Terms & Privacy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.9f))) {
                            append("I accept the ")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append("Privacy Policy")
                        }
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.9f))) {
                            append("\nand ")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append("Terms of Service")
                        }
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.White.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f),
                        uncheckedBorderColor = Color.Transparent,
                        checkedBorderColor = Color.Transparent
                    ),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Button (White/Opaque)
            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                enabled = acceptedTerms,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.95f), // Almost opaque white
                    contentColor = Color(0xFF555555), // Dark gray text
                    disabledContainerColor = Color.White.copy(alpha = 0.5f),
                    disabledContentColor = Color(0xFF555555).copy(alpha = 0.5f),
                ),
                elevation = null,
            ) {
                // Colored G Logo
                Text(
                    text = "G", 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (acceptedTerms) Color(0xFF4285F4) else Color(0xFF4285F4).copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Apple Button (Glass/Translucent)
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                enabled = false, // Visual only
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.25f), // Glassy
                    disabledContainerColor = Color.White.copy(alpha = 0.25f),
                    disabledContentColor = Color.White,
                ),
                elevation = null,
            ) {
                 Text(
                    text = "", 
                    fontSize = 24.sp, 
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Apple",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(80.dp)) // Space for the arc
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
        // Left Wreath
        LaurelWreath(isLeft = true)

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(
                text = headline,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            if (secondaryText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondaryText,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Wreath
        LaurelWreath(isLeft = false)
    }
}

@Composable
private fun LaurelWreath(
    isLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(40.dp, 80.dp)) {
        val w = size.width
        val h = size.height
        val leafColor = Color.White.copy(alpha = 0.7f)
        
        // Transform for right side (mirror)
        withTransform({
            if (!isLeft) {
                scale(-1f, 1f, pivot = Offset(w / 2, h / 2))
            }
        }) {
            // Draw Stem
            // Curves from bottom-right to top-left (in local coords for Left side)
            // Left laurel: ) shape. Convex to left.
            
            // Draw Leaves
            val leafCount = 7
            for (i in 0 until leafCount) {
                val t = i.toFloat() / (leafCount - 1)
                
                // Quadratic Bezier Curve for Stem
                // P0: Bottom Right (Start)
                // P1: Middle Left (Control)
                // P2: Top Center (End)
                val p0x = w * 0.9f; val p0y = h * 0.95f
                val p1x = w * 0.1f; val p1y = h * 0.5f
                val p2x = w * 0.6f; val p2y = h * 0.05f
                
                // Position B(t)
                val u = 1 - t
                val tt = t * t
                val uu = u * u
                
                val x = uu * p0x + 2 * u * t * p1x + tt * p2x
                val y = uu * p0y + 2 * u * t * p1y + tt * p2y
                
                // Tangent Vector B'(t)
                val tx = 2 * u * (p1x - p0x) + 2 * t * (p2x - p1x)
                val ty = 2 * u * (p1y - p0y) + 2 * t * (p2y - p1y)
                val tangentAngle = Math.toDegrees(atan2(ty.toDouble(), tx.toDouble())).toFloat()
                
                // Leaf Angle: Align with tangent + slight offset to fan out
                // Tangent points generally Up-Left.
                // We want leaves to look like they grow OUT of the stem.
                // Just using tangentAngle aligns them perfectly with the flow.
                
                drawLeaf(
                    cx = x,
                    cy = y,
                    size = 10.dp.toPx(),
                    angle = tangentAngle, 
                    color = leafColor
                )
            }
        }
    }
}

private fun DrawScope.drawLeaf(
    cx: Float,
    cy: Float,
    size: Float,
    angle: Float,
    color: Color,
) {
    rotate(degrees = angle, pivot = Offset(cx, cy)) {
        val path = Path().apply {
            // Leaf shape: ()
            moveTo(cx, cy + size/2)
            quadraticBezierTo(cx - size/2, cy, cx, cy - size/2)
            quadraticBezierTo(cx + size/2, cy, cx, cy + size/2)
            close()
        }
        drawPath(path, color, style = Fill)
    }
}
