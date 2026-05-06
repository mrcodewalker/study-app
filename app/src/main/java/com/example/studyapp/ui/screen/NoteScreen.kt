package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.studyapp.data.model.Note
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

private val noteAccents = listOf(
    ScPrimary, ScSecondary, ScTertiary, ScWarning, ScError, Color(0xFF7B5EA7)
)
private val noteCardBgs = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFEAF5F1),
    Color(0xFFF0EDFF),
    Color(0xFFE8F3F9),
    Color(0xFFFFF8E1),
    Color(0xFFF5F0FF),
)
private val noteColorLabels = listOf("Trắng", "Xanh lá", "Tím", "Xanh dương", "Vàng", "Tím nhạt")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel) {
    val notes by viewModel.allNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    if (showEditor || editingNote != null) {
        NoteEditorScreen(
            note = editingNote,
            onSave = { title, content, color ->
                if (editingNote != null)
                    viewModel.updateNote(editingNote!!.copy(title = title, content = content, color = color))
                else
                    viewModel.createNote(title, content, color)
                showEditor = false; editingNote = null
            },
            onBack = { showEditor = false; editingNote = null }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(ScBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ScSurfaceContainerLowest, ScBackground)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ghi chú",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ScOnSurface, fontWeight = FontWeight.Bold
                        )
                        AnimatedContent(
                            targetState = notes.size,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "count"
                        ) { count ->
                            Text(
                                "$count ghi chú",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Tìm kiếm ghi chú...", color = ScOutline.copy(0.6f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = ScOutline, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank())
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, null, tint = ScOutline, modifier = Modifier.size(16.dp))
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = ScOnSurface,
                        unfocusedTextColor = ScOnSurface,
                        cursorColor = ScPrimary,
                        unfocusedContainerColor = ScSurfaceContainerLow,
                        focusedContainerColor = ScPrimaryContainer.copy(alpha = 0.2f)
                    )
                )
            }

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(90.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(ScPrimaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Note, null, Modifier.size(44.dp), tint = ScPrimary)
                        }
                        Text(
                            if (searchQuery.isBlank()) "Chưa có ghi chú nào"
                            else "Không tìm thấy kết quả",
                            color = ScOnSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (searchQuery.isBlank())
                            Text(
                                "Nhấn + để viết ghi chú đầu tiên",
                                color = ScOutline,
                                style = MaterialTheme.typography.bodyMedium
                            )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f).background(ScBackground),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    itemsIndexed(notes, key = { _, n -> n.id }) { idx, note ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(note.id) {
                            kotlinx.coroutines.delay((idx * 35).toLong())
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.92f)
                        ) {
                            NoteCard(
                                note = note,
                                onClick = { editingNote = note },
                                onDelete = { viewModel.deleteNote(note) }
                            )
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(88.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = ScPrimary,
            contentColor = ScOnPrimary,
            shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Icon(Icons.Default.Add, "Tạo ghi chú", modifier = Modifier.size(26.dp))
        }
    }
}

// ── NoteCard ──────────────────────────────────────────────────────────────────

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    val colorIdx = note.color.coerceIn(0, noteCardBgs.size - 1)
    val bg = noteCardBgs[colorIdx]
    val accent = noteAccents[colorIdx]
    val dateFormat = remember { SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()) }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (showMenu) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Confirm delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(ScErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = ScError, modifier = Modifier.size(22.dp))
                }
            },
            title = {
                Text("Xóa ghi chú?", fontWeight = FontWeight.Bold, color = ScOnSurface)
            },
            text = {
                Text(
                    "Ghi chú \"${note.title.ifBlank { "Không có tiêu đề" }}\" sẽ bị xóa vĩnh viễn.",
                    color = ScOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ScError),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text("Xóa", color = ScOnError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, ScOutlineVariant)
                ) {
                    Text("Hủy", color = ScOnSurfaceVariant)
                }
            },
            containerColor = ScSurfaceContainerLowest,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = bg,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: color dot + menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(accent)
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = accent.copy(alpha = 0.1f)
                ) {
                    Text(
                        noteColorLabels.getOrElse(colorIdx) { "Ghi chú" },
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert, null,
                            tint = ScOnSurfaceVariant.copy(0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(ScSurfaceContainerLowest)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
                                    Text("Chỉnh sửa", color = ScOnSurface)
                                }
                            },
                            onClick = { showMenu = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = ScError, modifier = Modifier.size(16.dp))
                                    Text("Xóa", color = ScError)
                                }
                            },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (note.title.isNotBlank()) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = ScOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
            }
            if (note.content.isNotBlank()) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant,
                    maxLines = if (note.title.isBlank()) 7 else 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            // Bottom: word count + date
            Row(verticalAlignment = Alignment.CenterVertically) {
                val wordCount = remember(note.content) {
                    note.content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
                }
                if (wordCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = accent.copy(alpha = 0.08f)
                    ) {
                        Text(
                            "$wordCount từ",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    dateFormat.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                    color = ScOutline.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── NoteEditorScreen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(note: Note?, onSave: (String, String, Int) -> Unit, onBack: () -> Unit) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedColor by remember { mutableStateOf(note?.color ?: 0) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val bg = noteCardBgs.getOrElse(selectedColor) { ScSurfaceContainerLowest }
    val accent = noteAccents.getOrElse(selectedColor) { ScPrimary }

    val isEdited = title != (note?.title ?: "") || content != (note?.content ?: "") || selectedColor != (note?.color ?: 0)
    val canSave = title.isNotBlank() || content.isNotBlank()
    val wordCount = remember(content) {
        content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
    }
    val charCount = content.length

    fun handleBack() {
        if (isEdited && canSave) showDiscardDialog = true else onBack()
    }

    // Discard changes dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(ScWarningLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = ScWarning, modifier = Modifier.size(22.dp))
                }
            },
            title = { Text("Bỏ thay đổi?", fontWeight = FontWeight.Bold, color = ScOnSurface) },
            text = {
                Text(
                    "Các thay đổi chưa được lưu sẽ bị mất.",
                    color = ScOnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDiscardDialog = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = ScWarning),
                    shape = RoundedCornerShape(99.dp)
                ) {
                    Text("Bỏ qua", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDiscardDialog = false },
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, ScOutlineVariant)
                ) {
                    Text("Tiếp tục sửa", color = ScOnSurfaceVariant)
                }
            },
            containerColor = ScSurfaceContainerLowest,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Color picker dialog
    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ScSurfaceContainerLowest,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Chọn màu ghi chú",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ScOnSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    noteCardBgs.forEachIndexed { index, color ->
                        val isSelected = selectedColor == index
                        val itemAccent = noteAccents[index]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedColor = index; showColorPicker = false },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) color else color.copy(alpha = 0.6f),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) itemAccent else ScOutlineVariant.copy(0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(20.dp).clip(CircleShape)
                                        .background(itemAccent)
                                )
                                Text(
                                    noteColorLabels[index],
                                    color = ScOnSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Spacer(Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = itemAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showColorPicker = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Đóng", color = ScOnSurfaceVariant)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Toolbar ──
            Surface(color = bg, shadowElevation = 0.dp) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { handleBack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = ScOnSurface)
                        }
                        Spacer(Modifier.weight(1f))

                        // Color picker button
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { showColorPicker = true },
                            shape = CircleShape,
                            color = bg,
                            border = BorderStroke(2.dp, accent)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Palette, null, tint = accent, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Save button
                        Button(
                            onClick = { onSave(title.trim(), content.trim(), selectedColor) },
                            enabled = canSave,
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                disabledContainerColor = ScOutlineVariant.copy(0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lưu", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Divider(color = accent.copy(alpha = 0.18f), thickness = 1.dp)
                }
            }

            // ── Title field ──
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Tiêu đề...",
                        color = ScOutline.copy(0.5f),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = ScOnSurface, fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent
                ),
                singleLine = true
            )

            // ── Metadata row (word count, char count) ──
            AnimatedVisibility(visible = content.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.1f)) {
                        Text(
                            "$wordCount từ",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = ScSurfaceContainerLow) {
                        Text(
                            "$charCount ký tự",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Content field ──
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("Bắt đầu viết...", color = ScOutline.copy(0.5f)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = ScOnSurface, lineHeight = 28.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent
                )
            )

            // ── Bottom bar ──
            Surface(color = bg.copy(alpha = 0.95f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = ScOutline, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (note != null)
                            "Sửa: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(note.updatedAt))}"
                        else
                            "Ghi chú mới",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScOutline
                    )
                    Spacer(Modifier.weight(1f))
                    // Unsaved indicator
                    AnimatedVisibility(visible = isEdited) {
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                "Chưa lưu",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
