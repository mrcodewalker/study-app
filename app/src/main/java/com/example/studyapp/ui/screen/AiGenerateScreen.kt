package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.ai.AiGenerateState
import com.example.studyapp.ai.LlmInferenceManager
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.FlashcardViewModel

private val LANGUAGE_OPTIONS = listOf("Vietnamese", "English", "Japanese", "Korean", "Chinese")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerateScreen(
    deckId: Long,
    deckName: String,
    viewModel: FlashcardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val aiState by viewModel.aiState.collectAsState()

    var topic by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("10") }
    var language by remember { mutableStateOf("Vietnamese") }
    var showLangDropdown by remember { mutableStateOf(false) }
    var showModelHelp by remember { mutableStateOf(false) }

    val modelAvailable = remember { LlmInferenceManager.isModelAvailable(context) }
    val modelPath = remember { LlmInferenceManager.getModelPath(context) }

    // Auto-reset on success after delay
    LaunchedEffect(aiState) {
        if (aiState is AiGenerateState.Success) {
            kotlinx.coroutines.delay(2000)
            viewModel.resetAiState()
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ScBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(ScSurfaceContainerLowest, ScBackground))
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.resetAiState()
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, null, tint = ScOnSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AI Generate",
                        style = MaterialTheme.typography.titleLarge,
                        color = ScOnSurface, fontWeight = FontWeight.Bold
                    )
                    Text(
                        deckName,
                        style = MaterialTheme.typography.bodySmall,
                        color = ScOnSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                // Sparkles icon
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(listOf(ScPrimaryContainer, ScTertiaryContainer))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = ScPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Model status banner ──────────────────────────────────
                ModelStatusBanner(
                    available = modelAvailable,
                    modelPath = modelPath,
                    onHelpClick = { showModelHelp = true }
                )

                Spacer(Modifier.height(20.dp))

                // ── Input form ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = aiState is AiGenerateState.Idle || aiState is AiGenerateState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Topic
                        Column {
                            Text(
                                "CHỦ ĐỀ",
                                style = MaterialTheme.typography.labelSmall,
                                color = ScOnSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = topic,
                                onValueChange = { topic = it },
                                placeholder = { Text("VD: Cấu trúc dữ liệu, Lịch sử Việt Nam...", color = ScOutline) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = aiTextFieldColors()
                            )
                        }

                        // Count + Language row
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "SỐ LƯỢNG",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = countText,
                                    onValueChange = { v ->
                                        if (v.isEmpty() || v.all { it.isDigit() }) countText = v
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = aiTextFieldColors()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "NGÔN NGỮ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = showLangDropdown,
                                    onExpandedChange = { showLangDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = language,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLangDropdown)
                                        },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = aiTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = showLangDropdown,
                                        onDismissRequest = { showLangDropdown = false }
                                    ) {
                                        LANGUAGE_OPTIONS.forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text(lang) },
                                                onClick = { language = lang; showLangDropdown = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Error message
                        if (aiState is AiGenerateState.Error) {
                            val err = aiState as AiGenerateState.Error
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ScErrorContainer.copy(0.4f),
                                border = BorderStroke(1.dp, ScErrorContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = ScError, modifier = Modifier.size(18.dp))
                                    Text(
                                        err.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ScOnSurface
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Generate button
                        val count = countText.toIntOrNull()?.coerceIn(1, 50) ?: 10
                        Button(
                            onClick = {
                                viewModel.generateFlashcards(
                                    context = context,
                                    topic = topic.trim(),
                                    count = count,
                                    language = language
                                )
                            },
                            enabled = topic.isNotBlank() && modelAvailable,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScPrimary,
                                disabledContainerColor = ScPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = ScOnPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Tạo $count flashcard với AI",
                                color = ScOnPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // ── Loading / Generating state ───────────────────────────
                AnimatedVisibility(
                    visible = aiState is AiGenerateState.LoadingModel || aiState is AiGenerateState.Generating,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(200))
                ) {
                    GeneratingView(aiState = aiState)
                }

                // ── Preview state ────────────────────────────────────────
                AnimatedVisibility(
                    visible = aiState is AiGenerateState.Preview,
                    enter = fadeIn(tween(300)) + expandVertically(tween(350)),
                    exit = fadeOut(tween(200))
                ) {
                    val preview = aiState as? AiGenerateState.Preview
                    if (preview != null) {
                        PreviewSection(
                            cards = preview.cards,
                            errorLines = preview.errorLines,
                            onConfirm = { viewModel.confirmAiInsert(deckId) },
                            onDiscard = { viewModel.resetAiState() }
                        )
                    }
                }

                // ── Success state ────────────────────────────────────────
                AnimatedVisibility(visible = aiState is AiGenerateState.Success) {
                    val success = aiState as? AiGenerateState.Success
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "Đã thêm ${success?.insertedCount} thẻ!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ScOnSurface, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showModelHelp) {
        ModelHelpDialog(modelPath = modelPath, onDismiss = { showModelHelp = false })
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ModelStatusBanner(available: Boolean, modelPath: String, onHelpClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (available) Color(0xFF4CAF50).copy(0.1f) else ScErrorContainer.copy(0.3f),
        border = BorderStroke(
            1.dp,
            if (available) Color(0xFF4CAF50).copy(0.4f) else ScErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (available) Icons.Default.CheckCircle else Icons.Default.Warning,
                null,
                tint = if (available) Color(0xFF4CAF50) else ScError,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (available) "Model AI sẵn sàng" else "Chưa có model AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (available) Color(0xFF2E7D32) else ScError,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (available) LlmInferenceManager.DEFAULT_MODEL_FILE
                    else "Cần tải model để dùng tính năng này",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (!available) {
                TextButton(
                    onClick = onHelpClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Hướng dẫn", color = ScPrimary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun GeneratingView(aiState: AiGenerateState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(listOf(ScPrimaryContainer, ScTertiaryContainer.copy(0.5f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome, null,
                tint = ScPrimary.copy(alpha = alpha),
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            when (aiState) {
                is AiGenerateState.LoadingModel -> "Đang tải model AI..."
                is AiGenerateState.Generating -> "Đang tạo flashcard..."
                else -> ""
            },
            style = MaterialTheme.typography.titleMedium,
            color = ScOnSurface, fontWeight = FontWeight.SemiBold
        )

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.6f).clip(RoundedCornerShape(99.dp)),
            color = ScPrimary,
            trackColor = ScPrimaryContainer
        )

        // Stream preview
        if (aiState is AiGenerateState.Generating && aiState.streamedText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ScSurfaceContainerLow,
                border = BorderStroke(1.dp, ScOutlineVariant)
            ) {
                Text(
                    aiState.streamedText.takeLast(300),
                    style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PreviewSection(
    cards: List<Pair<String, String>>,
    errorLines: List<String>,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    val accentColors = listOf(ScPrimary, ScSecondary, ScTertiary)
    val accentBgColors = listOf(ScPrimaryContainer, ScSecondaryContainer, ScTertiaryContainer)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // ── Header row ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LIVE PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = ScOnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(99.dp), color = ScPrimaryContainer) {
                Text(
                    "${cards.size} thẻ",
                    color = ScPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // ── 2-column grid (same layout as BulkInsertScreen) ──────────────
        cards.chunked(2).forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (front, back) ->
                    val globalIdx = cards.indexOf(Pair(front, back))
                    AiPreviewCard(
                        front = front,
                        back = back,
                        accent = accentColors[globalIdx % accentColors.size],
                        accentBg = accentBgColors[globalIdx % accentBgColors.size],
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill empty slot if odd number of cards
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // ── Error lines ──────────────────────────────────────────────────
        if (errorLines.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = ScError, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "DÒNG LỖI (bỏ qua ${errorLines.size} dòng thiếu ~)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScError, fontWeight = FontWeight.Bold
                )
            }
            errorLines.forEach { line ->
                Text(
                    "• $line",
                    color = ScError.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScErrorContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Action buttons ───────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, ScOutlineVariant)
            ) {
                Icon(Icons.Default.Refresh, null, tint = ScOnSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tạo lại", color = ScOnSurfaceVariant)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
            ) {
                Icon(Icons.Default.Add, null, tint = ScOnPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Thêm vào bộ thẻ", color = ScOnPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AiPreviewCard(
    front: String,
    back: String,
    accent: Color,
    accentBg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = ScSurfaceContainerLowest,
        border = BorderStroke(1.dp, accentBg),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar — same as BulkInsertScreen PreviewCard
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "FRONT",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    front,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Divider(color = ScSurfaceContainerHigh)
                Spacer(Modifier.height(8.dp))
                Text(
                    "BACK",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    back,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant,
                    maxLines = 3, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModelHelpDialog(modelPath: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = ScPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Cách tải model AI", style = MaterialTheme.typography.titleLarge,
                        color = ScOnSurface, fontWeight = FontWeight.Bold)
                }
                Text(
                    "1. Tải model Gemma 3 1B (INT4) từ Kaggle hoặc HuggingFace:\n   gemma3-1b-it-int4.task (~500MB)\n\n" +
                    "2. Kết nối điện thoại qua USB, chạy lệnh:\n\n" +
                    "   adb push gemma3-1b-it-int4.task \"$modelPath\"\n\n" +
                    "3. Khởi động lại app và thử lại.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant,
                    fontFamily = FontFamily.Default
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ScSurfaceContainerLow
                ) {
                    Text(
                        modelPath,
                        style = MaterialTheme.typography.labelSmall,
                        color = ScPrimary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)
                ) { Text("Đã hiểu", color = ScOnPrimary) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun aiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ScPrimary,
    unfocusedBorderColor = ScOutlineVariant,
    focusedTextColor = ScOnSurface,
    unfocusedTextColor = ScOnSurface,
    cursorColor = ScPrimary,
    focusedContainerColor = ScSurfaceContainerLowest,
    unfocusedContainerColor = ScSurfaceContainerLow
)
