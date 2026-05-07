package com.example.studyapp.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.util.loadAssetImage
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // ── Animation states ──────────────────────────────────────────────────────
    var phase by remember { mutableStateOf(0) }
    // phase 0 = start, 1 = book appears, 2 = text appears, 3 = exit

    LaunchedEffect(Unit) {
        delay(100);  phase = 1   // book bounces in
        delay(500);  phase = 2   // text fades in
        delay(900);  phase = 3   // exit scale up + fade out
        delay(400);  onFinished()
    }

    // Book: scale bounce in
    val bookScale by animateFloatAsState(
        targetValue = when (phase) {
            0    -> 0f
            1, 2 -> 1f
            else -> 1.15f
        },
        animationSpec = when (phase) {
            1    -> spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            3    -> tween(300, easing = FastOutLinearInEasing)
            else -> tween(200)
        },
        label = "bookScale"
    )

    // Book: translate Y — drop from above then settle
    val bookOffsetY by animateFloatAsState(
        targetValue = when (phase) {
            0    -> -120f
            1, 2 -> 0f
            else -> -20f
        },
        animationSpec = when (phase) {
            1    -> spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            3    -> tween(300)
            else -> tween(200)
        },
        label = "bookY"
    )

    // Book: rotation — slight tilt on entry
    val bookRotation by animateFloatAsState(
        targetValue = when (phase) {
            0    -> -15f
            1, 2 -> 0f
            else -> 5f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bookRot"
    )

    // Text alpha
    val textAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(400),
        label = "textAlpha"
    )

    // Text slide up
    val textOffsetY by animateFloatAsState(
        targetValue = if (phase >= 2) 0f else 20f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "textY"
    )

    // Subtitle alpha (delayed)
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 0.7f else 0f,
        animationSpec = tween(500, delayMillis = 150),
        label = "subAlpha"
    )

    // Overall screen alpha for exit
    val screenAlpha by animateFloatAsState(
        targetValue = if (phase >= 3) 0f else 1f,
        animationSpec = tween(350),
        label = "screenAlpha"
    )

    // Sparkle infinite rotation
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "sparkleScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(
                Brush.verticalGradient(
                    listOf(
                        ScPrimaryContainer.copy(alpha = 0.4f),
                        ScBackground,
                        ScSecondaryContainer.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Book icon ──
            Box(contentAlignment = Alignment.Center) {
                // Glow ring behind book
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(bookScale * 0.9f)
                        .background(
                            Brush.radialGradient(
                                listOf(ScPrimaryContainer.copy(alpha = 0.5f), ScBackground.copy(alpha = 0f))
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )

                // Book image
                Image(
                    painter = loadAssetImage("book.png"),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = bookScale
                            scaleY = bookScale
                            translationY = bookOffsetY
                            rotationZ = bookRotation
                        }
                )

                // Sparkle top-right
                if (phase >= 1) {
                    Image(
                        painter = loadAssetImage("sparkles.png"),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .scale(bookScale * sparkleScale)
                            .alpha(if (phase >= 2) 1f else 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── App name ──
            Text(
                "KMAStudy",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = ScOnSurface,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .alpha(textAlpha)
                    .graphicsLayer { translationY = textOffsetY }
            )

            Spacer(Modifier.height(8.dp))

            // ── Tagline ──
            Text(
                "Học thông minh hơn mỗi ngày ✨",
                style = MaterialTheme.typography.bodyMedium,
                color = ScOnSurfaceVariant,
                modifier = Modifier.alpha(subtitleAlpha)
            )
        }

        // ── Bottom loading dots ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(textAlpha),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                val dotScale by infiniteTransition.animateFloat(
                    initialValue = 0.6f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(500, delayMillis = i * 150, easing = EaseInOutSine),
                        RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dotScale)
                        .background(ScPrimary.copy(alpha = 0.6f), shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}
