package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.data.model.TodoItem
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.FlashcardViewModel
import com.example.studyapp.ui.viewmodel.NoteViewModel
import com.example.studyapp.ui.viewmodel.TodoViewModel
import com.example.studyapp.ui.viewmodel.UserActivityViewModel
import java.util.*
import java.text.SimpleDateFormat
import kotlin.math.PI
import kotlin.math.cos
import com.example.studyapp.ui.util.loadAssetImage

@Composable
fun StatsScreen(
    flashcardViewModel: FlashcardViewModel,
    noteViewModel: NoteViewModel,
    todoViewModel: TodoViewModel,
    userActivityViewModel: UserActivityViewModel
) {
    val decks by flashcardViewModel.allDecks.collectAsState()
    val notes by noteViewModel.allNotes.collectAsState()
    val todos by todoViewModel.allTodos.collectAsState()
    val recentActivity by userActivityViewModel.recentActivity.collectAsState()
    val pendingCount by todoViewModel.pendingCount.collectAsState()

    val completedCount = todos.count { it.isCompleted }
    val totalCount = todos.size
    val todayProgress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    // Weekly bar data (mock based on real todos by day)
    val weekBars = remember(todos) { computeWeekBars(todos) }

    // Heatmap data for current year
    val heatmapData = remember(todos) { computeHeatmap(todos) }

    // Focus level by time of day (morning, afternoon, evening)
    val focusData = remember(todos) { computeFocusLevel(todos) }

    // Efficiency: compare this week vs last week
    val efficiency = remember(todos) { computeEfficiency(todos) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScSurfaceContainerLowest)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text("Thống kê", style = MaterialTheme.typography.headlineMedium,
                color = ScOnSurface, fontWeight = FontWeight.SemiBold)
            Text("Theo dõi tiến độ học tập của bạn",
                style = MaterialTheme.typography.bodyMedium, color = ScOnSurfaceVariant)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Stats summary grid ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatSummaryCard(
                    modifier = Modifier.weight(1f),
                    value = "$completedCount",
                    label = "Nhiệm vụ\nhoàn thành",
                    iconPath = "sparkles.png",
                    accent = ScPrimary,
                    bg = ScPrimaryContainer
                )
                StatSummaryCard(
                    modifier = Modifier.weight(1f),
                    value = "${decks.size}",
                    label = "Bộ thẻ\nđang học",
                    iconPath = "flash-card.png",
                    accent = ScSecondary,
                    bg = ScSecondaryContainer
                )
                StatSummaryCard(
                    modifier = Modifier.weight(1f),
                    value = formatDurationShort(recentActivity.firstOrNull()?.durationMillis ?: 0L),
                    label = "Thời gian\nhôm nay",
                    iconPath = "clock.png",
                    accent = Color(0xFF673AB7),
                    bg = Color(0xFFD1C4E9)
                )
            }

            // ── Circular progress + Weekly bar chart ──
            // Use IntrinsicSize.Max so both cards share the same height
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular progress (Stitch style)
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = ScSurfaceContainerLowest,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(loadAssetImage("clock.png"), null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hôm nay", style = MaterialTheme.typography.titleSmall,
                                color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                        }
                        Text("$completedCount/$totalCount nhiệm vụ",
                            style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        CircularProgressChart(
                            progress = todayProgress,
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = RoundedCornerShape(99.dp), color = ScPrimaryFixed) {
                            Text(
                                if (todayProgress >= 1f) "Hoàn thành!" else "Đang tiến hành",
                                color = ScOnPrimaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Weekly bar chart — fillMaxHeight so it matches the circular card
                Surface(
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = ScSurfaceContainerLowest,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxHeight()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(loadAssetImage("analysis.png"), null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tiến độ tuần",
                                style = MaterialTheme.typography.titleSmall,
                                color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                        }
                        Text("Số việc hoàn thành/ngày",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScOnSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        // fillMaxHeight(1f) so bars stretch to fill remaining space
                        WeekBarChart(
                            bars = weekBars,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }

            // ── GitHub-style heatmap ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ScSurfaceContainerLowest,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hoạt động học tập",
                            style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            Calendar.getInstance().get(Calendar.YEAR).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ScOnSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ActivityHeatmap(
                        data = heatmapData,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ít", style = MaterialTheme.typography.labelSmall,
                            color = ScOnSurfaceVariant, fontSize = 10.sp)
                        Spacer(Modifier.width(4.dp))
                        listOf(
                            ScSurfaceContainerHigh,
                            ScPrimaryFixed,
                            ScPrimaryContainer,
                            ScPrimary
                        ).forEach { color ->
                            Box(
                                modifier = Modifier.size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                            Spacer(Modifier.width(2.dp))
                        }
                        Text("Nhiều", style = MaterialTheme.typography.labelSmall,
                            color = ScOnSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }

            // ── Line chart: Focus level ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ScSurfaceContainerLowest,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(loadAssetImage("sparkles.png"), null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Thời điểm làm việc hiệu quả",
                            style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    FocusLineChart(
                        focusData = focusData,
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            Pair(ScTertiary, "Sáng"),
                            Pair(ScPrimary, "Chiều"),
                            Pair(ScSecondary, "Tối")
                        ).forEach { (color, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                                Spacer(Modifier.width(4.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── Usage Chart ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = ScSurfaceContainerLowest,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(loadAssetImage("clock.png"), null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Thời gian sử dụng (7 ngày)",
                            style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(16.dp))
                    UsageChart(recentActivity)
                }
            }

            // ── Efficiency bento cards ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = ScPrimaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Default.TrendingUp, null,
                            tint = ScOnPrimaryContainer,
                            modifier = Modifier.size(32.dp))
                        Column {
                            Text("HIỆU SUẤT",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = ScOnPrimaryContainer)
                            val sign = if (efficiency >= 0) "+" else ""
                            Text("$sign$efficiency%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ScOnPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = ScSurfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Image(loadAssetImage("fire.png"), null,
                            modifier = Modifier.size(40.dp))
                        Column {
                            Text("STREAK",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = ScOnSurfaceVariant)
                            Text("${computeStreak(todos)} ngày",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ── Circular Progress Chart ───────────────────────────────────────────────────

@Composable
fun CircularProgressChart(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )
    val sweepAngle = animatedProgress * 360f
    val pct = (animatedProgress * 100).toInt()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = ScPrimaryContainer, radius = radius, center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round))
            drawArc(color = ScPrimary, startAngle = -90f, sweepAngle = sweepAngle,
                useCenter = false, style = Stroke(width = stroke, cap = StrokeCap.Round))
        }
        Text("$pct%", style = MaterialTheme.typography.titleMedium,
            color = ScPrimary, fontWeight = FontWeight.Bold)
    }
}

// ── Week Bar Chart ────────────────────────────────────────────────────────────

@Composable
fun WeekBarChart(bars: List<Pair<String, Float>>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { (label, value) ->
            val isWeekend = label == "T7" || label == "CN"
            val barColor = if (isWeekend) ScSecondary else ScPrimary
            val trackColor = if (isWeekend) ScSecondaryContainer else ScPrimaryContainer

Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Track
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(99.dp))
                            .background(trackColor.copy(alpha = 0.3f))
                    )
                    // Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(value.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(99.dp))
                            .background(barColor)
                            .align(Alignment.BottomCenter)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = ScOnSurfaceVariant, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

// ── Activity Heatmap (GitHub style) ──────────────────────────────────────────

@Composable
fun ActivityHeatmap(data: Map<Int, Int>, modifier: Modifier = Modifier) {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val totalDays = if (isLeapYear(year)) 366 else 365
    val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1

    // Start from Jan 1
    val startCal = Calendar.getInstance().apply {
        set(year, Calendar.JANUARY, 1)
    }
    val startDow = (startCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0

    val cellSize = 10.dp
    val gap = 2.dp

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        val totalCells = startDow + totalDays
        val cols = (totalCells + 6) / 7

        for (col in 0 until cols) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until 7) {
                    val dayIndex = col * 7 + row - startDow
                    if (dayIndex < 0 || dayIndex >= totalDays) {
                        Box(modifier = Modifier.size(cellSize))
                    } else {
                        val count = data[dayIndex] ?: 0
                        val intensity = count.toFloat() / maxVal
                        val color = when {
                            count == 0 -> ScSurfaceContainerHigh
                            intensity < 0.33f -> ScPrimaryFixed
                            intensity < 0.66f -> ScPrimaryContainer
                            else -> ScPrimary
                        }
                        Box(
                            modifier = Modifier.size(cellSize)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}

// ── Focus Line Chart ──────────────────────────────────────────────────────────

@Composable
fun FocusLineChart(focusData: List<Float>, modifier: Modifier = Modifier) {
    // focusData: 7 điểm theo giờ trong ngày (0-23h chia 7 khoảng)
    val points = if (focusData.all { it == 0f }) {
        // Không có data — hiển thị đường phẳng ở giữa
        List(7) { 0.5f }
    } else {
        focusData
    }
    val hasData = focusData.any { it > 0f }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        val pathPoints = points.mapIndexed { i, v ->
            Offset(i * stepX, h - v * h * 0.85f - h * 0.05f)
        }

        // Gradient fill
        val fillPath = Path().apply {
            moveTo(pathPoints.first().x, pathPoints.first().y)
            for (i in 1 until pathPoints.size) {
                val prev = pathPoints[i - 1]
                val curr = pathPoints[i]
                val cx = (prev.x + curr.x) / 2f
                cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            }
            lineTo(pathPoints.last().x, h)
            lineTo(pathPoints.first().x, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(ScTertiary.copy(alpha = 0.2f), Color.Transparent)
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(pathPoints.first().x, pathPoints.first().y)
            for (i in 1 until pathPoints.size) {
                val prev = pathPoints[i - 1]
                val curr = pathPoints[i]
                val cx = (prev.x + curr.x) / 2f
                cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
            }
        }
        drawPath(
            path = linePath,
            color = ScTertiary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Dots at key points
        listOf(1, 3, 5).forEach { i ->
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = pathPoints[i]
            )
            drawCircle(
                color = ScTertiary,
                radius = 4.dp.toPx(),
                center = pathPoints[i],
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// ── Stat Summary Card ─────────────────────────────────────────────────────────


@Composable
fun UsageChart(activities: List<com.example.studyapp.data.model.UserActivity>) {
    val maxDuration = remember(activities) {
        (activities.maxOfOrNull { it.durationMillis } ?: 1L).coerceAtLeast(900000L) // Min 15 mins scale
    }
    val days = remember {
        (0..6).reversed().map { d ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -d) }
        }
    }
    val dayFormatter = SimpleDateFormat("EE", Locale("vi"))
    var selectedDayIndex by remember { mutableStateOf(-1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp).height(120.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, day ->
                val dayStart = (day.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val activity = activities.find { it.date == dayStart }
                val duration = activity?.durationMillis ?: 0L
                val isToday = index == 6
                val isSelected = selectedDayIndex == index
                
                // Animated height
                val targetHeight = (duration.toFloat() / maxDuration).coerceIn(0.08f, 1f)
                val animatedHeight by animateFloatAsState(
                    targetValue = targetHeight,
                    animationSpec = tween(1000, easing = EaseOutBack),
                    label = "height"
                )
                
                // Animated color
                val barColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> ScSecondary
                        isToday -> ScPrimary
                        duration > 0 -> ScPrimary.copy(alpha = 0.8f)
                        else -> ScOutlineVariant.copy(alpha = 0.2f)
                    },
                    animationSpec = tween(400),
                    label = "color"
                )

                val barScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "barScale"
                )
                
                val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
                val sparkleAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
                    label = "sparkleAlpha"
                )

                val animatedWidth by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 12.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "width"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedDayIndex = if (selectedDayIndex == index) -1 else index
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(animatedWidth)
                            .fillMaxHeight(animatedHeight)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        BorderStroke(2.dp, Brush.sweepGradient(listOf(
                                            ScPrimary.copy(alpha = sparkleAlpha), 
                                            ScSecondary.copy(alpha = sparkleAlpha), 
                                            ScTertiary.copy(alpha = sparkleAlpha), 
                                            ScPrimary.copy(alpha = sparkleAlpha)
                                        ))),
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                                } else if (isToday && duration == 0L) {
                                    Modifier.border(1.dp, ScPrimary.copy(alpha = 0.5f), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                } else Modifier
                            )
                    ) {
                        val textAlpha by animateFloatAsState(
                            targetValue = if (isSelected && duration > 0) 1f else 0f,
                            animationSpec = tween(300),
                            label = "textAlpha"
                        )
                        if (textAlpha > 0.01f) {
                            Text(
                                text = "${duration / 60000}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = textAlpha),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isToday) "Hnay" else dayFormatter.format(day.time).replace("Th ", "T"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected || isToday) ScPrimary else ScOnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun StatSummaryCard(
    modifier: Modifier,
    value: String,
    label: String,
    iconPath: String,
    accent: Color,
    bg: Color
) {
    val animatedValue = remember(value) { mutableStateOf(0) }
    LaunchedEffect(value) {
        // Simple animation logic for non-numeric values like "1h 20m"
        // If it's a number, we can use animateIntAsState, otherwise just set it
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ScSurfaceContainerLowest,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Image(loadAssetImage(iconPath), null, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge,
                color = accent, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = ScOnSurfaceVariant, lineHeight = 14.sp)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun computeWeekBars(todos: List<TodoItem>): List<Pair<String, Float>> {
    val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    val cal = Calendar.getInstance()
    // Get start of this week (Monday)
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val daysFromMon = (dow + 5) % 7
    cal.add(Calendar.DAY_OF_YEAR, -daysFromMon)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val weekStart = cal.timeInMillis

    val counts = IntArray(7)
    todos.filter { it.isCompleted && it.dueDate != null }.forEach { todo ->
        val diff = ((todo.dueDate!! - weekStart) / (24 * 60 * 60 * 1000)).toInt()
        if (diff in 0..6) counts[diff]++
    }
    val max = counts.max().coerceAtLeast(1).toFloat()
    return labels.mapIndexed { i, label -> Pair(label, counts[i] / max) }
}

private fun computeHeatmap(todos: List<TodoItem>): Map<Int, Int> {
    val year = Calendar.getInstance().get(Calendar.YEAR)
    val yearStart = Calendar.getInstance().apply {
        set(year, Calendar.JANUARY, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val map = mutableMapOf<Int, Int>()
    todos.filter { it.isCompleted && it.dueDate != null }.forEach { todo ->
        val dayIndex = ((todo.dueDate!! - yearStart) / (24 * 60 * 60 * 1000)).toInt()
        if (dayIndex >= 0) map[dayIndex] = (map[dayIndex] ?: 0) + 1
    }
    return map
}

/**
 * Tính streak: số ngày liên tiếp có ít nhất 1 task hoàn thành,
 * tính từ hôm nay trở về trước.
 */
private fun computeStreak(todos: List<TodoItem>): Int {
    val completedDays = todos.filter { it.isCompleted && it.dueDate != null }
        .map {
            val c = Calendar.getInstance().apply { timeInMillis = it.dueDate!! }
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            c.timeInMillis
        }.toSet()

    var streak = 0
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Kiểm tra từ hôm nay trở về trước
    while (true) {
        if (completedDays.contains(cal.timeInMillis)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    return streak
}

private fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

/**
 * Tính mức độ tập trung theo 7 khoảng thời gian trong ngày dựa trên
 * số task đã hoàn thành (dueDate) trong 30 ngày gần nhất.
 * Khoảng: 0-3h, 4-6h, 7-10h, 11-13h, 14-17h, 18-20h, 21-23h
 */
private fun computeFocusLevel(todos: List<TodoItem>): List<Float> {
    val slots = IntArray(7) // đếm task theo khoảng giờ
    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000

    todos.filter { it.isCompleted && it.dueDate != null && it.dueDate >= thirtyDaysAgo }
        .forEach { todo ->
            val cal = Calendar.getInstance().apply { timeInMillis = todo.dueDate!! }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slot = when (hour) {
                in 0..3   -> 0
                in 4..6   -> 1
                in 7..10  -> 2
                in 11..13 -> 3
                in 14..17 -> 4
                in 18..20 -> 5
                else      -> 6
            }
            slots[slot]++
        }

    val max = slots.max().coerceAtLeast(1).toFloat()
    return slots.map { it / max }
}

/**
 * Tính hiệu suất: so sánh số task hoàn thành tuần này vs tuần trước.
 * Trả về phần trăm thay đổi (dương = tốt hơn, âm = kém hơn).
 */
private fun computeEfficiency(todos: List<TodoItem>): Int {
    val cal = Calendar.getInstance()
    // Đầu tuần này (Thứ 2)
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val daysFromMon = (dow + 5) % 7
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    cal.add(Calendar.DAY_OF_YEAR, -daysFromMon)
    val thisWeekStart = cal.timeInMillis
    val lastWeekStart = thisWeekStart - 7L * 24 * 60 * 60 * 1000

    val completedTodos = todos.filter { it.isCompleted && it.dueDate != null }
    val thisWeek = completedTodos.count { it.dueDate!! >= thisWeekStart }
    val lastWeek = completedTodos.count { it.dueDate!! in lastWeekStart until thisWeekStart }

    return if (lastWeek == 0) {
        if (thisWeek > 0) 100 else 0
    } else {
        ((thisWeek - lastWeek) * 100 / lastWeek)
    }
}

private fun formatDurationShort(millis: Long): String {
    val mins = millis / 60000
    val hours = mins / 60
    return if (hours > 0) {
        "${hours}h ${mins % 60}m"
    } else {
        "${mins}m"
    }
}
