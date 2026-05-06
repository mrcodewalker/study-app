package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.EaseOutBack
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
import androidx.compose.material3.*import androidx.compose.runtime.*
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
    var showStudyOptions by remember { mutableStateOf(false) }
    // null = học tất cả, non-null = học lại các thẻ chưa thuộc
    var studySubset by remember { mutableStateOf<List<Flashcard>?>(null) }
    // Key để force recompose StudyModeScreen khi restart
    var studySessionKey by remember { mutableStateOf(0) }
    // Override initialStudiedCount — null = dùng từ DB
    var overrideInitialStudied by remember { mutableStateOf<Int?>(null) }

    if (studyMode && cards.isNotEmpty()) {
        val startIndex = deck?.lastStudiedIndex?.coerceIn(0, cards.size - 1) ?: 0
        val baseCards = studySubset ?: cards
        val studyCards = if (shuffleMode) remember(baseCards) { baseCards.shuffled() } else baseCards
        val isSubset = studySubset != null
        val initStudied = overrideInitialStudied ?: (if (isSubset) 0 else (deck?.studiedCount ?: 0))
        key(studySessionKey) {
            StudyModeScreen(
                cards = studyCards,
                initialIndex = if (shuffleMode || isSubset || (overrideInitialStudied == 0)) 0 else startIndex,
                initialStudiedCount = initStudied,
                onExit = { lastIdx, studiedCnt ->
                    if (isSubset) {
                        // Ôn lại chưa thuộc: chỉ cộng thêm số câu mới thuộc vào tổng, giữ nguyên vị trí cũ của bộ chính
                        val prevTotal = deck?.studiedCount ?: 0
                        val mainTotalSize = cards.size
                        val newTotal = (prevTotal + studiedCnt).coerceAtMost(mainTotalSize)
                        val currentMainIdx = deck?.lastStudiedIndex ?: 0
                        viewModel.saveStudyProgress(deckId, currentMainIdx, newTotal)
                    } else {
                        // Học bình thường: lưu vị trí thực tế và tổng số câu thuộc
                        viewModel.saveStudyProgress(deckId, lastIdx, studiedCnt)
                    }
                    studySubset = null
                    overrideInitialStudied = null
                    studyMode = false
                },
                onRestart = {
                    studySubset = null
                    overrideInitialStudied = 0
                    viewModel.saveStudyProgress(deckId, 0, 0)
                    studySessionKey++
                },
                onRestartNotLearned = { notLearned ->
                    studySubset = notLearned
                    overrideInitialStudied = 0
                    studySessionKey++
                }
            )
        }
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
                    val lastIdx = deck?.lastStudiedIndex ?: 0
                    val totalCount = cards.size
                    val progressPct = if (totalCount > 0) (lastIdx * 100 / totalCount) else 0
                    Text(
                        if (lastIdx > 0) "$lastIdx/$totalCount thẻ đã xem ($studiedCount thuộc) · $progressPct%"
                        else "${cards.size} thẻ",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (lastIdx > 0) ScPrimary else ScOnSurfaceVariant
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
                    IconButton(onClick = { showStudyOptions = true }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Học", tint = ScPrimary)
                    }
                }
            }

            // ── Progress bar ─────────────────────────────────────────────────
            val studiedCount = deck?.studiedCount ?: 0
            val totalCount = cards.size
            if (studiedCount > 0 && totalCount > 0) {
                val progress by animateFloatAsState(
                    targetValue = (deck?.lastStudiedIndex?.toFloat() ?: 0f) / totalCount,
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

    if (showStudyOptions) {
        val studiedSoFar = deck?.studiedCount ?: 0
        val lastIdx = deck?.lastStudiedIndex ?: 0
        // "Tiếp tục" chỉ khi đang dở giữa chừng: đã có lastIndex > 0 và chưa đến thẻ cuối
        val canContinue = lastIdx > 0 && lastIdx < cards.size - 1
        val notLearnedCount = (cards.size - studiedSoFar).coerceAtLeast(0)
        StudyOptionsDialog(
            totalCards = cards.size,
            notLearnedCount = cards.size - studiedSoFar,
            hasProgress = canContinue,
            onDismiss = { showStudyOptions = false },
            onContinue = {
                showStudyOptions = false
                studySubset = null
                overrideInitialStudied = null
                studySessionKey++
                studyMode = true
            },
            onRestartAll = {
                showStudyOptions = false
                studySubset = null
                overrideInitialStudied = 0
                viewModel.saveStudyProgress(deckId, 0, 0)
                studySessionKey++
                studyMode = true
            },
            onRestartNotLearned = {
                showStudyOptions = false
                studySubset = if (studiedSoFar < cards.size) cards.drop(studiedSoFar) else cards
                overrideInitialStudied = 0
                studySessionKey++
                studyMode = true
            }
        )
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
        BulkInsertScreen(
            deckName = deck?.name ?: "",
            onBack = { showAddBulk = false },
            onParse = { input ->
                viewModel.parseBulkInput(input)
                viewModel.confirmBulkInsert(deckId)
                showAddBulk = false
            }
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

@Composable
fun StudyModeScreen(
    cards: List<Flashcard>,
    initialIndex: Int,
    initialStudiedCount: Int,
    onExit: (Int, Int) -> Unit,
    onRestart: () -> Unit,
    onRestartNotLearned: (List<Flashcard>) -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, cards.size - 1)) }
    var studiedCount by remember { mutableStateOf(initialStudiedCount) }
    var isFlipped by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }

    // Track các index chưa thuộc (swipe trái)
    val notLearnedIndices = remember { mutableStateListOf<Int>() }
    // Track số câu đã trả lời (cả 2 chiều) cho progress bar - Khởi tạo từ initialIndex để đúng tiến trình khi học tiếp
    var answeredCount by remember { mutableStateOf(initialIndex) }

    // Drag & swipe state
    var dragOffsetX by remember { mutableStateOf(0f) }
    var isAnimatingOut by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableStateOf(0) } // -1=left, 1=right

    // Ngưỡng swipe: phải kéo >320dp mới trigger
    val SWIPE_THRESHOLD = 320f
    val SCREEN_WIDTH = 420f // approximate dp

    val animOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = if (isAnimatingOut)
            tween(380, easing = FastOutLinearInEasing)
        else
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "offsetX",
        finishedListener = {
            if (isAnimatingOut) {
                answeredCount++
                if (swipeDirection == 1) {
                    // Đã thuộc
                    studiedCount = (studiedCount + 1).coerceAtMost(cards.size)
                } else {
                    // Chưa thuộc — lưu index lại
                    notLearnedIndices.add(currentIndex)
                }
                if (currentIndex < cards.size - 1) {
                    currentIndex++
                } else {
                    showComplete = true
                }
                dragOffsetX = 0f
                isAnimatingOut = false
                swipeDirection = 0
                isFlipped = false
            }
        }
    )

    // 3D flip animation
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "flip"
    )

    // Card entrance animation
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentIndex) {
        cardVisible = false
        kotlinx.coroutines.delay(50)
        cardVisible = true
        isFlipped = false
    }

    // swipeProgress: 0 ở giữa, chỉ bắt đầu có màu khi kéo >30% ngưỡng
    val rawProgress = (animOffsetX / SCREEN_WIDTH).coerceIn(-1f, 1f)
    // Màu chỉ hiện khi kéo >40% ngưỡng (tức ~0.25 của màn hình)
    val colorThreshold = 0.25f
    val swipeColorProgress = when {
        rawProgress > colorThreshold -> ((rawProgress - colorThreshold) / (1f - colorThreshold)).coerceIn(0f, 1f)
        rawProgress < -colorThreshold -> ((-rawProgress - colorThreshold) / (1f - colorThreshold)).coerceIn(0f, 1f)
        else -> 0f
    }
    val swipeSign = if (rawProgress > 0) 1f else if (rawProgress < 0) -1f else 0f
    val cardRotation = rawProgress * 12f
    val overlayAlpha = swipeColorProgress * 0.45f
    val overlayColor = if (rawProgress > 0) ScPrimary else ScError

    val card = cards[currentIndex]
    val progress = answeredCount.toFloat() / cards.size

    if (showComplete) {
        val notLearnedCards = notLearnedIndices.map { cards[it] }
        StudyCompleteScreen(
            totalCards = cards.size,
            studiedCount = studiedCount,
            notLearnedCount = notLearnedIndices.size,
            onRestartAll = {
                currentIndex = 0
                studiedCount = 0
                answeredCount = 0
                notLearnedIndices.clear()
                showComplete = false
                onRestart()
            },
            onRestartNotLearned = {
                if (notLearnedCards.isNotEmpty()) {
                    onRestartNotLearned(notLearnedCards)
                }
            },
            onContinue = { onExit(cards.size, studiedCount) } // Khi hoàn thành, vị trí cuối cùng là cards.size
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ScPrimaryContainer.copy(alpha = 0.15f),
                        ScBackground,
                        ScBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onExit(answeredCount, studiedCount) }) {
                    Icon(Icons.Default.Close, null, tint = ScOnSurface)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(ScPrimaryContainer.copy(alpha = 0.4f))
                ) {
                    val animProg by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(600, easing = EaseOutCubic),
                        label = "prog"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animProg)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                Brush.horizontalGradient(listOf(ScPrimary, ScPrimaryFixedDim))
                            )
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "$answeredCount/${cards.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ScOnSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Card ─────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = cardVisible,
                enter = fadeIn(tween(200)) + scaleIn(
                    tween(280, easing = EaseOutBack),
                    initialScale = 0.88f
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = animOffsetX
                            translationY = abs(animOffsetX) * 0.06f
                            rotationZ = cardRotation
                            // Slight scale down when dragging
                            val dragScale = 1f - abs(rawProgress) * 0.04f
                            scaleX = dragScale
                            scaleY = dragScale
                        }
                        .pointerInput(currentIndex) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (!isAnimatingOut) {
                                        when {
                                            dragOffsetX > SWIPE_THRESHOLD -> {
                                                swipeDirection = 1
                                                isAnimatingOut = true
                                                dragOffsetX = 1100f
                                            }
                                            dragOffsetX < -SWIPE_THRESHOLD -> {
                                                swipeDirection = -1
                                                isAnimatingOut = true
                                                dragOffsetX = -1100f
                                            }
                                            else -> {
                                                // Snap back với spring
                                                dragOffsetX = 0f
                                            }
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, delta ->
                                    if (!isAnimatingOut) dragOffsetX += delta
                                }
                            )
                        }
                        .clickable(
                            enabled = !isAnimatingOut,
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) { isFlipped = !isFlipped },
                    contentAlignment = Alignment.Center
                ) {
                    // ── 3D Flip Card ──────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = flipRotation
                                cameraDistance = 12f * density
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Mặt trước (hiện khi chưa lật)
                        if (flipRotation <= 90f) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(28.dp),
                                color = ScSurfaceContainerLowest,
                                shadowElevation = 12.dp,
                                border = BorderStroke(
                                    1.5.dp,
                                    when {
                                        swipeColorProgress > 0f && rawProgress > 0 ->
                                            ScPrimary.copy(alpha = swipeColorProgress)
                                        swipeColorProgress > 0f && rawProgress < 0 ->
                                            ScError.copy(alpha = swipeColorProgress)
                                        else -> ScPrimaryContainer.copy(alpha = 0.6f)
                                    }
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Gradient overlay khi swipe
                                    if (overlayAlpha > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    overlayColor.copy(alpha = overlayAlpha),
                                                    RoundedCornerShape(28.dp)
                                                )
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(99.dp),
                                            color = ScPrimaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.HelpOutline, null,
                                                    tint = ScPrimary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    "Mặt trước",
                                                    color = ScOnPrimaryContainer,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(28.dp))
                                        Text(
                                            card.front,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = ScOnSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 34.sp
                                        )
                                        Spacer(Modifier.height(28.dp))
                                        // Tap hint
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
                                        Spacer(Modifier.height(10.dp))
                                        // Swipe hint
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = ScSurfaceContainerLow.copy(alpha = 0.8f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("👈", style = MaterialTheme.typography.labelMedium)
                                                Text(
                                                    "Cần ôn",
                                                    color = ScError.copy(0.8f),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text("·", color = ScOutlineVariant,
                                                    style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    "Đã thuộc",
                                                    color = ScPrimary.copy(0.8f),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text("👉", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Mặt sau (hiện khi đã lật, cần mirror lại)
                        if (flipRotation > 90f) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { rotationY = 180f },
                                shape = RoundedCornerShape(28.dp),
                                color = ScSurfaceContainerLowest,
                                shadowElevation = 12.dp,
                                border = BorderStroke(
                                    1.5.dp,
                                    when {
                                        swipeColorProgress > 0f && rawProgress > 0 ->
                                            ScPrimary.copy(alpha = swipeColorProgress)
                                        swipeColorProgress > 0f && rawProgress < 0 ->
                                            ScError.copy(alpha = swipeColorProgress)
                                        else -> ScTertiaryContainer.copy(alpha = 0.8f)
                                    }
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (overlayAlpha > 0.01f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    overlayColor.copy(alpha = overlayAlpha),
                                                    RoundedCornerShape(28.dp)
                                                )
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(99.dp),
                                            color = ScTertiaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LightMode, null,
                                                    tint = ScTertiary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    "Đáp án",
                                                    color = ScOnTertiaryContainer,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(28.dp))
                                        Text(
                                            card.back,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = ScTertiary,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 34.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Stamp overlays (chỉ hiện khi kéo đủ xa) ─────────
                    val stampAlpha = swipeColorProgress
                    if (stampAlpha > 0.05f && rawProgress > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(24.dp)
                                .graphicsLayer {
                                    alpha = stampAlpha
                                    rotationZ = -15f
                                    scaleX = 0.8f + stampAlpha * 0.2f
                                    scaleY = 0.8f + stampAlpha * 0.2f
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent,
                                border = BorderStroke(3.dp, ScPrimary)
                            ) {
                                Text(
                                    "THUỘC ✓",
                                    color = ScPrimary,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    if (stampAlpha > 0.05f && rawProgress < 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .graphicsLayer {
                                    alpha = stampAlpha
                                    rotationZ = 15f
                                    scaleX = 0.8f + stampAlpha * 0.2f
                                    scaleY = 0.8f + stampAlpha * 0.2f
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent,
                                border = BorderStroke(3.dp, ScError)
                            ) {
                                Text(
                                    "ÔN LẠI",
                                    color = ScError,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Bottom buttons: Quay lại | Cần ôn | Đã thuộc ─────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút Quay lại — disabled ở thẻ đầu tiên
                val canGoBack = currentIndex > 0
                FilledTonalIconButton(
                    onClick = {
                        if (canGoBack && !isAnimatingOut) {
                            currentIndex--
                            isFlipped = false
                        }
                    },
                    enabled = canGoBack,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = ScSurfaceContainerLow,
                        contentColor = ScOnSurface,
                        disabledContainerColor = ScSurfaceContainerLow.copy(alpha = 0.4f),
                        disabledContentColor = ScOnSurface.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(22.dp))
                }

                // Cần ôn
                Button(
                    onClick = {
                        if (!isAnimatingOut) {
                            swipeDirection = -1
                            isAnimatingOut = true
                            dragOffsetX = -1100f
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

                // Đã thuộc
                Button(
                    onClick = {
                        if (!isAnimatingOut) {
                            swipeDirection = 1
                            isAnimatingOut = true
                            dragOffsetX = 1100f
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
    notLearnedCount: Int,
    onRestartAll: () -> Unit,
    onRestartNotLearned: () -> Unit,
    onContinue: () -> Unit
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
                if (notLearnedCount > 0) {
                    Button(
                        onClick = onRestartNotLearned,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                shadowElevation = 8f
                                shape = RoundedCornerShape(99.dp)
                                clip = true
                            },
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScError,
                            contentColor = ScOnError
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Làm lại $notLearnedCount câu chưa thuộc",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Button(
                    onClick = onRestartAll,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                ) {
                    Icon(Icons.Default.Replay, null, tint = ScOnPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Học lại từ đầu", color = ScOnPrimary, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onContinue,
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

// ── StudyOptionsDialog ────────────────────────────────────────────────────────

@Composable
fun StudyOptionsDialog(
    totalCards: Int,
    notLearnedCount: Int,
    hasProgress: Boolean,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onRestartAll: () -> Unit,
    onRestartNotLearned: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(ScPrimaryContainer.copy(0.5f), ScSurfaceContainerLowest)
                            ),
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(ScPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = ScPrimary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Bắt đầu học",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$totalCards thẻ trong bộ này",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Option 1: Tiếp tục (chỉ hiện nếu có progress)
                    if (hasProgress) {
                        StudyOptionItem(
                            icon = Icons.Default.PlayCircle,
                            iconBg = ScPrimaryContainer,
                            iconTint = ScPrimary,
                            title = "Tiếp tục học",
                            subtitle = "Tiếp tục từ chỗ đã dừng",
                            onClick = onContinue
                        )
                    }

                    // Option 2: Ôn lại chưa thuộc (chỉ hiện nếu có câu chưa thuộc)
                    if (notLearnedCount > 0) {
                        StudyOptionItem(
                            icon = Icons.Default.Refresh,
                            iconBg = ScErrorContainer,
                            iconTint = ScError,
                            title = "Ôn lại chưa thuộc",
                            subtitle = "$notLearnedCount thẻ cần ôn thêm",
                            onClick = onRestartNotLearned
                        )
                    }

                    // Option 3: Học lại từ đầu
                    StudyOptionItem(
                        icon = Icons.Default.Replay,
                        iconBg = ScSecondaryContainer,
                        iconTint = ScSecondary,
                        title = "Học lại từ đầu",
                        subtitle = "Reset và học toàn bộ $totalCards thẻ",
                        onClick = onRestartAll
                    )

                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hủy", color = ScOnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = ScSurfaceContainerLow,
        border = BorderStroke(1.dp, ScOutlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    color = ScOnSurface, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = ScOutline, modifier = Modifier.size(20.dp))
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

// BulkAddDialog has been replaced by BulkInsertScreen.kt for a better UI experience.



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
