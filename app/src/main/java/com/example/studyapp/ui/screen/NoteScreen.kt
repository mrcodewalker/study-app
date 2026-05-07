package com.example.studyapp.ui.screen

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.studyapp.data.model.Note
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.NoteSortOrder
import com.example.studyapp.ui.viewmodel.NoteViewModel
import com.example.studyapp.ui.util.loadAssetImage
import java.text.SimpleDateFormat
import java.util.*

// ── Palette ───────────────────────────────────────────────────────────────────

private val noteAccents = listOf(
    ScPrimary, ScSecondary, ScTertiary, ScWarning, ScError, Color(0xFF7B5EA7)
)
private val noteCardBgs = listOf(
    Color(0xFFFFFFFF), Color(0xFFEAF5F1), Color(0xFFF0EDFF),
    Color(0xFFE8F3F9), Color(0xFFFFF8E1), Color(0xFFF5F0FF),
)
private val noteColorLabels = listOf("Trắng", "Xanh lá", "Tím", "Xanh dương", "Vàng", "Tím nhạt")

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toUriList() = split(",").map(String::trim).filter(String::isNotBlank)
private fun List<String>.toJoined() = joinToString(",")

private fun extractUrls(text: String): List<String> {
    val regex = Regex("""https?://[^\s]+""")
    return regex.findAll(text).map { it.value }.toList()
}

// ── NoteScreen ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(viewModel: NoteViewModel) {
    val notes by viewModel.allNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterTag by viewModel.filterTag.collectAsState()
    val allTags by viewModel.allTags.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    if (showEditor || editingNote != null) {
        NoteEditorScreen(
            note = editingNote,
            onSave = { title, content, color, tags, imageUris, links ->
                if (editingNote != null)
                    viewModel.updateNote(
                        editingNote!!.copy(
                            title = title, content = content, color = color,
                            tags = tags, imageUris = imageUris, links = links
                        )
                    )
                else
                    viewModel.createNote(title, content, color, tags, imageUris, links)
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
                    .background(Brush.verticalGradient(listOf(ScSurfaceContainerLowest, ScBackground)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ghi chú", style = MaterialTheme.typography.headlineMedium,
                            color = ScOnSurface, fontWeight = FontWeight.Bold
                        )
                        AnimatedContent(
                            targetState = notes.size,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "count"
                        ) { count ->
                            Text("$count ghi chú", style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant)
                        }
                    }
                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, null, tint = ScOnSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(ScSurfaceContainerLowest)
                        ) {
                            Text(
                                "Sắp xếp theo", style = MaterialTheme.typography.labelSmall,
                                color = ScOnSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            NoteSortOrder.values().forEach { order ->
                                val label = when (order) {
                                    NoteSortOrder.UPDATED -> "Cập nhật gần nhất"
                                    NoteSortOrder.CREATED -> "Tạo gần nhất"
                                    NoteSortOrder.TITLE   -> "Tên A-Z"
                                }
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (sortOrder == order)
                                                Icon(Icons.Default.Check, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
                                            else Spacer(Modifier.size(16.dp))
                                            Text(label, color = if (sortOrder == order) ScPrimary else ScOnSurface)
                                        }
                                    },
                                    onClick = { viewModel.setSortOrder(order); showSortMenu = false }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Tìm kiếm ghi chú...", color = ScOutline.copy(0.6f)) },
                    leadingIcon = {
                        Image(loadAssetImage("3d-magnifier.png"), null, modifier = Modifier.size(24.dp))
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
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = ScOnSurface, unfocusedTextColor = ScOnSurface,
                        cursorColor = ScPrimary,
                        unfocusedContainerColor = ScSurfaceContainerLow,
                        focusedContainerColor = ScPrimaryContainer.copy(alpha = 0.2f)
                    )
                )

                // Tag filter chips
                if (allTags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterTag == null,
                                onClick = { viewModel.setFilterTag(null) },
                                label = { Text("Tất cả") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScPrimary,
                                    selectedLabelColor = ScOnPrimary
                                )
                            )
                        }
                        items(allTags) { tag ->
                            FilterChip(
                                selected = filterTag == tag,
                                onClick = { viewModel.setFilterTag(if (filterTag == tag) null else tag) },
                                label = { Text("#$tag") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScPrimary,
                                    selectedLabelColor = ScOnPrimary
                                )
                            )
                        }
                    }
                }
            }

            if (notes.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                            Image(loadAssetImage("sticky-note.png"), null, Modifier.fillMaxSize())
                        }
                        Text(
                            if (searchQuery.isBlank() && filterTag == null) "Chưa có ghi chú nào"
                            else "Không tìm thấy kết quả",
                            color = ScOnSurface, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (searchQuery.isBlank() && filterTag == null)
                            Text("Nhấn + để viết ghi chú đầu tiên", color = ScOutline, style = MaterialTheme.typography.bodyMedium)
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
                                onDelete = { viewModel.deleteNote(note) },
                                onTogglePin = { viewModel.togglePin(note) }
                            )
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(88.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { showEditor = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = ScPrimary, contentColor = ScOnPrimary,
            shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Icon(Icons.Default.Add, "Tạo ghi chú", modifier = Modifier.size(26.dp))
        }
    }
}

// ── NoteCard ──────────────────────────────────────────────────────────────────

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDelete: () -> Unit, onTogglePin: () -> Unit) {
    val colorIdx = note.color.coerceIn(0, noteCardBgs.size - 1)
    val bg = noteCardBgs[colorIdx]
    val accent = noteAccents[colorIdx]
    val dateFormat = remember { SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()) }
    val tags = remember(note.tags) { note.tags.toUriList() }
    val images = remember(note.imageUris) { note.imageUris.toUriList() }
    val links = remember(note.links) { note.links.toUriList() }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (showMenu) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale"
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(ScErrorContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Delete, null, tint = ScError, modifier = Modifier.size(22.dp)) }
            },
            title = { Text("Xóa ghi chú?", fontWeight = FontWeight.Bold, color = ScOnSurface) },
            text = {
                Text(
                    "Ghi chú \"${note.title.ifBlank { "Không có tiêu đề" }}\" sẽ bị xóa vĩnh viễn.",
                    color = ScOnSurfaceVariant, style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ScError),
                    shape = RoundedCornerShape(99.dp)
                ) { Text("Xóa", color = ScOnError, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, ScOutlineVariant)
                ) { Text("Hủy", color = ScOnSurfaceVariant) }
            },
            containerColor = ScSurfaceContainerLowest, shape = RoundedCornerShape(24.dp)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().scale(scale).clickable { onClick() },
        shape = RoundedCornerShape(18.dp), color = bg, shadowElevation = 2.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPinned) {
                    Icon(Icons.Default.PushPin, null, tint = accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(6.dp))
                Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.1f)) {
                    Text(
                        noteColorLabels.getOrElse(colorIdx) { "Ghi chú" },
                        color = accent, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = ScOnSurfaceVariant.copy(0.6f), modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu, onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(ScSurfaceContainerLowest)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
                                    Text("Chỉnh sửa", color = ScOnSurface)
                                }
                            },
                            onClick = { showMenu = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(
                                        if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        null, tint = ScSecondary, modifier = Modifier.size(16.dp)
                                    )
                                    Text(if (note.isPinned) "Bỏ ghim" else "Ghim", color = ScOnSurface)
                                }
                            },
                            onClick = { showMenu = false; onTogglePin() }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

            // Thumbnail image (first image only)
            if (images.isNotEmpty()) {
                AsyncImage(
                    model = Uri.parse(images.first()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.height(8.dp))
            }

            if (note.title.isNotBlank()) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
            }
            if (note.content.isNotBlank()) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodySmall, color = ScOnSurfaceVariant,
                    maxLines = if (note.title.isBlank()) 7 else 4, overflow = TextOverflow.Ellipsis
                )
            }

            // Tags
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    tags.take(3).forEach { tag ->
                        Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.12f)) {
                            Text(
                                "#$tag", style = MaterialTheme.typography.labelSmall,
                                color = accent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Link indicator
            if (links.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Link, null, tint = ScPrimary.copy(0.7f), modifier = Modifier.size(12.dp))
                    Text(
                        "${links.size} liên kết",
                        style = MaterialTheme.typography.labelSmall, color = ScPrimary.copy(0.7f)
                    )
                    if (images.size > 1) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Image, null, tint = ScSecondary.copy(0.7f), modifier = Modifier.size(12.dp))
                        Text(
                            "${images.size} ảnh",
                            style = MaterialTheme.typography.labelSmall, color = ScSecondary.copy(0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Bottom: word count + date
            Row(verticalAlignment = Alignment.CenterVertically) {
                val wordCount = remember(note.content) {
                    note.content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
                }
                if (wordCount > 0) {
                    Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.08f)) {
                        Text(
                            "$wordCount từ", style = MaterialTheme.typography.labelSmall,
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
fun NoteEditorScreen(
    note: Note?,
    onSave: (title: String, content: String, color: Int, tags: String, imageUris: String, links: String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var contentTfv by remember {
        mutableStateOf(TextFieldValue(note?.content ?: ""))
    }
    var selectedColor by remember { mutableStateOf(note?.color ?: 0) }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(note?.tags?.toUriList()?.toMutableList() ?: mutableListOf()) }
    var imageUris by remember { mutableStateOf(note?.imageUris?.toUriList()?.toMutableList() ?: mutableListOf()) }
    var linkInput by remember { mutableStateOf("") }
    var links by remember { mutableStateOf(note?.links?.toUriList()?.toMutableList() ?: mutableListOf()) }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0=write, 1=attachments
    var fullscreenUri by remember { mutableStateOf<String?>(null) }

    val bg = noteCardBgs.getOrElse(selectedColor) { ScSurfaceContainerLowest }
    val accent = noteAccents.getOrElse(selectedColor) { ScPrimary }

    val content = contentTfv.text
    val isEdited = title != (note?.title ?: "") || content != (note?.content ?: "") ||
            selectedColor != (note?.color ?: 0) || tags != (note?.tags?.toUriList() ?: emptyList<String>()) ||
            imageUris != (note?.imageUris?.toUriList() ?: emptyList<String>()) ||
            links != (note?.links?.toUriList() ?: emptyList<String>())
    val canSave = title.isNotBlank() || content.isNotBlank()
    val wordCount = remember(content) { content.trim().split("\\s+".toRegex()).count { it.isNotEmpty() } }
    val charCount = content.length

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            // Persist permission
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            if (!imageUris.contains(uri.toString())) imageUris = (imageUris + uri.toString()).toMutableList()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) imageLauncher.launch("image/*")
    }

    fun pickImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        else
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Formatting helpers
    fun wrapSelection(prefix: String, suffix: String = prefix) {
        val sel = contentTfv.selection
        val text = contentTfv.text
        if (sel.collapsed) {
            val newText = text.substring(0, sel.start) + prefix + suffix + text.substring(sel.end)
            contentTfv = contentTfv.copy(
                text = newText,
                selection = TextRange(sel.start + prefix.length)
            )
        } else {
            val selected = text.substring(sel.start, sel.end)
            val newText = text.substring(0, sel.start) + prefix + selected + suffix + text.substring(sel.end)
            contentTfv = contentTfv.copy(
                text = newText,
                selection = TextRange(sel.start, sel.end + prefix.length + suffix.length)
            )
        }
    }

    fun insertAtCursor(insert: String) {
        val sel = contentTfv.selection
        val text = contentTfv.text
        val newText = text.substring(0, sel.start) + insert + text.substring(sel.end)
        contentTfv = contentTfv.copy(
            text = newText,
            selection = TextRange(sel.start + insert.length)
        )
    }

    fun handleBack() { if (isEdited && canSave) showDiscardDialog = true else onBack() }

    // Discard dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(ScWarningLight),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Warning, null, tint = ScWarning, modifier = Modifier.size(22.dp)) }
            },
            title = { Text("Bỏ thay đổi?", fontWeight = FontWeight.Bold, color = ScOnSurface) },
            text = { Text("Các thay đổi chưa được lưu sẽ bị mất.", color = ScOnSurfaceVariant, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { showDiscardDialog = false; onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = ScWarning),
                    shape = RoundedCornerShape(99.dp)
                ) { Text("Bỏ qua", color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDiscardDialog = false },
                    shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, ScOutlineVariant)
                ) { Text("Tiếp tục sửa", color = ScOnSurfaceVariant) }
            },
            containerColor = ScSurfaceContainerLowest, shape = RoundedCornerShape(24.dp)
        )
    }

    // Color picker dialog
    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = ScSurfaceContainerLowest, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Chọn màu ghi chú", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ScOnSurface)
                    Spacer(Modifier.height(16.dp))
                    noteCardBgs.forEachIndexed { index, color ->
                        val isSelected = selectedColor == index
                        val itemAccent = noteAccents[index]
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { selectedColor = index; showColorPicker = false },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) color else color.copy(alpha = 0.6f),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) itemAccent else ScOutlineVariant.copy(0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(itemAccent))
                                Text(noteColorLabels[index], color = ScOnSurface, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                Spacer(Modifier.weight(1f))
                                if (isSelected) Icon(Icons.Default.Check, null, tint = itemAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showColorPicker = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Đóng", color = ScOnSurfaceVariant)
                    }
                }
            }
        }
    }

    // Add link dialog
    if (showLinkDialog) {
        Dialog(onDismissRequest = { showLinkDialog = false; linkInput = "" }) {
            Surface(shape = RoundedCornerShape(24.dp), color = ScSurfaceContainerLowest, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Thêm liên kết", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ScOnSurface)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = linkInput, onValueChange = { linkInput = it },
                        placeholder = { Text("https://...") },
                        leadingIcon = { Icon(Icons.Default.Link, null, tint = ScPrimary) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showLinkDialog = false; linkInput = "" },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp)
                        ) { Text("Hủy") }
                        Button(
                            onClick = {
                                val url = linkInput.trim()
                                if (url.isNotBlank() && !links.contains(url)) {
                                    links = (links + url).toMutableList()
                                }
                                showLinkDialog = false; linkInput = ""
                            },
                            enabled = linkInput.isNotBlank(),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) { Text("Thêm", color = Color.White) }
                    }
                }
            }
        }
    }

    // Add tag dialog
    if (showTagDialog) {
        Dialog(onDismissRequest = { showTagDialog = false; tagInput = "" }) {
            Surface(shape = RoundedCornerShape(24.dp), color = ScSurfaceContainerLowest, shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Thêm thẻ tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ScOnSurface)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tagInput, onValueChange = { tagInput = it.replace(" ", "").replace(",", "") },
                        placeholder = { Text("tên-tag (không dấu cách)") },
                        leadingIcon = { Text("#", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (tags.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Tags hiện tại:", style = MaterialTheme.typography.labelSmall, color = ScOnSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            tags.forEach { tag ->
                                InputChip(
                                    selected = false, onClick = { tags = tags.toMutableList().also { it.remove(tag) } },
                                    label = { Text("#$tag") },
                                    trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showTagDialog = false; tagInput = "" },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp)
                        ) { Text("Đóng") }
                        Button(
                            onClick = {
                                val t = tagInput.trim()
                                if (t.isNotBlank() && !tags.contains(t)) tags = (tags + t).toMutableList()
                                tagInput = ""
                            },
                            enabled = tagInput.isNotBlank(),
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) { Text("Thêm", color = Color.White) }
                    }
                }
            }
        }
    }

    // ── Fullscreen image viewer ──
    fullscreenUri?.let { uri ->
        Dialog(onDismissRequest = { fullscreenUri = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .clickable { fullscreenUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = Uri.parse(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                )
                // Close button
                IconButton(
                    onClick = { fullscreenUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // ── Main layout ──
    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Toolbar
            Surface(color = bg, shadowElevation = 0.dp) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { handleBack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = ScOnSurface)
                        }
                        Spacer(Modifier.weight(1f))
                        // Tag button
                        IconButton(onClick = { showTagDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (tags.isNotEmpty()) Badge(containerColor = accent) {
                                        Text("${tags.size}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Label, null, tint = if (tags.isNotEmpty()) accent else ScOnSurfaceVariant)
                            }
                        }
                        // Color picker
                        Surface(
                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable { showColorPicker = true },
                            shape = CircleShape, color = bg, border = BorderStroke(2.dp, accent)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(accent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Palette, null, tint = accent, modifier = Modifier.size(16.dp)) }
                        }
                        Spacer(Modifier.width(8.dp))
                        // Save button
                        Button(
                            onClick = {
                                onSave(
                                    title.trim(), content.trim(), selectedColor,
                                    tags.toJoined(), imageUris.toJoined(), links.toJoined()
                                )
                            },
                            enabled = canSave, shape = RoundedCornerShape(99.dp),
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

                    // Tab row: Viết / Đính kèm
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = bg,
                        contentColor = accent,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                color = accent, height = 2.dp
                            )
                        }
                    ) {
                        Tab(selected = activeTab == 0, onClick = { activeTab = 0 },
                            text = { Text("✏️ Viết", style = MaterialTheme.typography.labelMedium) })
                        Tab(selected = activeTab == 1, onClick = { activeTab = 1 },
                            text = {
                                BadgedBox(badge = {
                                    val cnt = imageUris.size + links.size
                                    if (cnt > 0) Badge(containerColor = accent) {
                                        Text("$cnt", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }) { Text("📎 Đính kèm", style = MaterialTheme.typography.labelMedium) }
                            })
                    }
                }
            }

            when (activeTab) {
                0 -> {
                    // ── Write tab ──
                    Column(modifier = Modifier.weight(1f)) {
                        // Title
                        TextField(
                            value = title, onValueChange = { title = it },
                            placeholder = {
                                Text("Tiêu đề...", color = ScOutline.copy(0.5f),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(color = ScOnSurface, fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = accent
                            ),
                            singleLine = true
                        )

                        // Stats row
                        AnimatedVisibility(visible = content.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.1f)) {
                                    Text("$wordCount từ", style = MaterialTheme.typography.labelSmall, color = accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontWeight = FontWeight.SemiBold)
                                }
                                Surface(shape = RoundedCornerShape(99.dp), color = ScSurfaceContainerLow) {
                                    Text("$charCount ký tự", style = MaterialTheme.typography.labelSmall, color = ScOnSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }

                        // Formatting toolbar
                        Surface(color = bg.copy(alpha = 0.95f), shadowElevation = 1.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FormatButton("B", accent) { wrapSelection("**") }
                                FormatButton("I", accent, italic = true) { wrapSelection("_") }
                                FormatButton("S̶", accent) { wrapSelection("~~") }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).padding(horizontal = 4.dp).background(ScOutlineVariant.copy(0.4f)))
                                FormatIconButton(Icons.Default.FormatListBulleted, accent) { insertAtCursor("\n• ") }
                                FormatIconButton(Icons.Default.FormatListNumbered, accent) { insertAtCursor("\n1. ") }
                                FormatIconButton(Icons.Default.CheckBox, accent) { insertAtCursor("\n☐ ") }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).padding(horizontal = 4.dp).background(ScOutlineVariant.copy(0.4f)))
                                FormatButton("H1", accent) { insertAtCursor("\n# ") }
                                FormatButton("H2", accent) { insertAtCursor("\n## ") }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).padding(horizontal = 4.dp).background(ScOutlineVariant.copy(0.4f)))
                                FormatIconButton(Icons.Default.Link, accent) { showLinkDialog = true }
                                FormatIconButton(Icons.Default.Image, accent) { pickImages() }
                                FormatIconButton(Icons.Default.HorizontalRule, accent) { insertAtCursor("\n---\n") }
                            }
                        }

                        // Content field
                        TextField(
                            value = contentTfv,
                            onValueChange = { contentTfv = it },
                            placeholder = { Text("Bắt đầu viết...", color = ScOutline.copy(0.5f)) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ScOnSurface, lineHeight = 28.sp),
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 4.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = accent
                            )
                        )
                    }
                }
                1 -> {
                    // ── Attachments tab ──
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Images section
                        Text("Hình ảnh", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ScOnSurface)
                        if (imageUris.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { pickImages() },
                                shape = RoundedCornerShape(16.dp),
                                color = accent.copy(alpha = 0.06f),
                                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = accent, modifier = Modifier.size(36.dp))
                                    Text("Thêm ảnh từ thư viện", color = accent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            // Image grid
                            val chunked = imageUris.chunked(2)
                            chunked.forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { uri ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            AsyncImage(
                                                model = Uri.parse(uri), contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { fullscreenUri = uri }
                                            )
                                            IconButton(
                                                onClick = { imageUris = imageUris.toMutableList().also { it.remove(uri) } },
                                                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                                                    .padding(4.dp).clip(CircleShape).background(Color.Black.copy(0.5f))
                                            ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                            OutlinedButton(
                                onClick = { pickImages() }, modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, accent.copy(0.4f))
                            ) {
                                Icon(Icons.Default.Add, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Thêm ảnh", color = accent)
                            }
                        }

                        Divider(color = ScOutlineVariant.copy(0.3f))

                        // Links section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Liên kết", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ScOnSurface, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showLinkDialog = true }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Thêm")
                            }
                        }
                        if (links.isEmpty()) {
                            Text("Chưa có liên kết nào", color = ScOutline, style = MaterialTheme.typography.bodySmall)
                        } else {
                            links.forEach { url ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ScSurfaceContainerLow,
                                    border = BorderStroke(1.dp, ScOutlineVariant.copy(0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(shape = RoundedCornerShape(8.dp), color = ScPrimary.copy(0.1f)) {
                                            Icon(Icons.Default.Language, null, tint = ScPrimary, modifier = Modifier.padding(6.dp).size(18.dp))
                                        }
                                        Text(
                                            url, style = MaterialTheme.typography.bodySmall, color = ScPrimary,
                                            textDecoration = TextDecoration.Underline,
                                            modifier = Modifier.weight(1f).clickable {
                                                try { uriHandler.openUri(url) } catch (_: Exception) {}
                                            },
                                            maxLines = 2, overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(
                                            onClick = { links = links.toMutableList().also { it.remove(url) } },
                                            modifier = Modifier.size(24.dp)
                                        ) { Icon(Icons.Default.Close, null, tint = ScError, modifier = Modifier.size(14.dp)) }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(80.dp))
                    }
                }
            }

            // Bottom bar
            Surface(color = bg.copy(alpha = 0.95f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = ScOutline, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (note != null)
                            "Sửa: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(note.updatedAt))}"
                        else "Ghi chú mới",
                        style = MaterialTheme.typography.labelSmall, color = ScOutline
                    )
                    Spacer(Modifier.weight(1f))
                    AnimatedVisibility(visible = isEdited) {
                        Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.12f)) {
                            Text(
                                "Chưa lưu", style = MaterialTheme.typography.labelSmall,
                                color = accent, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Small reusable format buttons ─────────────────────────────────────────────

@Composable
private fun FormatButton(label: String, accent: Color, italic: Boolean = false, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label, color = accent, fontWeight = FontWeight.Bold,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun FormatIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
    }
}
