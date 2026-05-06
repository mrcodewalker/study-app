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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.data.model.Note
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

// Note card accent colors (cycling)
private val noteAccents = listOf(ScPrimary, ScSecondary, ScTertiary, ScWarning, ScError, ScPrimary)
private val noteCardBgs = listOf(
    Color(0xFFEAF5F1), // mint
    Color(0xFFF0EDFF), // lavender
    Color(0xFFE8F3F9), // sky
    Color(0xFFFFF8E1), // amber
    Color(0xFFFFEEEE), // rose
    Color(0xFFF3F0FF), // purple
)

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
                modifier = Modifier.fillMaxWidth()
                    .background(ScSurfaceContainerLowest)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ghi chú",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ScOnSurface, fontWeight = FontWeight.Bold)
                        Text("${notes.size} ghi chú",
                            style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant)
                    }
                    // Sort/filter icon placeholder
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.GridView, null, tint = ScOnSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Search bar (Stitch style: grey bg, no border)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Tìm kiếm ghi chú...", color = ScOutline.copy(0.6f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null,
                            tint = ScOutline, modifier = Modifier.size(20.dp))
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
                Box(modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape)
                                .background(ScPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Note, null, Modifier.size(40.dp), tint = ScPrimary)
                        }
                        Text(
                            if (searchQuery.isBlank()) "Chưa có ghi chú nào"
                            else "Không tìm thấy kết quả",
                            color = ScOnSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (searchQuery.isBlank())
                            Text("Nhấn + để viết ghi chú đầu tiên",
                                color = ScOutline, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                // ── Staggered 2-column grid (like Stitch / Google Keep) ──
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
                            kotlinx.coroutines.delay((idx * 40).toLong())
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f)
                        ) {
                            NoteCard(note,
                                onClick = { editingNote = note },
                                onDelete = { viewModel.deleteNote(note) })
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = ScPrimary, contentColor = ScOnPrimary,
            shape = RoundedCornerShape(16.dp)
        ) { Icon(Icons.Default.Add, "Tạo ghi chú") }
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    val colorIdx = note.color.coerceIn(0, noteCardBgs.size - 1)
    val bg = noteCardBgs[colorIdx]
    val accent = noteAccents[colorIdx]
    val dateFormat = remember { SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()) }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable {
            pressed = true; onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = bg,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Subject chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Text("Ghi chú", color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MoreVert, null,
                        tint = ScOnSurfaceVariant.copy(0.5f), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            if (note.title.isNotBlank()) {
                Text(note.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
            }
            if (note.content.isNotBlank()) {
                Text(note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant,
                    maxLines = if (note.title.isBlank()) 6 else 4,
                    overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                dateFormat.format(Date(note.updatedAt)).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = ScOutline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(note: Note?, onSave: (String, String, Int) -> Unit, onBack: () -> Unit) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var selectedColor by remember { mutableStateOf(note?.color ?: 0) }
    val bg = noteCardBgs.getOrElse(selectedColor) { ScSurfaceContainerLowest }
    val accent = noteAccents.getOrElse(selectedColor) { ScPrimary }

    val canSave = title.isNotBlank() || content.isNotBlank()
    val wordCount = remember(content) { content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() } }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Toolbar ──
            Surface(
                color = bg,
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = ScOnSurface)
                        }
                        Spacer(Modifier.weight(1f))
                        // Color swatches
                        noteCardBgs.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(if (selectedColor == index) 26.dp else 22.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (selectedColor == index) 2.dp else 1.dp,
                                        if (selectedColor == index) noteAccents[index]
                                        else ScOutlineVariant.copy(0.5f),
                                        CircleShape
                                    )
                                    .clickable { selectedColor = index }
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        // Save button
                        Button(
                            onClick = { onSave(title.trim(), content.trim(), selectedColor) },
                            enabled = canSave,
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                disabledContainerColor = ScOutlineVariant.copy(0.3f)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lưu", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Divider(color = accent.copy(alpha = 0.15f), thickness = 1.dp)
                }
            }

            // ── Title field ──
            TextField(
                value = title, onValueChange = { title = it },
                placeholder = {
                    Text("Tiêu đề...", color = ScOutline,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = ScOnSurface, fontWeight = FontWeight.Bold),
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

            // ── Content field ──
            TextField(
                value = content, onValueChange = { content = it },
                placeholder = { Text("Bắt đầu viết...", color = ScOutline) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = ScOnSurface, lineHeight = 26.sp),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent
                )
            )

            // ── Bottom bar: word count + date ──
            Surface(color = bg.copy(alpha = 0.95f)) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TextFields, null,
                        tint = ScOutline, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$wordCount từ",
                        style = MaterialTheme.typography.labelSmall, color = ScOutline)
                    Spacer(Modifier.weight(1f))
                    if (note != null) {
                        Text(
                            "Sửa lần cuối: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(note.updatedAt))}",
                            style = MaterialTheme.typography.labelSmall, color = ScOutline
                        )
                    }
                }
            }
        }
    }
}
