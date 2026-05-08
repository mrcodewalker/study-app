package com.example.studyapp.ui.screen

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.data.model.StudySession
import com.example.studyapp.timer.formatTime
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.StudySessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudySessionsScreen(
    viewModel: StudySessionViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sessions by viewModel.allSessions.collectAsState()

    // Calendar state
    var displayCal by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }

    val today = remember {
        Calendar.getInstance().let { Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH), it.get(Calendar.DAY_OF_MONTH)) }
    }

    // Group sessions by day
    val sessionsByDay = remember(sessions) {
        sessions.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.startedAt }
            Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        }
    }

    // Total duration per day (ms)
    val durationByDay = remember(sessionsByDay) {
        sessionsByDay.mapValues { (_, list) -> list.sumOf { it.durationMillis } }
    }

    // Sessions for selected day or all
    val displayedSessions = remember(sessions, selectedDate) {
        if (selectedDate == null) sessions
        else sessionsByDay[selectedDate] ?: emptyList()
    }

    // 7-day bar data
    val barData = remember(durationByDay) {
        (0..6).reversed().map { d ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }
            val key = Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            val label = if (d == 0) "Hnay" else SimpleDateFormat("EE", Locale("vi")).format(cal.time).replace("Th ", "T")
            Pair(label, durationByDay[key] ?: 0L)
        }
    }

    val totalMs = sessions.sumOf { it.durationMillis }
    val totalSessions = sessions.size

    Column(modifier = Modifier.fillMaxSize().background(ScBackground)) {
        // ── Header ────────────────────────────────────────────────────────
        Surface(color = ScSurfaceContainerLowest, shadowElevation = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = ScOnSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lịch sử học tập", style = MaterialTheme.typography.titleLarge,
                            color = ScOnSurface, fontWeight = FontWeight.Bold)
                        Text("$totalSessions phiên · Tổng ${formatTime(totalMs)}",
                            style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant)
                    }
                    // Share tổng kết
                    IconButton(onClick = {
                        val text = buildShareText(sessions)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Chia sẻ kết quả học"))
                    }) {
                        Icon(Icons.Default.Share, null, tint = ScPrimary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Summary chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryChip("⏱ ${formatTime(totalMs)}", "Tổng thời gian", ScPrimary, ScPrimaryContainer)
                    SummaryChip("📚 $totalSessions", "Phiên học", ScSecondary, ScSecondaryContainer)
                    val avgMs = if (totalSessions > 0) totalMs / totalSessions else 0L
                    SummaryChip("⌀ ${formatTime(avgMs)}", "Trung bình", ScTertiary, ScTertiaryContainer)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Biểu đồ 7 ngày ───────────────────────────────────────────
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    color = ScSurfaceContainerLowest, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("7 ngày gần nhất", style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        SessionBarChart(barData)
                    }
                }
            }

            // ── Calendar ─────────────────────────────────────────────────
            item {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    color = ScSurfaceContainerLowest, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Lịch học", style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        SessionCalendarView(
                            displayCal = displayCal,
                            durationByDay = durationByDay,
                            selectedDate = selectedDate,
                            today = today,
                            onMonthChange = { displayCal = it },
                            onDateSelected = { selectedDate = if (selectedDate == it) null else it }
                        )
                    }
                }
            }

            // ── Filter chip ───────────────────────────────────────────────
            if (selectedDate != null) {
                item {
                    val (y, m, d) = selectedDate!!
                    val label = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi"))
                        .format(Date(calendarDayOf(y, m, d)))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = ScPrimaryContainer) {
                            Text(label, color = ScPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { selectedDate = null }, contentPadding = PaddingValues(0.dp)) {
                            Text("Xem tất cả", color = ScOutline, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // ── Danh sách sessions ────────────────────────────────────────
            if (displayedSessions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TimerOff, null, tint = ScOnSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có phiên học nào", style = MaterialTheme.typography.bodyMedium, color = ScOnSurfaceVariant)
                        }
                    }
                }
            } else {
                items(displayedSessions, key = { it.id }) { session ->
                    SessionItem(
                        session = session,
                        onDelete = { viewModel.deleteSession(session) },
                        onShare = {
                            val text = buildSingleShareText(session)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Chia sẻ"))
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Session Item ──────────────────────────────────────────────────────────────

@Composable
private fun SessionItem(session: StudySession, onDelete: () -> Unit, onShare: () -> Unit) {
    val dateStr = remember(session.startedAt) {
        SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale("vi")).format(Date(session.startedAt))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ScSurfaceContainerLowest,
        border = BorderStroke(1.dp, ScOutlineVariant),
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon môn học
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(ScPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (session.subject.isNotBlank()) session.subject.take(1).uppercase() else "📚",
                    style = MaterialTheme.typography.titleMedium,
                    color = ScPrimary, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (session.subject.isNotBlank()) session.subject else "Phiên học",
                    style = MaterialTheme.typography.titleSmall,
                    color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = ScTertiaryContainer.copy(alpha = 0.6f)) {
                        Text(formatTime(session.durationMillis),
                            style = MaterialTheme.typography.labelSmall, color = ScTertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = ScOnSurfaceVariant)
                }
                if (session.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(session.note, style = MaterialTheme.typography.bodySmall,
                        color = ScOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, tint = ScError.copy(0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Session Calendar ──────────────────────────────────────────────────────────

@Composable
private fun SessionCalendarView(
    displayCal: Calendar,
    durationByDay: Map<Triple<Int, Int, Int>, Long>,
    selectedDate: Triple<Int, Int, Int>?,
    today: Triple<Int, Int, Int>,
    onMonthChange: (Calendar) -> Unit,
    onDateSelected: (Triple<Int, Int, Int>) -> Unit
) {
    val year = displayCal.get(Calendar.YEAR)
    val month = displayCal.get(Calendar.MONTH)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale("vi")).format(displayCal.time)
    val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK)
    val offset = (firstDow + 5) % 7

    // Max duration trong tháng để tính màu tương đối
    val maxDuration = remember(durationByDay, year, month) {
        (1..daysInMonth).mapNotNull { d -> durationByDay[Triple(year, month, d - 1)] }.maxOrNull() ?: 1L
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                onMonthChange((displayCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) })
            }) { Icon(Icons.Default.ChevronLeft, null, tint = ScOnSurfaceVariant) }
            Text(monthName, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall, color = ScOnSurface,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            IconButton(onClick = {
                onMonthChange((displayCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) })
            }) { Icon(Icons.Default.ChevronRight, null, tint = ScOnSurfaceVariant) }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("T2","T3","T4","T5","T6","T7","CN").forEach { d ->
                Text(d, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall, color = ScOutline,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))

        val rows = ((offset + daysInMonth) + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - offset + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val key = Triple(year, month, day)
                        val duration = durationByDay[key] ?: 0L
                        val isSelected = key == selectedDate
                        val isToday = key == today
                        val intensity = if (duration > 0) (duration.toFloat() / maxDuration).coerceIn(0.2f, 1f) else 0f

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> ScPrimary
                                        isToday -> ScPrimaryContainer
                                        duration > 0 -> ScTertiary.copy(alpha = intensity * 0.35f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(key) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("$day",
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isSelected -> Color.White
                                    isToday -> ScPrimary
                                    else -> ScOnSurface
                                },
                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp)
                            if (duration > 0) {
                                Text(formatTime(duration),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White.copy(0.85f) else ScTertiary,
                                    fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Session Bar Chart ─────────────────────────────────────────────────────────

@Composable
private fun SessionBarChart(barData: List<Pair<String, Long>>) {
    val maxMs = barData.maxOfOrNull { it.second }?.coerceAtLeast(60_000L) ?: 60_000L
    var selectedIdx by remember { mutableStateOf(-1) }

    Row(
        modifier = Modifier.height(100.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        barData.forEachIndexed { idx, (label, ms) ->
            val isSelected = selectedIdx == idx
            val isToday = idx == barData.size - 1
            val targetH = (ms.toFloat() / maxMs).coerceIn(0.05f, 1f)
            val animH by animateFloatAsState(targetValue = targetH, animationSpec = tween(800, easing = EaseOutBack), label = "h")
            val animW by animateDpAsState(targetValue = if (isSelected) 22.dp else 12.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "w")
            val barColor by animateColorAsState(
                targetValue = when {
                    isSelected -> ScSecondary
                    isToday -> ScTertiary
                    ms > 0 -> ScTertiary.copy(alpha = 0.7f)
                    else -> ScOutlineVariant.copy(alpha = 0.2f)
                }, animationSpec = tween(300), label = "c")

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { selectedIdx = if (selectedIdx == idx) -1 else idx }
            ) {
                Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight()
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (isSelected || isToday) ScTertiaryContainer.copy(0.4f) else ScOutlineVariant.copy(0.12f)))
                    Box(modifier = Modifier.width(animW).fillMaxHeight(animH)
                        .clip(RoundedCornerShape(99.dp)).background(barColor),
                        contentAlignment = Alignment.Center) {
                        val ta by animateFloatAsState(targetValue = if (isSelected && ms > 0) 1f else 0f,
                            animationSpec = tween(200), label = "ta")
                        if (ta > 0.01f) {
                            Text(formatTime(ms), style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(ta), fontWeight = FontWeight.Bold, fontSize = 7.sp)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected || isToday) ScTertiary else ScOnSurfaceVariant,
                    fontSize = 9.sp, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

// ── Summary Chip ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(value: String, label: String, accent: Color, bg: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = bg.copy(alpha = 0.6f)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent.copy(0.7f))
        }
    }
}

// ── Share helpers ─────────────────────────────────────────────────────────────

private fun buildShareText(sessions: List<StudySession>): String {
    val total = sessions.sumOf { it.durationMillis }
    val sb = StringBuilder()
    sb.appendLine("📚 KMAStudy — Kết quả học tập")
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
    sb.appendLine("⏱ Tổng thời gian: ${formatTime(total)}")
    sb.appendLine("📋 Số phiên học: ${sessions.size}")
    if (sessions.isNotEmpty()) {
        val avg = total / sessions.size
        sb.appendLine("⌀ Trung bình: ${formatTime(avg)}/phiên")
    }
    val subjects = sessions.filter { it.subject.isNotBlank() }.groupBy { it.subject }
    if (subjects.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("📖 Theo môn học:")
        subjects.forEach { (subj, list) ->
            sb.appendLine("  • $subj: ${formatTime(list.sumOf { it.durationMillis })}")
        }
    }
    sb.appendLine()
    sb.appendLine("🎯 Học đều đặn mỗi ngày để đạt mục tiêu!")
    return sb.toString()
}

private fun buildSingleShareText(session: StudySession): String {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi")).format(Date(session.startedAt))
    return buildString {
        appendLine("📚 KMAStudy — Phiên học")
        appendLine("━━━━━━━━━━━━━━━━━━━━")
        if (session.subject.isNotBlank()) appendLine("📖 Môn: ${session.subject}")
        appendLine("⏱ Thời gian: ${formatTime(session.durationMillis)}")
        appendLine("🗓 Ngày: $dateStr")
        if (session.note.isNotBlank()) appendLine("📝 Ghi chú: ${session.note}")
        appendLine()
        appendLine("💪 Tiếp tục cố gắng!")
    }
}
