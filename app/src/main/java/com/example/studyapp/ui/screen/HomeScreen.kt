package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.data.model.FlashcardDeck
import com.example.studyapp.data.model.TodoItem
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.FlashcardViewModel
import com.example.studyapp.ui.viewmodel.NoteViewModel
import com.example.studyapp.ui.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    flashcardViewModel: FlashcardViewModel,
    noteViewModel: NoteViewModel,
    todoViewModel: TodoViewModel,
    onNavigateToFlashcard: () -> Unit,
    onNavigateToNote: () -> Unit,
    onNavigateToTodo: () -> Unit,
    onDeckClick: (Long) -> Unit
) {
    val decks by flashcardViewModel.allDecks.collectAsState()
    val notes by noteViewModel.allNotes.collectAsState()
    val todos by todoViewModel.allTodos.collectAsState()

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when (hour) {
            in 5..11 -> "Chào buổi sáng ☀️"
            in 12..17 -> "Chào buổi chiều 🌤"
            else -> "Chào buổi tối 🌙"
        }
    }

    val completedCount = todos.count { it.isCompleted }
    val totalCount = todos.size
    val todayProgress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    val upcomingTodos = remember(todos) {
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis
        todos.filter { !it.isCompleted && it.dueDate != null && it.dueDate <= todayEnd }
            .sortedBy { it.dueDate }.take(3)
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScBackground)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Welcome section ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -30 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ScPrimaryContainer.copy(alpha = 0.45f), ScBackground)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "KMAStudy",
                    style = MaterialTheme.typography.displaySmall,
                    color = ScOnSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                if (totalCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Hôm nay bạn có $totalCount mục tiêu cần hoàn thành.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ScOnSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Bento: Circular progress + Continue learning ─────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, 150)) + slideInVertically(tween(500, 150)) { 30 }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular progress card
                Surface(
                    modifier = Modifier.weight(5f).fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = ScSurfaceContainerLowest,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        HomeCircularProgress(
                            progress = todayProgress,
                            modifier = Modifier.size(110.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Mục tiêu học tập",
                            style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (totalCount == 0) "Chưa có nhiệm vụ"
                            else "Còn ${totalCount - completedCount} nhiệm vụ",
                            style = MaterialTheme.typography.bodySmall,
                            color = ScOnSurfaceVariant
                        )
                    }
                }

                // Continue learning / quick stats card
                Surface(
                    modifier = Modifier.weight(7f).fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = ScSurfaceContainerLowest,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = ScSecondaryContainer
                                ) {
                                    Text(
                                        "Tiếp tục học",
                                        color = ScOnSecondaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (decks.isNotEmpty()) decks.first().name else "Chưa có bộ thẻ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ScOnSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = onNavigateToFlashcard,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward, null,
                                    tint = ScPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Flashcard preview box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = ScSurfaceContainerLow,
                            border = BorderStroke(1.dp, ScPrimaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Style, null,
                                    tint = ScPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                if (decks.isNotEmpty()) {
                                    val deck = decks.first()
                                    val remaining = (deck.cardCount - deck.studiedCount).coerceAtLeast(0)
                                    Text(
                                        "$remaining thẻ còn lại",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ScOnSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        "Nhấn + để tạo bộ thẻ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ScOnSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Progress bar
                        if (decks.isNotEmpty()) {
                            val deck = decks.first()
                            val pf = if (deck.cardCount > 0)
                                (deck.studiedCount.toFloat() / deck.cardCount).coerceIn(0f, 1f)
                            else 0f
                            val prog by animateFloatAsState(
                                targetValue = pf,
                                animationSpec = tween(900, easing = EaseOutCubic),
                                label = "prog"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(ScPrimaryContainer.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(prog)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(ScPrimary)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Quick actions ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, 250)) + slideInVertically(tween(500, 250)) { 20 }
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Truy cập nhanh",
                        style = MaterialTheme.typography.titleSmall,
                        color = ScOnSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeQuickAction(
                        Modifier.weight(1f), Icons.Filled.Style,
                        "Thẻ học", ScPrimary, ScPrimaryContainer, onNavigateToFlashcard
                    )
                    HomeQuickAction(
                        Modifier.weight(1f), Icons.Filled.EditNote,
                        "Ghi chú", ScSecondary, ScSecondaryContainer, onNavigateToNote
                    )
                    HomeQuickAction(
                        Modifier.weight(1f), Icons.Filled.Task,
                        "Công việc", ScTertiary, ScTertiaryContainer, onNavigateToTodo
                    )
                }
            }
        }

        // ── Today's tasks ────────────────────────────────────────────────────
        if (upcomingTodos.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 350)) + slideInVertically(tween(500, 350)) { 20 }
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Công việc hôm nay",
                            style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onNavigateToTodo,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Xem tất cả",
                                color = ScPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        upcomingTodos.forEach { HomeTodoRow(it) }
                    }
                }
            }
        }

        // ── Recent decks ─────────────────────────────────────────────────────
        if (decks.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 450)) + slideInVertically(tween(500, 450)) { 20 }
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Bộ thẻ gần đây",
                            style = MaterialTheme.typography.titleSmall,
                            color = ScOnSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onNavigateToFlashcard,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Xem tất cả",
                                color = ScPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(decks.take(5), key = { _, d -> d.id }) { idx, deck ->
                            var cardVis by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay((idx * 80).toLong())
                                cardVis = true
                            }
                            AnimatedVisibility(
                                visible = cardVis,
                                enter = fadeIn(tween(350)) + slideInHorizontally(tween(350)) { 40 }
                            ) {
                                HomeRecentDeckCard(deck, onClick = { onDeckClick(deck.id) })
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

// ── Circular progress (Stitch style) ─────────────────────────────────────────

@Composable
fun HomeCircularProgress(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )
    val pct = (animatedProgress * 100).toInt()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 11.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = ScPrimaryContainer,
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ScPrimary,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$pct%",
                style = MaterialTheme.typography.headlineSmall,
                color = ScPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Hoàn thành",
                style = MaterialTheme.typography.labelSmall,
                color = ScOnSurfaceVariant
            )
        }
    }
}

// ── Quick action card ─────────────────────────────────────────────────────────

@Composable
fun HomeQuickAction(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    accent: Color,
    bg: Color,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable { pressed = true; onClick() },
        shape = RoundedCornerShape(16.dp),
        color = ScSurfaceContainerLowest,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .scale(pulse)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = ScOnSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Today todo row ────────────────────────────────────────────────────────────

@Composable
fun HomeTodoRow(todo: TodoItem) {
    val isOverdue = todo.dueDate != null && todo.dueDate < System.currentTimeMillis()
    val dueDateStr = todo.dueDate?.let {
        SimpleDateFormat("HH:mm, dd/MM", Locale("vi")).format(Date(it))
    }
    val priorityChip = when (todo.priority) {
        2 -> Pair("Cao", ScTertiaryContainer)
        1 -> Pair("TB", ScSecondaryContainer)
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ScSurfaceContainerLowest,
        border = BorderStroke(1.dp, if (isOverdue) ScErrorContainer else ScOutlineVariant),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox placeholder
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.5.dp, ScPrimaryContainer, RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dueDateStr != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule, null,
                            tint = if (isOverdue) ScError else ScOnSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            dueDateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverdue) ScError else ScOnSurfaceVariant
                        )
                    }
                }
            }
            if (priorityChip != null) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = priorityChip.second
                ) {
                    Text(
                        priorityChip.first,
                        color = ScOnSurface,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Recent deck card (horizontal scroll) ─────────────────────────────────────

@Composable
fun HomeRecentDeckCard(deck: FlashcardDeck, onClick: () -> Unit) {
    val colorIdx = (deck.id % 3).toInt()
    val (chipBg, chipText, accent) = listOf(
        Triple(ScPrimaryContainer, ScOnPrimaryContainer, ScPrimary),
        Triple(ScSecondaryContainer, ScOnSecondaryContainer, ScSecondary),
        Triple(ScTertiaryContainer, ScOnTertiaryContainer, ScTertiary),
    )[colorIdx]

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val progressFraction = if (deck.cardCount > 0)
        (deck.studiedCount.toFloat() / deck.cardCount).coerceIn(0f, 1f)
    else 0f
    val progress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )
    val progressPct = (progressFraction * 100).toInt()

    Surface(
        modifier = Modifier
            .width(170.dp)
            .scale(scale)
            .clickable { pressed = true; onClick() },
        shape = RoundedCornerShape(18.dp),
        color = ScSurfaceContainerLowest,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(99.dp), color = chipBg) {
                Text(
                    "Flashcard",
                    color = chipText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                deck.name,
                style = MaterialTheme.typography.titleSmall,
                color = ScOnSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${deck.cardCount} thẻ",
                style = MaterialTheme.typography.bodySmall,
                color = ScOnSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(chipBg.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(99.dp))
                        .background(accent)
                )
            }
            if (progressPct > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "$progressPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Keep legacy aliases so other files that reference them still compile
@Composable
fun HomeStatCard(
    modifier: Modifier, value: String, label: String,
    icon: ImageVector, accent: Color, bg: Color, onClick: () -> Unit
) = HomeQuickAction(modifier, icon, label, accent, bg, onClick)

@Composable
fun UpcomingTodoRow(todo: TodoItem) = HomeTodoRow(todo)
