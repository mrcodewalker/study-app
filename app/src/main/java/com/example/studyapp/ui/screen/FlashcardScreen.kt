package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.studyapp.ui.util.loadAssetImage
import com.example.studyapp.ui.util.ScTextField

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
                                .size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                loadAssetImage("play-button.png"), null,
                                Modifier.fillMaxSize()
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
                    itemsIndexed(
                        items = decks,
                        key = { _, d -> d.id }
                    ) { idx, deck ->
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
        Dialog(onDismissRequest = { showDeleteConfirm = null }) {
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
                                    listOf(ScErrorContainer.copy(0.5f), ScSurfaceContainerLowest)
                                ),
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                    .background(ScErrorContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DeleteForever, null,
                                    tint = ScError, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Xóa bộ thẻ?",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ScOnSurface, fontWeight = FontWeight.Bold)
                                Text("Hành động này không thể hoàn tác",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ScOnSurfaceVariant)
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ScErrorContainer.copy(0.25f),
                            border = BorderStroke(1.dp, ScErrorContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Warning, null,
                                    tint = ScError, modifier = Modifier.size(18.dp))
                                Text(
                                    "Bộ thẻ \"${deck.name}\" và tất cả flashcard bên trong sẽ bị xóa vĩnh viễn.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ScOnSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = null },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(99.dp),
                                border = BorderStroke(1.dp, ScOutlineVariant)
                            ) { Text("Hủy", color = ScOnSurfaceVariant) }
                            Button(
                                onClick = { viewModel.deleteDeck(deck); showDeleteConfirm = null },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(99.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ScError)
                            ) {
                                Icon(Icons.Default.Delete, null,
                                    tint = ScOnError, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Xóa", color = ScOnError, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
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

    // lastStudiedAt — dùng field thực từ model
    val lastStudiedLabel = formatLastStudied(
        if (deck.lastStudiedAt > 0L) deck.lastStudiedAt else null
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

            // ── Last studied + card count row ────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "${deck.cardCount} thẻ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScOnSurfaceVariant
                )
                if (lastStudiedLabel != null) {
                    Box(modifier = Modifier.size(4.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(ScOutlineVariant))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.History, null,
                            tint = accent, modifier = Modifier.size(13.dp))
                        Text(lastStudiedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ScOnSurfaceVariant)
                    }
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
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(ScPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CreateNewFolder, null, 
                                tint = ScPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Tạo bộ thẻ mới",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                            Text("Bắt đầu hành trình học tập",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant)
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    ScTextField(name, { name = it }, "Tên bộ thẻ *", singleLine = true)
                    Spacer(Modifier.height(14.dp))
                    ScTextField(desc, { desc = it }, "Mô tả (tuỳ chọn)", maxLines = 3)
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(
                            onClick = { if (name.isNotBlank()) onCreate(name.trim(), desc.trim()) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScPrimary,
                                disabledContainerColor = ScPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Add, null, 
                                tint = ScOnPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tạo thẻ", color = ScOnPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
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
                                listOf(ScTertiaryContainer.copy(0.4f), ScSurfaceContainerLowest)
                            ),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(ScTertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, null, 
                                tint = ScTertiary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Chỉnh sửa bộ thẻ",
                                style = MaterialTheme.typography.titleLarge,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                            Text("Cập nhật thông tin bộ thẻ",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant)
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    ScTextField(name, { name = it }, "Tên bộ thẻ", singleLine = true)
                    Spacer(Modifier.height(14.dp))
                    ScTextField(desc, { desc = it }, "Mô tả", maxLines = 3)
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            border = BorderStroke(1.dp, ScOutlineVariant)
                        ) { Text("Hủy", color = ScOnSurfaceVariant) }
                        Button(
                            onClick = {
                                if (name.isNotBlank())
                                    onSave(deck.copy(name = name.trim(), description = desc.trim()))
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ScTertiary)
                        ) {
                            Icon(Icons.Default.Save, null, 
                                tint = ScOnTertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lưu thay đổi", color = ScOnTertiary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
