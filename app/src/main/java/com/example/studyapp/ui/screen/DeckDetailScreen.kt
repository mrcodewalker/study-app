package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.studyapp.data.model.Flashcard
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.FlashcardViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(deckId: Long, viewModel: FlashcardViewModel, onBack: () -> Unit) {
    LaunchedEffect(deckId) { viewModel.selectDeck(deckId) }

    val deck by viewModel.selectedDeck.collectAsState()
    val cards by viewModel.cardsForSelectedDeck.collectAsState()
    val bulkPreview by viewModel.bulkPreview.collectAsState()

    var showAddSingle by remember { mutableStateOf(false) }
    var showAddBulk by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Flashcard?>(null) }
    var studyMode by remember { mutableStateOf(false) }
    var shuffleMode by remember { mutableStateOf(false) }

    if (studyMode && cards.isNotEmpty()) {
        val startIndex = deck?.lastStudiedIndex?.coerceIn(0, cards.size - 1) ?: 0
        val studyCards = if (shuffleMode) remember(cards) { cards.shuffled() } else cards
        StudyModeScreen(
            cards = studyCards,
            initialIndex = if (shuffleMode) 0 else startIndex,
            initialStudiedCount = deck?.studiedCount ?: 0,
            onExit = { lastIdx, studiedCnt ->
                viewModel.saveStudyProgress(deckId, lastIdx, studiedCnt)
                studyMode = false
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(ScBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(ScSurfaceContainerLowest, ScBackground)))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.clearSelection(); onBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = ScOnSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        deck?.name ?: "Đang tải...",
                        style = MaterialTheme.typography.titleLarge,
                        color = ScOnSurface, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    val studiedCount = deck?.studiedCount ?: 0
                    val totalCount = cards.size
                    val progressPct = if (totalCount > 0) (studiedCount * 100 / totalCount) else 0
                    Text(
                        if (studiedCount > 0) "$studiedCount/$totalCount thẻ · $progressPct%"
                        else "${cards.size} thẻ",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (studiedCount > 0) ScPrimary else ScOnSurfaceVariant
                    )
                }
                if (cards.isNotEmpty()) {
                    IconButton(onClick = { shuffleMode = !shuffleMode }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Xáo trộn",
                            tint = if (shuffleMode) ScPrimary else ScOnSurfaceVariant
                        )
                    }
                    IconButton(onClick = { studyMode = true }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Học", tint = ScPrimary)
                    }
                }
            }

            // ── Progress bar ─────────────────────────────────────────────────
            val studiedCount = deck?.studiedCount ?: 0
            val totalCount = cards.size
            if (studiedCount > 0 && totalCount > 0) {
                val progress by animateFloatAsState(
                    targetValue = studiedCount.toFloat() / totalCount,
                    animationSpec = tween(800, easing = EaseOutCubic),
                    label = "progress"
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(ScPrimaryContainer.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(progress).fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(Brush.horizontalGradient(listOf(ScPrimary, ScPrimaryFixedDim)))
                        )
                    }
                    val lastIdx = deck?.lastStudiedIndex ?: 0
                    if (lastIdx > 0 && studiedCount < totalCount) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tiếp tục từ thẻ ${lastIdx + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScPrimary
                        )
                    }
                }
            }

            // ── Action buttons ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddSingle = true },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, ScPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm thẻ", color = ScPrimary, style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { showAddBulk = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ScPrimaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DynamicFeed, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm hàng loạt", color = ScPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Card list ────────────────────────────────────────────────────
            if (cards.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(ScPrimaryContainer.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Style, null, Modifier.size(48.dp), tint = ScPrimary)
                        }
                        Text("Chưa có thẻ nào", color = ScOnSurface,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Thêm thẻ đơn lẻ hoặc hàng loạt", color = ScOnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(card.id) { visible = true }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { 20 }
                        ) {
                            FlashcardItem(
                                card = card,
                                onEdit = { editingCard = card },
                                onDelete = { viewModel.deleteCard(card) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddSingle) {
        AddSingleCardDialog(
            onDismiss = { showAddSingle = false },
            onAdd = { front, back ->
                viewModel.addSingleCard(deckId, front, back)
                showAddSingle = false
            }
        )
    }
    if (showAddBulk) {
        BulkAddDialog(
            onDismiss = { showAddBulk = false },
            onParse = { input -> viewModel.parseBulkInput(input) }
        )
    }
    if (bulkPreview.isVisible) {
        BulkPreviewDialog(
            validCards = bulkPreview.validCards,
            errorLines = bulkPreview.errorLines,
            onConfirm = { viewModel.confirmBulkInsert(deckId); showAddBulk = false },
            onCancel = { viewModel.cancelBulkInsert() }
        )
    }
    editingCard?.let { card ->
        EditCardDialog(
            card = card,
            onDismiss = { editingCard = null },
            onSave = { updated -> viewModel.updateCard(updated); editingCard = null }
        )
    }
}

// ── FlashcardItem ─────────────────────────────────────────────────────────────

@Composable
fun FlashcardItem(card: Flashcard, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (expanded) ScPrimary.copy(alpha = 0.5f) else ScOutlineVariant,
        animationSpec = tween(250), label = "border"
    )
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScSurfaceContainerLowest),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (expanded) ScPrimary else ScPrimaryContainer)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    card.front,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ScOnSurface,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = ScOnSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = ScError.copy(0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = ScOutlineVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ScTertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                Icons.Default.LightMode, null,
                                tint = ScTertiary,
                                modifier = Modifier.padding(4.dp).size(14.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            card.back,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ScTertiary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

// ── StudyModeScreen ───────────────────────────────────────────────────────────

@Composable
fun StudyModeScreen(
    cards: List<Flashcard>,
    initialIndex: Int,
    initialStudiedCount: Int,
    onExit: (Int, Int) -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, cards.size - 1)) }
    var studiedCount by remember { mutableStateOf(initialStudiedCount) }
    var isFlipped by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }

    // 3-D flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "flip"
    )

    // Swipe offset
    var dragOffset by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "drag"
    )

    // Card entrance animation
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentIndex) {
        cardVisible = false
        kotlinx.coroutines.delay(80)
        cardVisible = true
        isFlipped = false
        dragOffset = 0f
    }

    val card = cards[currentIndex]
    val progress = (currentIndex + 1).toFloat() / cards.size

    if (showComplete) {
        StudyCompleteScreen(
            totalCards = cards.size,
            studiedCount = studiedCount,
            onRestart = {
                currentIndex = 0
                studiedCount = 0
                showComplete = false
            },
            onExit = { onExit(currentIndex, studiedCount) }
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ScPrimaryContainer.copy(alpha = 0.25f), ScBackground, ScBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onExit(currentIndex, studiedCount) }) {
                    Icon(Icons.Default.Close, null, tint = ScOnSurface)
                }
                // Progress bar
                Box(
                    modifier = Modifier.weight(1f).height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(ScPrimaryContainer.copy(alpha = 0.4f))
                ) {
                    val animProg by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(600, easing = EaseOutCubic),
                        label = "prog"
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight()
                            .clip(RoundedCornerShape(99.dp))
                            .background(ScPrimary)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${currentIndex + 1}/${cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ScOnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Flashcard (big, 3:4 aspect ratio) ───────────────────────
            AnimatedVisibility(
                visible = cardVisible,
                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = animatedOffset / 8f
                            cameraDistance = 12f * density
                        }
                        .pointerInput(currentIndex) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (abs(dragOffset) < 80f) {
                                        dragOffset = 0f
                                    }
                                },
                                onHorizontalDrag = { _, delta ->
                                    if (isFlipped) dragOffset += delta
                                }
                            )
                        }
                        .clickable { isFlipped = !isFlipped },
                    contentAlignment = Alignment.Center
                ) {
                    // Back face
                    if (rotation > 90f) {
                        Surface(
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                rotationY = 180f
                                cameraDistance = 12f * density
                                alpha = if (rotation > 90f) 1f else 0f
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = ScSurfaceContainerLowest,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, ScTertiaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = ScTertiaryContainer
                                ) {
                                    Text(
                                        "Mặt sau",
                                        color = ScOnTertiaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    card.back,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ScTertiary,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp
                                )
                                if (isFlipped) {
                                    Spacer(Modifier.height(24.dp))
                                    Text(
                                        "← Cần xem lại   |   Đã thuộc →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScOnSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Front face
                        Surface(
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                                alpha = if (rotation <= 90f) 1f else 0f
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = ScSurfaceContainerLowest,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, ScPrimaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = ScPrimaryContainer
                                ) {
                                    Text(
                                        "Mặt trước",
                                        color = ScOnPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    card.front,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = ScOnSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp
                                )
                                Spacer(Modifier.height(32.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.TouchApp, null,
                                        tint = ScOutline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Nhấn để lật thẻ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ScOutline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Controls ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = !isFlipped,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                Button(
                    onClick = { isFlipped = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                ) {
                    Icon(Icons.Default.Sync, null, tint = ScOnPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Lật thẻ", color = ScOnPrimary,
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }

            AnimatedVisibility(
                visible = isFlipped,
                enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { 40 },
                exit = fadeOut(tween(150))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cần xem lại
                    Button(
                        onClick = {
                            if (currentIndex < cards.size - 1) {
                                currentIndex++
                            } else {
                                showComplete = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScErrorContainer)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.History, null,
                                tint = ScOnErrorContainer, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(2.dp))
                            Text("Cần xem lại", color = ScOnErrorContainer,
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    // Đã thuộc
                    Button(
                        onClick = {
                            studiedCount = (studiedCount + 1).coerceAtMost(cards.size)
                            if (currentIndex < cards.size - 1) {
                                currentIndex++
                            } else {
                                showComplete = true
                            }
                        },
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScPrimaryContainer)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = ScOnPrimaryContainer, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.height(2.dp))
                            Text("Đã thuộc", color = ScOnPrimaryContainer,
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── StudyCompleteScreen ───────────────────────────────────────────────────────

@Composable
fun StudyCompleteScreen(
    totalCards: Int,
    studiedCount: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val pct = if (totalCards > 0) (studiedCount * 100 / totalCards) else 0

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ScPrimaryContainer.copy(alpha = 0.4f), ScBackground)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Trophy icon with pulse
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(ScPrimaryContainer, ScPrimary.copy(alpha = 0.3f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents, null,
                        tint = ScPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Text(
                    "Hoàn thành!",
                    style = MaterialTheme.typography.displaySmall,
                    color = ScOnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Bạn đã học xong bộ thẻ này.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ScOnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Stats card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = ScSurfaceContainerLowest,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$studiedCount",
                                style = MaterialTheme.typography.headlineLarge,
                                color = ScPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Đã thuộc", style = MaterialTheme.typography.labelMedium,
                                color = ScOnSurfaceVariant)
                        }
                        Box(modifier = Modifier.width(1.dp).height(48.dp).background(ScOutlineVariant))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${totalCards - studiedCount}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = ScError,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Cần ôn lại", style = MaterialTheme.typography.labelMedium,
                                color = ScOnSurfaceVariant)
                        }
                        Box(modifier = Modifier.width(1.dp).height(48.dp).background(ScOutlineVariant))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$pct%",
                                style = MaterialTheme.typography.headlineLarge,
                                color = ScSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Tỉ lệ", style = MaterialTheme.typography.labelMedium,
                                color = ScOnSurfaceVariant)
                        }
                    }
                }

                // Buttons
                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                ) {
                    Icon(Icons.Default.Replay, null, tint = ScOnPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Học lại từ đầu", color = ScOnPrimary, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, ScOutlineVariant)
                ) {
                    Text("Quay lại bộ thẻ", color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── AddSingleCardDialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSingleCardDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column {
                // Gradient header
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ScPrimaryContainer.copy(0.5f), ScSurfaceContainerLowest)
                            ),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(ScPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = ScPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Thêm thẻ mới",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                            Text("Nhập nội dung mặt trước và mặt sau",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant)
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text("MẶT TRƯỚC", style = MaterialTheme.typography.labelSmall,
                        color = ScPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = front, onValueChange = { front = it },
                        placeholder = { Text("Câu hỏi hoặc thuật ngữ...", color = ScOutline) },
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScPrimary, unfocusedBorderColor = ScOutlineVariant,
                            focusedTextColor = ScOnSurface, unfocusedTextColor = ScOnSurface,
                            cursorColor = ScPrimary,
                            focusedContainerColor = ScPrimaryContainer.copy(0.08f),
                            unfocusedContainerColor = ScSurfaceContainerLow
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Divider(modifier = Modifier.weight(1f), color = ScOutlineVariant)
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp).size(32.dp)
                                .clip(CircleShape).background(ScPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SwapVert, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
                        }
                        Divider(modifier = Modifier.weight(1f), color = ScOutlineVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("MẶT SAU", style = MaterialTheme.typography.labelSmall,
                        color = ScTertiary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = back, onValueChange = { back = it },
                        placeholder = { Text("Định nghĩa hoặc câu trả lời...", color = ScOutline) },
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScTertiary, unfocusedBorderColor = ScOutlineVariant,
                            focusedTextColor = ScOnSurface, unfocusedTextColor = ScOnSurface,
                            cursorColor = ScTertiary,
                            focusedContainerColor = ScTertiaryContainer.copy(0.08f),
                            unfocusedContainerColor = ScSurfaceContainerLow
                        )
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(
                            onClick = { if (front.isNotBlank() && back.isNotBlank()) onAdd(front.trim(), back.trim()) },
                            enabled = front.isNotBlank() && back.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                        ) { Text("Thêm thẻ", color = ScOnPrimary) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── BulkAddDialog ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddDialog(onDismiss: () -> Unit, onParse: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val lineCount = input.lines().count { it.contains("~") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ScSecondaryContainer.copy(0.4f), ScSurfaceContainerLowest)
                            ),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(ScSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DynamicFeed, null, tint = ScSecondary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Thêm hàng loạt",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                            AnimatedContent(
                                targetState = lineCount,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "count"
                            ) { count ->
                                Text(
                                    if (count > 0) "$count thẻ được phát hiện" else "Nhập nhiều thẻ cùng lúc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (count > 0) ScSecondary else ScOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ScSecondaryContainer.copy(0.25f),
                        border = BorderStroke(1.dp, ScSecondaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = ScSecondary,
                                modifier = Modifier.size(16.dp).padding(top = 1.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Cách nhập:", style = MaterialTheme.typography.labelMedium,
                                    color = ScSecondary, fontWeight = FontWeight.Bold)
                                Text("Mỗi dòng: Mặt trước ~ Mặt sau",
                                    style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = ScSurfaceContainerLow) {
                                    Text(
                                        "Xin chào ~ Hello\nCảm ơn ~ Thank you",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ScOnSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        placeholder = { Text("Nhập nội dung thẻ...", color = ScOutline) },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        maxLines = 20,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScSecondary, unfocusedBorderColor = ScOutlineVariant,
                            focusedTextColor = ScOnSurface, unfocusedTextColor = ScOnSurface,
                            cursorColor = ScSecondary,
                            focusedContainerColor = ScSurfaceContainerLow,
                            unfocusedContainerColor = ScSurfaceContainerLow
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(
                            onClick = { onParse(input) },
                            enabled = input.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScSecondary)
                        ) {
                            Icon(Icons.Default.Visibility, null, tint = ScOnSecondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Xem trước", color = ScOnSecondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── BulkPreviewDialog ─────────────────────────────────────────────────────────

@Composable
fun BulkPreviewDialog(
    validCards: List<Pair<String, String>>,
    errorLines: List<String>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp).heightIn(max = 520.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(ScPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Preview, null, tint = ScPrimary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Xác nhận thêm thẻ",
                        style = MaterialTheme.typography.titleLarge,
                        color = ScOnSurface, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(10.dp), color = ScPrimaryContainer.copy(0.5f)) {
                        Text("✓ ${validCards.size} thẻ hợp lệ", color = ScPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (errorLines.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(10.dp), color = ScErrorContainer.copy(0.5f)) {
                            Text("✗ ${errorLines.size} lỗi", color = ScError,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(validCards) { (front, back) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ScSurfaceContainerLow)
                                .padding(12.dp)
                        ) {
                            Text(front, modifier = Modifier.weight(1f), color = ScOnSurface,
                                style = MaterialTheme.typography.bodySmall, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Text(" → ", color = ScOutline, style = MaterialTheme.typography.bodySmall)
                            Text(back, modifier = Modifier.weight(1f), color = ScTertiary,
                                style = MaterialTheme.typography.bodySmall, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (errorLines.isNotEmpty()) {
                        item {
                            Text("Dòng lỗi (không có dấu ~):", color = ScError,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                        items(errorLines) { line ->
                            Text("• $line", color = ScError.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ScErrorContainer.copy(0.3f))
                                    .padding(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancel, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(99.dp),
                        border = BorderStroke(1.dp, ScOutlineVariant)
                    ) { Text("Hủy bỏ", color = ScOnSurfaceVariant) }
                    Button(
                        onClick = onConfirm,
                        enabled = validCards.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                    ) { Text("Thêm ${validCards.size} thẻ", color = ScOnPrimary) }
                }
            }
        }
    }
}

// ── EditCardDialog ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(card: Flashcard, onDismiss: () -> Unit, onSave: (Flashcard) -> Unit) {
    var front by remember { mutableStateOf(card.front) }
    var back by remember { mutableStateOf(card.back) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ScTertiaryContainer.copy(0.4f), ScSurfaceContainerLowest)
                            ),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(ScTertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, null, tint = ScTertiary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Chỉnh sửa thẻ",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                            Text("Cập nhật nội dung mặt trước và mặt sau",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant)
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text("MẶT TRƯỚC", style = MaterialTheme.typography.labelSmall,
                        color = ScPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = front, onValueChange = { front = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScPrimary, unfocusedBorderColor = ScOutlineVariant,
                            focusedTextColor = ScOnSurface, unfocusedTextColor = ScOnSurface,
                            cursorColor = ScPrimary,
                            focusedContainerColor = ScPrimaryContainer.copy(0.08f),
                            unfocusedContainerColor = ScSurfaceContainerLow
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("MẶT SAU", style = MaterialTheme.typography.labelSmall,
                        color = ScTertiary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = back, onValueChange = { back = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScTertiary, unfocusedBorderColor = ScOutlineVariant,
                            focusedTextColor = ScOnSurface, unfocusedTextColor = ScOnSurface,
                            cursorColor = ScTertiary,
                            focusedContainerColor = ScTertiaryContainer.copy(0.08f),
                            unfocusedContainerColor = ScSurfaceContainerLow
                        )
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(
                            onClick = {
                                if (front.isNotBlank() && back.isNotBlank())
                                    onSave(card.copy(front = front.trim(), back = back.trim()))
                            },
                            enabled = front.isNotBlank() && back.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScTertiary)
                        ) { Text("Lưu thẻ", color = ScOnTertiary) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
