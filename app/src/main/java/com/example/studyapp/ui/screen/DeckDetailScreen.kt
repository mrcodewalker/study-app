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

// ── StudyModeScreen — Tinder-style swipe ─────────────────────────────────────
// ── StudyModeScreen — Tinder-style swipe ─────────────────────────────────────

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

    // Drag state
    var dragOffsetX by remember { mutableStateOf(0f) }
    var isAnimatingOut by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableStateOf(0) } // -1=left(ôn), 1=right(thuộc)

    val animOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = if (isAnimatingOut)
            tween(300, easing = FastOutLinearInEasing)
        else
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "offsetX",
        finishedListener = {
            if (isAnimatingOut) {
                if (swipeDirection == 1) studiedCount = (studiedCount + 1).coerceAtMost(cards.size)
                if (currentIndex < cards.size - 1) currentIndex++
                else showComplete = true
                dragOffsetX = 0f
                isAnimatingOut = false
                swipeDirection = 0
                isFlipped = false
            }
        }
    )

    // Card entrance
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentIndex) {
        cardVisible = false
        kotlinx.coroutines.delay(60)
        cardVisible = true
        isFlipped = false
    }

    val swipeProgress = (animOffsetX / 380f).coerceIn(-1f, 1f)
    val cardRotation = swipeProgress * 10f
    val overlayAlpha = abs(swipeProgress).coerceIn(0f, 0.5f)
    val overlayColor = if (swipeProgress > 0) ScPrimary else ScError

    val card = cards[currentIndex]
    val progress = (currentIndex + 1).toFloat() / cards.size

    if (showComplete) {
        StudyCompleteScreen(
            totalCards = cards.size,
            studiedCount = studiedCount,
            onRestart = { currentIndex = 0; studiedCount = 0; showComplete = false },
            onExit = { onExit(currentIndex, studiedCount) }
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(
                listOf(ScPrimaryContainer.copy(alpha = 0.2f), ScBackground, ScBackground)
            ))
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
                    Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight()
                        .clip(RoundedCornerShape(99.dp)).background(ScPrimary))
                }
                Spacer(Modifier.width(12.dp))
                Text("${currentIndex + 1}/${cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ScOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }

            // ── Swipe hint labels ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val leftAlpha = (-swipeProgress).coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = ScErrorContainer.copy(alpha = 0.08f + leftAlpha * 0.5f),
                    border = BorderStroke(1.5.dp, ScError.copy(alpha = 0.15f + leftAlpha * 0.85f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Close, null,
                            tint = ScError.copy(alpha = 0.4f + leftAlpha * 0.6f),
                            modifier = Modifier.size(14.dp))
                        Text("Cần ôn", style = MaterialTheme.typography.labelSmall,
                            color = ScError.copy(alpha = 0.4f + leftAlpha * 0.6f),
                            fontWeight = FontWeight.Bold)
                    }
                }
                val rightAlpha = swipeProgress.coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = ScPrimaryContainer.copy(alpha = 0.08f + rightAlpha * 0.5f),
                    border = BorderStroke(1.5.dp, ScPrimary.copy(alpha = 0.15f + rightAlpha * 0.85f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Đã thuộc", style = MaterialTheme.typography.labelSmall,
                            color = ScPrimary.copy(alpha = 0.4f + rightAlpha * 0.6f),
                            fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Check, null,
                            tint = ScPrimary.copy(alpha = 0.4f + rightAlpha * 0.6f),
                            modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Card ─────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = cardVisible,
                enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.93f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = animOffsetX
                            translationY = abs(animOffsetX) * 0.08f
                            rotationZ = cardRotation
                        }
                        .pointerInput(currentIndex) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (!isAnimatingOut) {
                                        when {
                                            dragOffsetX > 120f -> {
                                                swipeDirection = 1; isAnimatingOut = true
                                                dragOffsetX = 900f
                                            }
                                            dragOffsetX < -120f -> {
                                                swipeDirection = -1; isAnimatingOut = true
                                                dragOffsetX = -900f
                                            }
                                            else -> dragOffsetX = 0f
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, delta ->
                                    if (!isAnimatingOut) dragOffsetX += delta
                                }
                            )
                        }
                        .clickable(enabled = !isAnimatingOut) { isFlipped = !isFlipped },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(24.dp),
                        color = ScSurfaceContainerLowest,
                        shadowElevation = 8.dp,
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                swipeProgress > 0.1f -> ScPrimary.copy(alpha = swipeProgress.coerceAtMost(1f))
                                swipeProgress < -0.1f -> ScError.copy(alpha = (-swipeProgress).coerceAtMost(1f))
                                isFlipped -> ScTertiaryContainer
                                else -> ScPrimaryContainer
                            }
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Color overlay on swipe
                            if (overlayAlpha > 0.01f) {
                                Box(modifier = Modifier.fillMaxSize()
                                    .background(overlayColor.copy(alpha = overlayAlpha),
                                        RoundedCornerShape(24.dp)))
                            }
                            // Crossfade front/back
                            Crossfade(targetState = isFlipped,
                                animationSpec = tween(220), label = "flip") { flipped ->
                                if (!flipped) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(shape = RoundedCornerShape(99.dp),
                                            color = ScPrimaryContainer) {
                                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.HelpOutline, null,
                                                    tint = ScPrimary, modifier = Modifier.size(12.dp))
                                                Text("Mặt trước", color = ScOnPrimaryContainer,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(Modifier.height(28.dp))
                                        Text(card.front,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center, lineHeight = 34.sp)
                                        Spacer(Modifier.height(36.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.TouchApp, null,
                                                tint = ScOutline, modifier = Modifier.size(16.dp))
                                            Text("Nhấn để xem đáp án",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ScOutline)
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(shape = RoundedCornerShape(99.dp),
                                            color = ScTertiaryContainer) {
                                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.LightMode, null,
                                                    tint = ScTertiary, modifier = Modifier.size(12.dp))
                                                Text("Đáp án", color = ScOnTertiaryContainer,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(Modifier.height(28.dp))
                                        Text(card.back,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = ScTertiary, fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center, lineHeight = 34.sp)
                                        Spacer(Modifier.height(28.dp))
                                        Surface(shape = RoundedCornerShape(12.dp),
                                            color = ScSurfaceContainerLow) {
                                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.SwipeLeft, null,
                                                    tint = ScError.copy(0.7f), modifier = Modifier.size(16.dp))
                                                Text("Cần ôn", color = ScOnSurfaceVariant,
                                                    style = MaterialTheme.typography.labelSmall)
                                                Text("·", color = ScOutlineVariant,
                                                    style = MaterialTheme.typography.labelSmall)
                                                Text("Đã thuộc", color = ScOnSurfaceVariant,
                                                    style = MaterialTheme.typography.labelSmall)
                                                Icon(Icons.Default.SwipeRight, null,
                                                    tint = ScPrimary.copy(0.7f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Stamp overlays
                    if (swipeProgress > 0.18f) {
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(20.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp),
                                color = ScPrimary.copy(alpha = (swipeProgress * 1.5f).coerceAtMost(0.9f)),
                                border = BorderStroke(2.dp, ScPrimary)) {
                                Text("THUỘC ✓", color = ScOnPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                    if (swipeProgress < -0.18f) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp),
                                color = ScError.copy(alpha = ((-swipeProgress) * 1.5f).coerceAtMost(0.9f)),
                                border = BorderStroke(2.dp, ScError)) {
                                Text("ÔN LẠI", color = ScOnError,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Bottom buttons ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (!isAnimatingOut) {
                            swipeDirection = -1; isAnimatingOut = true; dragOffsetX = -900f
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScErrorContainer)
                ) {
                    Icon(Icons.Default.Close, null, tint = ScError, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cần ôn", color = ScError, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        if (!isAnimatingOut) {
                            swipeDirection = 1; isAnimatingOut = true; dragOffsetX = 900f
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScPrimaryContainer)
                ) {
                    Icon(Icons.Default.Check, null, tint = ScPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Đã thuộc", color = ScPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

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

// ── Shared label composable ───────────────────────────────────────────────────

@Composable
private fun CardFaceLabel(
    text: String,
    hint: String,
    color: Color,
    bgColor: Color,
    onBgColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Spacer(Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(99.dp), color = bgColor) {
            Text(hint, style = MaterialTheme.typography.labelSmall, color = onBgColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
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
                    CardFaceLabel("MẶT TRƯỚC", "Câu hỏi / Thuật ngữ",
                        ScPrimary, ScPrimaryContainer, ScOnPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
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
                    CardFaceLabel("MẶT SAU", "Định nghĩa / Đáp án",
                        ScTertiary, ScTertiaryContainer, ScOnTertiaryContainer)
                    Spacer(Modifier.height(8.dp))
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

    // Live parse preview
    val previewCards = remember(input) {
        input.lines().filter { it.contains("~") }.mapNotNull { line ->
            val parts = line.split("~")
            if (parts.size >= 2) {
                val f = parts[0].trim(); val b = parts.drop(1).joinToString("~").trim()
                if (f.isNotBlank() && b.isNotBlank()) Pair(f, b) else null
            } else null
        }.take(4) // show max 4 in preview
    }
    val lineCount = previewCards.size

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Header
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(
                        listOf(ScSecondaryContainer.copy(0.4f), ScSurfaceContainerLowest)),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(ScSecondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint = ScSecondary,
                                modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Nhập hàng loạt",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                            AnimatedContent(targetState = lineCount,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "count") { count ->
                                Text(if (count > 0) "$count thẻ được phát hiện"
                                     else "Chuyển ghi chú thành bộ thẻ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (count > 0) ScSecondary else ScOnSurfaceVariant)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    // Instruction card
                    Surface(shape = RoundedCornerShape(14.dp),
                        color = ScSecondaryContainer.copy(0.25f),
                        border = BorderStroke(1.dp, ScSecondaryContainer)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = ScSecondary,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("CÁCH NHẬP", style = MaterialTheme.typography.labelSmall,
                                    color = ScSecondary, fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("Mỗi dòng một thẻ, dùng dấu ~ để phân cách",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ScOnSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Surface(shape = RoundedCornerShape(8.dp),
                                    color = ScSurfaceContainerLowest) {
                                    Text("Xin chào ~ Hello\nCảm ơn ~ Thank you",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ScOnSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(10.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // Text input
                    Box {
                        OutlinedTextField(
                            value = input, onValueChange = { input = it },
                            placeholder = { Text("Thuật ngữ ~ Định nghĩa\nMitochondria ~ Nhà máy điện của tế bào",
                                color = ScOutline, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 200.dp),
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
                        // Line count badge
                        if (lineCount > 0) {
                            Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                                shape = RoundedCornerShape(99.dp),
                                color = ScSecondaryContainer) {
                                Text("$lineCount dòng", color = ScSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Live preview
                    AnimatedVisibility(visible = previewCards.isNotEmpty()) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("XEM TRƯỚC", style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp)
                                Spacer(Modifier.weight(1f))
                                if (lineCount > 4) {
                                    Text("+ ${lineCount - 4} thẻ nữa",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScSecondary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            previewCards.forEachIndexed { idx, (front, back) ->
                                val accent = listOf(ScPrimary, ScSecondary, ScTertiary, ScPrimary)[idx % 3]
                                val accentBg = listOf(ScPrimaryContainer, ScSecondaryContainer,
                                    ScTertiaryContainer, ScPrimaryContainer)[idx % 3]
                                Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = ScSurfaceContainerLowest,
                                    border = BorderStroke(1.dp, accentBg)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        // Accent left bar
                                        Box(modifier = Modifier.width(4.dp).fillMaxHeight()
                                            .background(accent, RoundedCornerShape(
                                                topStart = 12.dp, bottomStart = 12.dp)))
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("TRƯỚC", style = MaterialTheme.typography.labelSmall,
                                                color = accent, fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp)
                                            Text(front, style = MaterialTheme.typography.bodyMedium,
                                                color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(6.dp))
                                            Divider(color = ScOutlineVariant)
                                            Spacer(Modifier.height(6.dp))
                                            Text("SAU", style = MaterialTheme.typography.labelSmall,
                                                color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp)
                                            Text(back, style = MaterialTheme.typography.bodySmall,
                                                color = ScOnSurfaceVariant, maxLines = 2,
                                                overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    // Action button
                    Button(
                        onClick = { onParse(input) },
                        enabled = lineCount > 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScSecondary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = ScOnSecondary,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nhập $lineCount thẻ", color = ScOnSecondary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Hủy", color = ScOnSurfaceVariant)
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
                    CardFaceLabel("MẶT TRƯỚC", "Câu hỏi / Thuật ngữ",
                        ScPrimary, ScPrimaryContainer, ScOnPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
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
                    CardFaceLabel("MẶT SAU", "Định nghĩa / Đáp án",
                        ScTertiary, ScTertiaryContainer, ScOnTertiaryContainer)
                    Spacer(Modifier.height(8.dp))
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
