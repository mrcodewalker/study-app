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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.ai.AiApiClient
import com.example.studyapp.ai.AiGenerateState
import com.example.studyapp.ui.theme.*
import com.example.studyapp.ui.viewmodel.FlashcardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LANGUAGE_OPTIONS = listOf("Vietnamese", "English", "Japanese", "Korean", "Chinese")

private val CARD_TYPE_OPTIONS = listOf(
    Triple("term_def",  "Thuật ngữ",        "Khái niệm ~ Định nghĩa"),
    Triple("en_vi",     "Anh → Việt",       "English word ~ Nghĩa tiếng Việt"),
    Triple("vi_en",     "Việt → Anh",       "Từ tiếng Việt ~ English translation"),
    Triple("qa",        "Hỏi & Đáp",        "Câu hỏi ~ Câu trả lời"),
    Triple("antonym",   "Trái/Đồng nghĩa",  "Từ ~ Trái nghĩa (Đồng nghĩa)"),
    Triple("code",      "Lập trình",        "Lệnh/Hàm ~ Giải thích + Cú pháp"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerateScreen(
    deckId: Long,
    deckName: String,
    viewModel: FlashcardViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val aiState by viewModel.aiState.collectAsState()
    val scope = rememberCoroutineScope()

    var topic by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("10") }
    var language by remember { mutableStateOf("Vietnamese") }
    var cardType by remember { mutableStateOf("term_def") }
    var showLangDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }

    // Server URL — persists across recompositions, user can change for real device
    var serverUrl by remember { mutableStateOf(AiApiClient.baseUrl) }

    // Server status
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }
    var serverModel by remember { mutableStateOf("") }
    var serverReady by remember { mutableStateOf(false) }
    var serverLanIp by remember { mutableStateOf("") }

    // Poll server status — check immediately on enter, then every 5s
    LaunchedEffect(Unit) {
        while (true) {
            val s = AiApiClient.checkStatus()
            serverOnline = s.isOnline
            serverReady = s.isReady
            serverModel = s.model
            if (s.lanIp.isNotBlank()) serverLanIp = s.lanIp
            // Poll faster when offline so UI updates quickly when server starts
            delay(if (s.isOnline) 8000L else 2000L)
        }
    }

    // Re-check immediately when URL changes
    LaunchedEffect(serverUrl) {
        val s = AiApiClient.checkStatus()
        serverOnline = s.isOnline
        serverReady = s.isReady
        serverModel = s.model
        if (s.lanIp.isNotBlank()) serverLanIp = s.lanIp
    }

    // Auto-navigate on success
    LaunchedEffect(aiState) {
        if (aiState is AiGenerateState.Success) {
            delay(1800)
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
                    .background(Brush.verticalGradient(listOf(ScSurfaceContainerLowest, ScBackground)))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.resetAiState(); onBack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = ScOnSurface)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Generate", style = MaterialTheme.typography.titleLarge,
                        color = ScOnSurface, fontWeight = FontWeight.Bold)
                    Text(deckName, style = MaterialTheme.typography.bodySmall,
                        color = ScOnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(ScPrimaryContainer, ScTertiaryContainer))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = ScPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Server status banner ─────────────────────────────────
                ServerStatusBanner(online = serverOnline, ready = serverReady, model = serverModel)

                Spacer(Modifier.height(20.dp))

                // ── Input form ───────────────────────────────────────────
                AnimatedVisibility(
                    visible = aiState is AiGenerateState.Idle || aiState is AiGenerateState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // ── Server connection mode ────────────────────────────
                        ServerUrlPicker(
                            currentUrl = serverUrl,
                            lanIp = serverLanIp,
                            onUrlChange = { url ->
                                serverUrl = url
                                AiApiClient.baseUrl = url
                            }
                        )

                        // ── Topic ────────────────────────────────────────────
                        Column {
                            Text("CHỦ ĐỀ", style = MaterialTheme.typography.labelSmall,
                                color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = topic, onValueChange = { topic = it },
                                placeholder = { Text("VD: Cấu trúc dữ liệu, Lịch sử Việt Nam...", color = ScOutline) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = RoundedCornerShape(14.dp), colors = aiFieldColors()
                            )
                        }

                        // Count + Language + Type
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SỐ LƯỢNG", style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = countText,
                                    onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) countText = it },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = aiFieldColors()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NGÔN NGỮ", style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(Modifier.height(6.dp))
                                ExposedDropdownMenuBox(
                                    expanded = showLangDropdown,
                                    onExpandedChange = { showLangDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = language, onValueChange = {}, readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showLangDropdown) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        singleLine = true, shape = RoundedCornerShape(14.dp),
                                        colors = aiFieldColors()
                                    )
                                    ExposedDropdownMenu(expanded = showLangDropdown,
                                        onDismissRequest = { showLangDropdown = false }) {
                                        LANGUAGE_OPTIONS.forEach { lang ->
                                            DropdownMenuItem(text = { Text(lang) },
                                                onClick = { language = lang; showLangDropdown = false })
                                        }
                                    }
                                }
                            }
                        }

                        // Card type selector
                        val selectedType = CARD_TYPE_OPTIONS.first { it.first == cardType }
                        Column {
                            Text("LOẠI THẺ", style = MaterialTheme.typography.labelSmall,
                                color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                            ExposedDropdownMenuBox(
                                expanded = showTypeDropdown,
                                onExpandedChange = { showTypeDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = "${selectedType.second}  •  ${selectedType.third}",
                                    onValueChange = {}, readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showTypeDropdown) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true, shape = RoundedCornerShape(14.dp),
                                    colors = aiFieldColors()
                                )
                                ExposedDropdownMenu(expanded = showTypeDropdown,
                                    onDismissRequest = { showTypeDropdown = false }) {
                                    CARD_TYPE_OPTIONS.forEach { (key, label, hint) ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(label, fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium)
                                                    Text(hint, style = MaterialTheme.typography.labelSmall,
                                                        color = ScOnSurfaceVariant)
                                                }
                                            },
                                            onClick = { cardType = key; showTypeDropdown = false },
                                            leadingIcon = {
                                                if (cardType == key)
                                                    Icon(Icons.Default.Check, null,
                                                        tint = ScPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Error banner
                        if (aiState is AiGenerateState.Error) {
                            val err = aiState as AiGenerateState.Error
                            Surface(shape = RoundedCornerShape(12.dp),
                                color = ScErrorContainer.copy(0.4f),
                                border = BorderStroke(1.dp, ScErrorContainer)) {
                                Row(modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = ScError, modifier = Modifier.size(18.dp))
                                    Text(err.message, style = MaterialTheme.typography.bodySmall, color = ScOnSurface)
                                }
                            }
                        }

                        val count = countText.toIntOrNull()?.coerceIn(1, 50) ?: 10
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.generateFlashcards(
                                        context = context,
                                        topic = topic.trim(), count = count,
                                        language = language, cardType = cardType
                                    )
                                }
                            },
                            enabled = topic.isNotBlank() && serverReady,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(99.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScPrimary,
                                disabledContainerColor = ScPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = ScOnPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Tạo $count flashcard với AI", color = ScOnPrimary,
                                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // ── Generating state ─────────────────────────────────────
                AnimatedVisibility(
                    visible = aiState is AiGenerateState.CheckingServer || aiState is AiGenerateState.Generating,
                    enter = fadeIn(tween(300)), exit = fadeOut(tween(200))
                ) {
                    GeneratingView(isChecking = aiState is AiGenerateState.CheckingServer)
                }

                // ── Preview state ────────────────────────────────────────
                AnimatedVisibility(
                    visible = aiState is AiGenerateState.Preview,
                    enter = fadeIn(tween(300)) + expandVertically(tween(350)),
                    exit = fadeOut(tween(200))
                ) {
                    val preview = aiState as? AiGenerateState.Preview
                    if (preview != null) {
                        AiPreviewSection(
                            cards = preview.cards,
                            errorLines = preview.errorLines,
                            durationMs = preview.durationMs,
                            onConfirm = { viewModel.confirmAiInsert(deckId) },
                            onDiscard = { viewModel.resetAiState() }
                        )
                    }
                }

                // ── Success state ────────────────────────────────────────
                val successState = aiState as? AiGenerateState.Success
                AnimatedVisibility(visible = successState != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                            Text("Đã thêm ${successState?.insertedCount ?: 0} thẻ!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ScOnSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ServerStatusBanner(online: Boolean?, ready: Boolean, model: String) {
    data class BannerData(
        val bgColor: Color,
        val borderColor: Color,
        val iconTint: Color,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val title: String,
        val subtitle: String
    )

    val d = when {
        online == null -> BannerData(ScSurfaceContainerLow, ScOutlineVariant, ScOnSurfaceVariant,
            Icons.Default.HourglassEmpty, "Đang kiểm tra server...", "Kết nối tới localhost:8000")
        !online -> BannerData(ScErrorContainer.copy(0.3f), ScErrorContainer, ScError,
            Icons.Default.WifiOff, "Server offline", "Chạy: cd ai_server && python server.py")
        !ready -> BannerData(Color(0xFFFFF3E0), Color(0xFFFFCC02), Color(0xFFF57C00),
            Icons.Default.Warning, "Server online — chưa load model", "Chạy: ollama pull gemma2:2b")
        else -> BannerData(Color(0xFF4CAF50).copy(0.1f), Color(0xFF4CAF50).copy(0.4f), Color(0xFF4CAF50),
            Icons.Default.CheckCircle, "Server sẵn sàng", model.ifBlank { "Model đã load" })
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = d.bgColor,
        border = BorderStroke(1.dp, d.borderColor)
    ) {
        Row(modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(d.icon, null, tint = d.iconTint, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(d.title, style = MaterialTheme.typography.labelMedium,
                    color = d.iconTint, fontWeight = FontWeight.SemiBold)
                Text(d.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant, maxLines = 2)
            }
            if (online == null) {
                val inf = rememberInfiniteTransition(label = "dot")
                val a by inf.animateFloat(0.3f, 1f,
                    infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "a")
                Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(ScOnSurfaceVariant.copy(alpha = a)))
            }
        }
    }
}

@Composable
private fun GeneratingView(isChecking: Boolean) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val alpha by inf.animateFloat(0.4f, 1f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse), label = "a")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.radialGradient(listOf(ScPrimaryContainer, ScTertiaryContainer.copy(0.5f)))),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AutoAwesome, null,
                tint = ScPrimary.copy(alpha = alpha), modifier = Modifier.size(40.dp))
        }
        Text(if (isChecking) "Đang kết nối server..." else "Đang tạo flashcard...",
            style = MaterialTheme.typography.titleMedium,
            color = ScOnSurface, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.6f).clip(RoundedCornerShape(99.dp)),
            color = ScPrimary, trackColor = ScPrimaryContainer
        )
    }
}

@Composable
private fun AiPreviewSection(
    cards: List<Pair<String, String>>,
    errorLines: List<String>,
    durationMs: Int,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    val accentColors = listOf(ScPrimary, ScSecondary, ScTertiary)
    val accentBgColors = listOf(ScPrimaryContainer, ScSecondaryContainer, ScTertiaryContainer)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LIVE PREVIEW", style = MaterialTheme.typography.labelSmall,
                color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(99.dp), color = ScPrimaryContainer) {
                Text("${cards.size} thẻ", color = ScPrimary,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            if (durationMs > 0) {
                Spacer(Modifier.width(8.dp))
                Text("${durationMs / 1000}s", style = MaterialTheme.typography.labelSmall,
                    color = ScOnSurfaceVariant)
            }
        }

        // 2-column grid
        cards.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (front, back) ->
                    val idx = cards.indexOf(Pair(front, back))
                    AiCardPreview(front, back,
                        accentColors[idx % 3], accentBgColors[idx % 3],
                        Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (errorLines.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = ScError, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Bỏ qua ${errorLines.size} dòng lỗi",
                    style = MaterialTheme.typography.labelSmall, color = ScError)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, ScOutlineVariant)) {
                Icon(Icons.Default.Refresh, null, tint = ScOnSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tạo lại", color = ScOnSurfaceVariant)
            }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ScPrimary)) {
                Icon(Icons.Default.Add, null, tint = ScOnPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Thêm vào bộ thẻ", color = ScOnPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AiCardPreview(front: String, back: String, accent: Color, accentBg: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp),
        color = ScSurfaceContainerLowest, border = BorderStroke(1.dp, accentBg), shadowElevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight()
                .background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
            Column(modifier = Modifier.padding(12.dp)) {
                Text("FRONT", style = MaterialTheme.typography.labelSmall,
                    color = accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(front, style = MaterialTheme.typography.bodyMedium,
                    color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Divider(color = ScSurfaceContainerHigh)
                Spacer(Modifier.height(8.dp))
                Text("BACK", style = MaterialTheme.typography.labelSmall,
                    color = ScSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(back, style = MaterialTheme.typography.bodySmall,
                    color = ScOnSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun aiFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ScPrimary, unfocusedBorderColor = ScOutlineVariant,
    focusedTextColor = ScOnSurface, unfocusedTextColor = ScOnSurface,
    cursorColor = ScPrimary,
    focusedContainerColor = ScSurfaceContainerLowest,
    unfocusedContainerColor = ScSurfaceContainerLow
)

// ── ServerUrlPicker ───────────────────────────────────────────────────────────

private const val URL_EMULATOR = "http://10.0.2.2:8000"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerUrlPicker(
    currentUrl: String,
    lanIp: String,
    onUrlChange: (String) -> Unit
) {
    val urlRealDevice = if (lanIp.isNotBlank()) "http://$lanIp:8000" else ""

    // Determine which chip is selected
    val selectedMode = when {
        currentUrl == URL_EMULATOR -> 0
        urlRealDevice.isNotBlank() && currentUrl == urlRealDevice -> 1
        else -> 2  // custom
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "KẾT NỐI SERVER",
            style = MaterialTheme.typography.labelSmall,
            color = ScOnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // ── Mode chips ────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Chip: Máy ảo
            ModeChip(
                label = "Máy ảo",
                icon = Icons.Default.PhoneAndroid,
                selected = selectedMode == 0,
                onClick = { onUrlChange(URL_EMULATOR) },
                modifier = Modifier.weight(1f)
            )

            // Chip: Máy thật
            ModeChip(
                label = if (urlRealDevice.isNotBlank()) "Máy thật" else "Máy thật (chờ server)",
                icon = Icons.Default.Wifi,
                selected = selectedMode == 1,
                enabled = urlRealDevice.isNotBlank(),
                onClick = { if (urlRealDevice.isNotBlank()) onUrlChange(urlRealDevice) },
                modifier = Modifier.weight(1f)
            )
        }

        // ── URL text field (always editable) ─────────────────────────
        OutlinedTextField(
            value = currentUrl,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = aiFieldColors(),
            placeholder = { Text("http://...", color = ScOutline) },
            label = { Text("URL", color = ScOnSurfaceVariant) },
            trailingIcon = {
                // Reset button — only show when URL is non-default
                if (currentUrl != URL_EMULATOR) {
                    IconButton(onClick = { onUrlChange(URL_EMULATOR) }) {
                        Icon(
                            Icons.Default.RestartAlt, null,
                            tint = ScOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        )

        // Hint text
        val hint = when {
            selectedMode == 0 -> "Emulator → máy tính qua 10.0.2.2"
            selectedMode == 1 && urlRealDevice.isNotBlank() ->
                "Điện thoại thật → IP LAN tự động: $lanIp"
            urlRealDevice.isBlank() ->
                "Chạy server trước để tự động lấy IP LAN"
            else -> "URL tuỳ chỉnh"
        }
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = ScOutline
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val bg     = if (selected) ScPrimaryContainer else ScSurfaceContainerLow
    val border = if (selected) ScPrimary else ScOutlineVariant
    val tint   = if (selected) ScPrimary else if (enabled) ScOnSurfaceVariant else ScOutlineVariant

    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
