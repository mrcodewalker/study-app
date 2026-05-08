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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.studyapp.ui.viewmodel.UserActivityViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.example.studyapp.ui.util.loadAssetImage
import com.example.studyapp.ui.util.SoundManager
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    flashcardViewModel: FlashcardViewModel,
    noteViewModel: NoteViewModel,
    todoViewModel: TodoViewModel,
    userActivityViewModel: UserActivityViewModel,
    onNavigateToFlashcard: () -> Unit,
    onNavigateToNote: () -> Unit,
    onNavigateToTodo: () -> Unit,
    onDeckClick: (Long) -> Unit,
    onOpenAiChat: () -> Unit = {},
    aiChatEnabled: Boolean = true,
    onToggleAiChat: (Boolean) -> Unit = {},
    musicEnabled: Boolean = true,
    onToggleMusic: (Boolean) -> Unit = {},
    onShuffleGif: () -> Unit = {}
) {
    val decks by flashcardViewModel.allDecks.collectAsState()
    val notes by noteViewModel.allNotes.collectAsState()
    val todos by todoViewModel.allTodos.collectAsState()
    val recentActivity by userActivityViewModel.recentActivity.collectAsState()

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember {
        when (hour) {
            in 5..11 -> "Chào buổi sáng ☀️"
            in 12..17 -> "Chào buổi chiều 🌤️"
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
    var iconLanded by remember { mutableStateOf(false) }
    var showFireworks by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(700)
        iconLanded = true
    }

    // Fireworks effect
    LaunchedEffect(showFireworks) {
        if (showFireworks) {
            kotlinx.coroutines.delay(3000)
            showFireworks = false
        }
    }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ScPrimaryContainer.copy(alpha = 0.5f),
                                ScPrimaryContainer.copy(alpha = 0.2f),
                                ScBackground
                            )
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    drawCircle(
                        color = ScPrimary.copy(alpha = 0.08f),
                        radius = 120.dp.toPx(),
                        center = Offset(size.width * 0.85f, -40.dp.toPx())
                    )
                    drawCircle(
                        color = ScSecondary.copy(alpha = 0.06f),
                        radius = 80.dp.toPx(),
                        center = Offset(size.width * 0.15f, size.height * 0.7f)
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                greeting,
                                style = MaterialTheme.typography.bodyLarge,
                                color = ScPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "KMAStudy",
                                style = MaterialTheme.typography.displaySmall,
                                color = ScOnSurface,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            if (totalCount > 0) {
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = ScPrimaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle, null,
                                            tint = ScPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            "$completedCount/$totalCount nhiệm vụ",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ScPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Floating reading icon + AI toggle button
                        val infiniteTransition = rememberInfiniteTransition(label = "float")
                        val floatY by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = -8f,
                            animationSpec = infiniteRepeatable(
                                tween(2000, easing = EaseInOutSine), RepeatMode.Reverse
                            ),
                            label = "float"
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .graphicsLayer { translationY = floatY },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = loadAssetImage("reading.png"),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            // Nút bật/tắt AI chatbot + Shuffle GIF
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (aiChatEnabled) ScPrimaryContainer else ScSurfaceContainerLow,
                                    border = BorderStroke(1.dp, if (aiChatEnabled) ScPrimary.copy(alpha = 0.4f) else ScOutlineVariant),
                                    modifier = Modifier.clickable { onToggleAiChat(!aiChatEnabled) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "Bật/tắt AI",
                                            tint = if (aiChatEnabled) ScPrimary else ScOnSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            if (aiChatEnabled) "AI" else "AI",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (aiChatEnabled) ScPrimary else ScOnSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                // Nút bật/tắt Music bubble
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (musicEnabled) ScSecondaryContainer else ScSurfaceContainerLow,
                                    border = BorderStroke(1.dp, if (musicEnabled) ScSecondary.copy(alpha = 0.4f) else ScOutlineVariant),
                                    modifier = Modifier.clickable { onToggleMusic(!musicEnabled) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = "Bật/tắt nhạc",
                                            tint = if (musicEnabled) ScSecondary else ScOnSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            "♪",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (musicEnabled) ScSecondary else ScOnSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                // Shuffle GIF button
                                Surface(
                                    shape = CircleShape,
                                    color = ScSecondaryContainer,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable {
                                            onShuffleGif()
                                            showFireworks = true
                                            SoundManager.playConfettiThenCong(scope)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Shuffle,
                                            contentDescription = "Đổi GIF",
                                            tint = ScOnSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                        HomeCircularProgress(progress = todayProgress, modifier = Modifier.size(110.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Mục tiêu học tập", style = MaterialTheme.typography.titleSmall, color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (totalCount == 0) "Chưa có nhiệm vụ" else "Còn ${totalCount - completedCount} nhiệm vụ",
                            style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant
                        )
                    }
                }

                // Continue learning card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Surface(shape = RoundedCornerShape(99.dp), color = ScSecondaryContainer) {
                                    Text(
                                        "Tiếp tục học", color = ScOnSecondaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    if (decks.isNotEmpty()) decks.first().name else "Chưa có bộ thẻ",
                                    style = MaterialTheme.typography.titleMedium, color = ScOnSurface,
                                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onNavigateToFlashcard, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.ArrowForward, null, tint = ScPrimary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            color = ScSurfaceContainerLow,
                            border = BorderStroke(1.dp, ScPrimaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val isMastered = decks.isNotEmpty() && decks.first().let { it.cardCount > 0 && it.studiedCount >= it.cardCount }
                                Image(loadAssetImage(if (isMastered) "trophy.png" else "flash-card.png"), null, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(6.dp))
                                if (decks.isNotEmpty()) {
                                    val deck = decks.first()
                                    val remaining = (deck.cardCount - deck.studiedCount).coerceAtLeast(0)
                                    Text(
                                        if (isMastered) "Hoàn thành xuất sắc!" else "$remaining thẻ còn lại",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isMastered) Color(0xFFFBC02D) else ScOnSurfaceVariant,
                                        fontWeight = if (isMastered) FontWeight.Bold else FontWeight.Normal
                                    )
                                } else {
                                    Text("Nhấn + để tạo bộ thẻ", style = MaterialTheme.typography.labelMedium, color = ScOnSurfaceVariant)
                                }
                            }
                        }

                        if (decks.isNotEmpty()) {
                            val deck = decks.first()
                            val pf = if (deck.cardCount > 0) (deck.studiedCount.toFloat() / deck.cardCount).coerceIn(0f, 1f) else 0f
                            val prog by animateFloatAsState(targetValue = pf, animationSpec = tween(900, easing = EaseOutCubic), label = "prog")
                            Box(
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp))
                                    .background(ScPrimaryContainer.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(prog).fillMaxHeight().clip(RoundedCornerShape(99.dp))
                                        .background(if (pf >= 1f) Brush.horizontalGradient(listOf(ScPrimary, Color(0xFF4CAF50))) else SolidColor(ScPrimary))
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
                Text(
                    "Truy cập nhanh", style = MaterialTheme.typography.titleSmall,
                    color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeQuickAction(Modifier.weight(1f), "flash-card.png", "Thẻ học", ScPrimary, ScPrimaryContainer, onNavigateToFlashcard)
                    HomeQuickAction(Modifier.weight(1f), "sticky-note.png", "Ghi chú", ScSecondary, ScSecondaryContainer, onNavigateToNote)
                    HomeQuickAction(Modifier.weight(1f), "clipboard.png", "Công việc", ScTertiary, ScTertiaryContainer, onNavigateToTodo)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Công việc hôm nay", style = MaterialTheme.typography.titleSmall, color = ScOnSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = onNavigateToTodo, contentPadding = PaddingValues(0.dp)) {
                            Text("Xem tất cả", color = ScPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        upcomingTodos.forEach { HomeTodoRow(it) }
                    }
                }
            }
        }

        // ── Recent notes ─────────────────────────────────────────────────────
        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 450)) + slideInVertically(tween(500, 450)) { 20 }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ghi chú gần đây", style = MaterialTheme.typography.titleSmall, color = ScOnSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = onNavigateToNote, contentPadding = PaddingValues(0.dp)) {
                            Text("Xem tất cả", color = ScPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(notes.take(10), key = { _, n -> n.id }) { idx, note ->
                            var cardVis by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { kotlinx.coroutines.delay((idx * 60).toLong()); cardVis = true }
                            AnimatedVisibility(visible = cardVis, enter = fadeIn(tween(350)) + slideInHorizontally(tween(350)) { 40 }) {
                                HomeRecentNoteCard(note, onClick = onNavigateToNote)
                            }
                        }
                    }
                }
            }
        }

        // ── Recent decks ─────────────────────────────────────────────────────
        if (decks.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500, 550)) + slideInVertically(tween(500, 550)) { 20 }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bộ thẻ gần đây", style = MaterialTheme.typography.titleSmall, color = ScOnSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = onNavigateToFlashcard, contentPadding = PaddingValues(0.dp)) {
                            Text("Xem tất cả", color = ScPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(decks.take(10), key = { _, d -> d.id }) { idx, deck ->
                            var cardVis by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { kotlinx.coroutines.delay((idx * 60).toLong()); cardVis = true }
                            AnimatedVisibility(visible = cardVis, enter = fadeIn(tween(350)) + slideInHorizontally(tween(350)) { 40 }) {
                                HomeRecentDeckCard(deck, onClick = { onDeckClick(deck.id) })
                            }
                        }
                    }
                }
            }
        }

        // ── Activity Chart ───────────────────────────────────────────────────
        Spacer(Modifier.height(28.dp))
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500, 300)) + slideInVertically(tween(500, 300)) { 20 }
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("Hoạt động 7 ngày qua", style = MaterialTheme.typography.titleSmall, color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                UsageChart(recentActivity)
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    // Fireworks overlay
    if (showFireworks) {
        FireworksOverlay()
    }
}

// ── Circular progress ─────────────────────────────────────────────────────────

@Composable
fun HomeCircularProgress(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "progress"
    )
    val pct = (animatedProgress * 100).toInt()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Track (background circle)
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.fillMaxSize(),
            color = ScPrimaryContainer,
            strokeWidth = 10.dp
        )
        // Actual progress
        CircularProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxSize(),
            color = ScPrimary,
            strokeWidth = 10.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pct%", style = MaterialTheme.typography.headlineSmall, color = ScPrimary, fontWeight = FontWeight.Bold)
            Text("Hoàn thành", style = MaterialTheme.typography.labelSmall, color = ScOnSurfaceVariant)
        }
    }
}

// ── Quick action card ─────────────────────────────────────────────────────────

@Composable
fun HomeQuickAction(modifier: Modifier, iconPath: String, label: String, accent: Color, bg: Color, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Surface(
        modifier = modifier.scale(scale).clickable { pressed = true; SoundManager.play("click"); onClick() },
        shape = RoundedCornerShape(16.dp), color = ScSurfaceContainerLowest, shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(46.dp).scale(pulse).clip(RoundedCornerShape(14.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Image(loadAssetImage(iconPath), null, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = ScOnSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Today todo row ────────────────────────────────────────────────────────────

@Composable
fun HomeTodoRow(todo: TodoItem) {
    val isOverdue = todo.dueDate != null && todo.dueDate < System.currentTimeMillis()
    val dueDateStr = todo.dueDate?.let { SimpleDateFormat("HH:mm, dd/MM", Locale("vi")).format(Date(it)) }
    val priorityChip = when (todo.priority) {
        2 -> Pair("Cao", ScTertiaryContainer)
        1 -> Pair("TB", ScSecondaryContainer)
        else -> null
    }

    Surface(
        shape = RoundedCornerShape(14.dp), color = ScSurfaceContainerLowest,
        border = BorderStroke(1.dp, if (isOverdue) ScErrorContainer else ScOutlineVariant), shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).border(1.5.dp, ScPrimaryContainer, RoundedCornerShape(6.dp)))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(todo.title, style = MaterialTheme.typography.bodyMedium, color = ScOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (dueDateStr != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = if (isOverdue) ScError else ScOnSurfaceVariant, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(dueDateStr, style = MaterialTheme.typography.labelSmall, color = if (isOverdue) ScError else ScOnSurfaceVariant)
                    }
                }
            }
            if (priorityChip != null) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(99.dp), color = priorityChip.second) {
                    Text(priorityChip.first, color = ScOnSurface, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Recent deck card ──────────────────────────────────────────────────────────

@Composable
fun HomeRecentDeckCard(deck: FlashcardDeck, onClick: () -> Unit) {
    val colorIdx = (deck.id % 3).toInt()
    val (chipBg, chipText, accent) = listOf(
        Triple(ScPrimaryContainer, ScOnPrimaryContainer, ScPrimary),
        Triple(ScSecondaryContainer, ScOnSecondaryContainer, ScSecondary),
        Triple(ScTertiaryContainer, ScOnTertiaryContainer, ScTertiary),
    )[colorIdx]

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    val progressFraction = if (deck.cardCount > 0) (deck.studiedCount.toFloat() / deck.cardCount).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(targetValue = progressFraction, animationSpec = tween(1000, easing = EaseOutCubic), label = "progress")
    val progressPct = (progressFraction * 100).toInt()

    Surface(
        modifier = Modifier.width(170.dp).height(165.dp).scale(scale).clickable { pressed = true; onClick() },
        shape = RoundedCornerShape(18.dp), color = ScSurfaceContainerLowest, shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val isMastered = progressFraction >= 1f
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = RoundedCornerShape(99.dp), color = if (isMastered) Color(0xFFFFECB3) else chipBg) {
                    Text(
                        if (isMastered) "MASTERED" else "FLASHCARD",
                        color = if (isMastered) Color(0xFFFFA000) else chipText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontWeight = FontWeight.Bold
                    )
                }
                Image(loadAssetImage(if (isMastered) "trophy.png" else "fire.png"), null, modifier = Modifier.size(if (isMastered) 24.dp else 18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(deck.name, style = MaterialTheme.typography.titleSmall, color = ScOnSurface, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("${deck.cardCount} thẻ", style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(chipBg.copy(alpha = 0.5f))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(99.dp))
                    .background(if (isMastered) Brush.horizontalGradient(listOf(accent, Color(0xFF4CAF50))) else SolidColor(accent)))
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isMastered) "Hoàn thành 100%" else "$progressPct% thuộc",
                    style = MaterialTheme.typography.labelSmall, color = if (isMastered) Color(0xFF2E7D32) else accent, fontWeight = FontWeight.Bold
                )
                val notLearned = deck.cardCount - deck.studiedCount
                if (notLearned > 0 && !isMastered) {
                    Surface(shape = RoundedCornerShape(99.dp), color = ScErrorContainer.copy(alpha = 0.7f)) {
                        Text("$notLearned cần ôn", style = MaterialTheme.typography.labelSmall, color = ScError, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ── Recent note card ──────────────────────────────────────────────────────────

private val homeNoteAccents = listOf(ScPrimary, ScSecondary, ScTertiary, ScWarning, ScError, Color(0xFF7B5EA7))
private val homeNoteBgs = listOf(Color(0xFFFFFFFF), Color(0xFFEAF5F1), Color(0xFFF0EDFF), Color(0xFFE8F3F9), Color(0xFFFFF8E1), Color(0xFFF5F0FF))

@Composable
fun HomeRecentNoteCard(note: com.example.studyapp.data.model.Note, onClick: () -> Unit) {
    val colorIdx = note.color.coerceIn(0, homeNoteBgs.size - 1)
    val bg = homeNoteBgs[colorIdx]
    val accent = homeNoteAccents[colorIdx]
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

    Surface(
        modifier = Modifier.width(150.dp).height(155.dp).scale(scale).clickable { pressed = true; onClick() },
        shape = RoundedCornerShape(18.dp), color = bg, shadowElevation = 2.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.weight(1f))
                Text(dateFormat.format(Date(note.updatedAt)), style = MaterialTheme.typography.labelSmall, color = ScOutline.copy(0.7f))
            }
            Spacer(Modifier.height(8.dp))
            if (note.title.isNotBlank()) {
                Text(note.title, style = MaterialTheme.typography.titleSmall, color = ScOnSurface, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                note.content.ifBlank { "Không có nội dung" },
                style = MaterialTheme.typography.bodySmall,
                color = if (note.content.isBlank()) ScOutline else ScOnSurfaceVariant,
                maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp
            )
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(99.dp)).background(accent.copy(alpha = 0.25f)))
        }
    }
}

// ── Fireworks overlay ─────────────────────────────────────────────────────────

@Composable
fun FireworksOverlay() {
    val parties = remember {
        val colors = listOf(0xFFFF6B6B, 0xFFFFD93D, 0xFF6BCB77, 0xFF4D96FF, 0xFFFF6FC8, 0xFFFFB347)
            .map { it.toInt() }
        val base = Party(
            speed = 0f,
            maxSpeed = 35f,
            damping = 0.9f,
            spread = 360,
            colors = colors,
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 150, TimeUnit.MILLISECONDS).max(120)
        )
        listOf(
            base,
            base.copy(position = Position.Relative(0.2, 0.5)),
            base.copy(position = Position.Relative(0.8, 0.5)),
            base.copy(position = Position.Relative(0.5, 0.6))
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        KonfettiView(
            modifier = Modifier.fillMaxSize(),
            parties = parties
        )
        // Emoji 🎉 ở giữa
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val scale by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "emoji"
            )
            Text("🎉", fontSize = (52f * scale).sp,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
        }
    }
}

// ── Legacy aliases ────────────────────────────────────────────────────────────
@Composable
fun HomeStatCard(modifier: Modifier, value: String, label: String, iconPath: String, accent: Color, bg: Color, onClick: () -> Unit) =
    HomeQuickAction(modifier, iconPath, label, accent, bg, onClick)

@Composable
fun UpcomingTodoRow(todo: TodoItem) = HomeTodoRow(todo)
