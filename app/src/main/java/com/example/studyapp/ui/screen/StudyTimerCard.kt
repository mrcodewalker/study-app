package com.example.studyapp.ui.screen

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.timer.StudyTimerService
import com.example.studyapp.timer.formatTime
import com.example.studyapp.ui.theme.*

// Preset thời gian hẹn giờ (phút)
private val PRESETS = listOf(15, 25, 30, 45, 60, 90)

@Composable
fun StudyTimerCard() {
    val context = LocalContext.current

    val elapsedMs   by StudyTimerService.elapsedMs.collectAsState()
    val isRunning   by StudyTimerService.isRunning.collectAsState()
    val isCountdown by StudyTimerService.isCountdown.collectAsState()
    val targetMs    by StudyTimerService.targetMs.collectAsState()
    val finished    by StudyTimerService.finished.collectAsState()

    // Trạng thái local
    var selectedPreset by remember { mutableStateOf<Int?>(null) }  // phút, null = stopwatch
    val isActive = isRunning || elapsedMs > 0L

    // Hiển thị dialog khi hết giờ
    if (finished) {
        AlertDialog(
            onDismissRequest = { StudyTimerService.resetFinished() },
            icon = { Text("⏰", fontSize = 32.sp) },
            title = { Text("Hết giờ!", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn đã học được ${formatTime(targetMs)}. Nghỉ ngơi một chút nhé!") },
            confirmButton = {
                Button(onClick = {
                    StudyTimerService.resetFinished()
                    stopTimer(context)
                }) { Text("Xong") }
            },
            dismissButton = {
                TextButton(onClick = {
                    StudyTimerService.resetFinished()
                    // Tiếp tục đếm stopwatch
                    startTimer(context, 0L)
                }) { Text("Tiếp tục học") }
            },
            containerColor = ScSurfaceContainerLowest,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = ScSurfaceContainerLowest,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(ScPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Timer, null, tint = ScPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Đồng hồ học tập", style = MaterialTheme.typography.titleSmall,
                        color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isCountdown && targetMs > 0) "Hẹn giờ ${formatTime(targetMs)}"
                        else "Đếm tự do",
                        style = MaterialTheme.typography.labelSmall, color = ScOnSurfaceVariant
                    )
                }
                // Badge trạng thái
                if (isRunning) {
                    Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFF22c55e).copy(alpha = 0.15f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
                                initialValue = 0.4f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                                label = "dot"
                            )
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                                .background(Color(0xFF22c55e).copy(alpha = pulse)))
                            Text("Đang chạy", style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF22c55e), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Đồng hồ hiển thị ─────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val displayMs = if (isCountdown && targetMs > 0)
                    (targetMs - elapsedMs).coerceAtLeast(0L)
                else elapsedMs

                val progress = if (isCountdown && targetMs > 0)
                    (elapsedMs.toFloat() / targetMs).coerceIn(0f, 1f)
                else 0f

                // Ring progress (chỉ hiện khi countdown)
                if (isCountdown && targetMs > 0) {
                    val animProg by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(400),
                        label = "ring"
                    )
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
                        val stroke = 8.dp.toPx()
                        val inset = stroke / 2
                        drawArc(
                            color = ScPrimaryContainer,
                            startAngle = -90f, sweepAngle = 360f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(ScPrimary, ScSecondary, ScPrimary)),
                            startAngle = -90f, sweepAngle = animProg * 360f, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatTime(displayMs),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = if (isCountdown && displayMs < 60_000L && displayMs > 0L) ScError
                                else ScOnSurface
                    )
                    if (isCountdown && targetMs > 0) {
                        Text(
                            "còn lại",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Preset chips (chỉ hiện khi chưa chạy) ────────────────────
            AnimatedVisibility(visible = !isActive) {
                Column {
                    Text("Hẹn giờ nhanh",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScOnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        // Chip "Tự do"
                        FilterChip(
                            selected = selectedPreset == null,
                            onClick = { selectedPreset = null },
                            label = { Text("Tự do") },
                            leadingIcon = {
                                Icon(Icons.Default.AllInclusive, null, modifier = Modifier.size(14.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ScPrimaryContainer,
                                selectedLabelColor = ScPrimary
                            )
                        )
                        PRESETS.forEach { min ->
                            FilterChip(
                                selected = selectedPreset == min,
                                onClick = { selectedPreset = min },
                                label = { Text("${min}p") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScPrimaryContainer,
                                    selectedLabelColor = ScPrimary
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Nút điều khiển ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isActive) {
                    // Bắt đầu
                    Button(
                        onClick = {
                            val targetMs = if (selectedPreset != null)
                                selectedPreset!! * 60_000L else 0L
                            startTimer(context, targetMs)
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bắt đầu", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Pause / Resume
                    FilledTonalButton(
                        onClick = {
                            if (isRunning) pauseTimer(context) else resumeTimer(context)
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isRunning) "Tạm dừng" else "Tiếp tục")
                    }
                    // Dừng
                    OutlinedButton(
                        onClick = { stopTimer(context) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ScError.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Stop, null, tint = ScError, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dừng", color = ScError)
                    }
                }
            }
        }
    }
}

private fun startTimer(context: Context, targetMs: Long) {
    val intent = Intent(context, StudyTimerService::class.java).apply {
        action = StudyTimerService.ACTION_START
        putExtra(StudyTimerService.EXTRA_TARGET_MS, targetMs)
    }
    context.startForegroundService(intent)
}

private fun pauseTimer(context: Context) {
    context.startService(Intent(context, StudyTimerService::class.java).apply {
        action = StudyTimerService.ACTION_PAUSE
    })
}

private fun resumeTimer(context: Context) {
    context.startService(Intent(context, StudyTimerService::class.java).apply {
        action = StudyTimerService.ACTION_RESUME
    })
}

private fun stopTimer(context: Context) {
    context.startService(Intent(context, StudyTimerService::class.java).apply {
        action = StudyTimerService.ACTION_STOP
    })
}
