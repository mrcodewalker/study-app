package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.studyapp.data.model.FlashcardDeck
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.FlashcardViewModel

private val deckChipColors = listOf(
    Triple(ScPrimaryContainer, ScOnPrimaryContainer, ScPrimary),
    Triple(ScSecondaryContainer, ScOnSecondaryContainer, ScSecondary),
    Triple(ScTertiaryContainer, ScOnTertiaryContainer, ScTertiary),
)

/** Format "X giờ trước / X ngày trước / vừa xong" từ epoch millis */
private fun formatLastStudied(lastStudiedAt: Long?): String? {
    if (lastStudiedAt == null || lastStudiedAt == 0L) return null
    val diff = System.currentTimeMillis() - lastStudiedAt
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "Vừa học xong"
        minutes < 60 -> "$minutes phút trước"
        hours < 24 -> "$hours giờ trước"
        days == 1L -> "Hôm qua"
        days < 30 -> "$days ngày trước"
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(viewModel: FlashcardViewModel, onDeckClick: (Long) -> Unit) {
    val decks by viewModel.allDecks.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<FlashcardDeck?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<FlashcardDeck?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(ScBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScSurfaceContainerLowest)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    "Bộ thẻ của bạn",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ScOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Ôn tập và ghi nhớ kiến thức hiệu quả.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScOnSurfaceVariant
                )
            }

            if (decks.isEmpty()) {
                // ── Empty state ──────────────────────────────────────────────
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(ScPrimaryContainer.copy(alpha = 0.4f))
                                .border(2.dp, ScOutlineVariant, RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Style, null,
                                Modifier.size(52.dp), tint = ScPrimary
                            )
                        }
                        Text(
                            "Tạo bộ thẻ đầu tiên",
                            style = MaterialTheme.typography.titleMedium,
                            color = ScOnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Nhấn + để bắt đầu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ScOnSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).background(ScBackground),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(decks, key = { _, d -> d.id }) { idx, deck ->
                        val colorIdx = (deck.id % 3).toInt()
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(deck.id) {
                            kotlinx.coroutines.delay((idx * 60).toLong())
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { 30 }
                        ) {
                            DeckCard(
                                deck, colorIdx,
                                onClick = { onDeckClick(deck.id) },
                                onEdit = { showEditDialog = deck },
                                onDelete = { showDeleteConfirm = deck }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = ScPrimary,
            contentColor = ScOnPrimary,
            shape = RoundedCornerShape(16.dp)
        ) { Icon(Icons.Default.Add, "Tạo bộ thẻ mới") }
    }

    if (showCreateDialog) {
        CreateDeckDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc -> viewModel.createDeck(name, desc); showCreateDialog = false }
        )
    }
    showEditDialog?.let { deck ->
        EditDeckDialog(
            deck,
            onDismiss = { showEditDialog = null },
            onSave = { viewModel.updateDeck(it); showEditDialog = null }
        )
    }
    showDeleteConfirm?.let { deck ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = ScSurfaceContainerLowest,
            title = {
                Text("Xóa bộ thẻ?", color = ScOnSurface, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "Bộ thẻ \"${deck.name}\" và tất cả flashcard sẽ bị xóa.",
                    color = ScOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteDeck(deck); showDeleteConfirm = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ScError),
                    shape = RoundedCornerShape(99.dp)
                ) { Text("Xóa", color = ScOnError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Hủy", color = ScOnSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun DeckCard(
    deck: FlashcardDeck,
    colorIdx: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (chipBg, chipText, accent) = deckChipColors[colorIdx]

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val progressFraction = if (deck.cardCount > 0)
        (deck.studiedCount.toFloat() / deck.cardCount).coerceIn(0f, 1f)
    else 0f
    val progress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "progress"
    )
    val progressPct = (progressFraction * 100).toInt()

    // lastStudiedAt field — fallback to createdAt if not present
    val lastStudiedLabel = formatLastStudied(
        if (deck.studiedCount > 0) deck.createdAt else null
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { pressed = true; onClick() },
        shape = RoundedCornerShape(20.dp),
        color = ScSurfaceContainerLowest,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Top row: chip + actions ──────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(99.dp), color = chipBg) {
                    Text(
                        "Thẻ học",
                        color = chipText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit, null,
                        tint = ScOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete, null,
                        tint = ScError.copy(0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Deck name ────────────────────────────────────────────────
            Text(
                deck.name,
                style = MaterialTheme.typography.headlineSmall,
                color = ScOnSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 30.sp
            )

            Spacer(Modifier.height(6.dp))

            // ── Card count ───────────────────────────────────────────────
            Text(
                "${deck.cardCount} thẻ",
                style = MaterialTheme.typography.bodyMedium,
                color = ScOnSurfaceVariant
            )

            // ── Last studied label ───────────────────────────────────────
            if (lastStudiedLabel != null) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History, null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        lastStudiedLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = ScOnSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Progress bar ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(ScSurfaceContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.65f)))
                        )
                )
            }

            if (progressPct > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "$progressPct% hoàn thành",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Shared dialog components ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false,
    maxLines: Int = 4
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ScOnSurfaceVariant) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else maxLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ScPrimary,
            unfocusedBorderColor = ScOutlineVariant,
            focusedTextColor = ScOnSurface,
            unfocusedTextColor = ScOnSurface,
            cursorColor = ScPrimary,
            focusedLabelColor = ScPrimary,
            unfocusedLabelColor = ScOnSurfaceVariant,
            focusedContainerColor = ScSurfaceContainerLowest,
            unfocusedContainerColor = ScSurfaceContainerLow
        )
    )
}

@Composable
fun ScDialogButtons(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmText: String = "Lưu"
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) {
            Text("Hủy", color = ScOnSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            shape = RoundedCornerShape(99.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ScPrimary,
                disabledContainerColor = ScPrimaryContainer
            )
        ) { Text(confirmText, color = ScOnPrimary) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeckDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Tạo bộ thẻ mới",
                    style = MaterialTheme.typography.titleLarge,
                    color = ScOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(20.dp))
                ScTextField(name, { name = it }, "Tên bộ thẻ *", singleLine = true)
                Spacer(Modifier.height(12.dp))
                ScTextField(desc, { desc = it }, "Mô tả (tuỳ chọn)", maxLines = 3)
                Spacer(Modifier.height(24.dp))
                ScDialogButtons(
                    onDismiss,
                    { if (name.isNotBlank()) onCreate(name.trim(), desc.trim()) },
                    name.isNotBlank(),
                    "Tạo"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeckDialog(
    deck: FlashcardDeck,
    onDismiss: () -> Unit,
    onSave: (FlashcardDeck) -> Unit
) {
    var name by remember { mutableStateOf(deck.name) }
    var desc by remember { mutableStateOf(deck.description) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Chỉnh sửa bộ thẻ",
                    style = MaterialTheme.typography.titleLarge,
                    color = ScOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(20.dp))
                ScTextField(name, { name = it }, "Tên bộ thẻ", singleLine = true)
                Spacer(Modifier.height(12.dp))
                ScTextField(desc, { desc = it }, "Mô tả", maxLines = 3)
                Spacer(Modifier.height(24.dp))
                ScDialogButtons(
                    onDismiss,
                    {
                        if (name.isNotBlank())
                            onSave(deck.copy(name = name.trim(), description = desc.trim()))
                    },
                    name.isNotBlank(),
                    "Lưu"
                )
            }
        }
    }
}
