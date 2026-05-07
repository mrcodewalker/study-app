package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.studyapp.ai.AiApiClient
import com.example.studyapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
//  Data
// ─────────────────────────────────────────────────────────────────────────────

data class AiChatMsg(
    val id: String = UUID.randomUUID().toString(),
    val role: String,          // "user" | "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

private val QUICK_PROMPTS = listOf(
    "Giải thích OOP đơn giản",
    "Mẹo học thuộc nhanh",
    "Lập kế hoạch ôn thi",
    "Big O notation là gì?",
    "Phương pháp Pomodoro",
    "Sự khác biệt TCP vs UDP",
)

// GIF anime từ assets/gif/ — thêm file vào app/src/main/assets/gif/ để tự động nhận
private const val BOT_GIF_DIR = "gif"

// ─────────────────────────────────────────────────────────────────────────────
//  Helper: GIF avatar với fallback emoji
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BotGifAvatar(assetPath: String, modifier: Modifier = Modifier, fallbackSize: TextUnit = 13.sp) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/$assetPath")
            .memoryCacheKey("$assetPath-${assetPath.hashCode()}")
            .diskCacheKey("$assetPath-${assetPath.hashCode()}")
            .decoderFactory(
                if (android.os.Build.VERSION.SDK_INT >= 28)
                    ImageDecoderDecoder.Factory()
                else
                    GifDecoder.Factory()
            )
            .crossfade(false)
            .build(),
        contentDescription = "AI Bot",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        error = { Text("🤖", fontSize = fallbackSize) }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Floating AI Bubble  (đặt trong Box toàn màn hình)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBubble(
    onShowSheetChange: ((Boolean) -> Unit)? = null,
    forceShow: Boolean = false,
    isEnabled: Boolean = true,
    gifShuffleKey: Int = 0
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val context = LocalContext.current

    val screenWidthPx  = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val bubbleSizePx   = with(density) { 70.dp.toPx() }

    // Random 1 GIF từ assets/gif/ — random ngay khi khởi tạo, re-pick khi gifShuffleKey thay đổi
    var botGifAsset by remember { mutableStateOf("gif/anime-bunny.gif") }
    
    // Random ngay lần đầu vào app
    LaunchedEffect(Unit) {
        botGifAsset = try {
            val files = context.assets.list(BOT_GIF_DIR)
                ?.filter { it.endsWith(".gif") }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf("anime-bunny.gif")
            "$BOT_GIF_DIR/${files.random()}"
        } catch (_: Exception) {
            "gif/anime-bunny.gif"
        }
    }
    
    // Re-pick khi shuffle
    LaunchedEffect(gifShuffleKey) {
        if (gifShuffleKey > 0) { // Chỉ chạy khi shuffle, không chạy lần đầu
            botGifAsset = try {
                val files = context.assets.list(BOT_GIF_DIR)
                    ?.filter { it.endsWith(".gif") }
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf("anime-bunny.gif")
                "$BOT_GIF_DIR/${files.random()}"
            } catch (_: Exception) {
                "gif/anime-bunny.gif"
            }
        }
    }

    // Bubble position — start bottom-right
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - bubbleSizePx - 24f) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx - bubbleSizePx - 160f) }

    // Drag state — distinguish tap vs drag
    var isDragging by remember { mutableStateOf(false) }
    var dragDistance by remember { mutableFloatStateOf(0f) }

    // Chat state
    var showSheet by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf(listOf<AiChatMsg>()) }

    // Mở từ bên ngoài (ví dụ button trên HomeScreen)
    LaunchedEffect(forceShow) {
        if (forceShow) {
            showSheet = true
            onShowSheetChange?.invoke(false) // reset để có thể trigger lại lần sau
        }
    }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var sessionId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var serverReady by remember { mutableStateOf(false) }
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }

    // Pulse animation on bubble
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    // Check server on first open
    LaunchedEffect(showSheet) {
        if (showSheet) {
            val s = AiApiClient.checkStatus()
            serverOnline = s.isOnline
            serverReady = s.isReady
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || isLoading || !serverReady) return
        val userMsg = AiChatMsg(role = "user", content = text.trim())
        messages = messages + userMsg
        inputText = ""
        isLoading = true
        scope.launch {
            try {
                val result = AiApiClient.chat(
                    message = userMsg.content,
                    sessionId = sessionId,
                    language = "Vietnamese"
                )
                messages = messages + AiChatMsg(role = "assistant", content = result.reply)
            } catch (e: Exception) {
                messages = messages + AiChatMsg(
                    role = "assistant",
                    content = "Lỗi kết nối: ${e.message}",
                    isError = true
                )
            } finally {
                isLoading = false
            }
        }
    }

    // ── Bottom Sheet ──────────────────────────────────────────────────────────
    if (showSheet) {        AiChatBottomSheet(
            messages = messages,
            inputText = inputText,
            isLoading = isLoading,
            serverReady = serverReady,
            serverOnline = serverOnline,
            botGifAsset = botGifAsset,
            onInputChange = { inputText = it },
            onSend = { sendMessage(inputText) },
            onQuickPrompt = { sendMessage(it) },
            onClear = {
                scope.launch {
                    try { AiApiClient.deleteChatSession(sessionId) } catch (_: Exception) {}
                    sessionId = UUID.randomUUID().toString()
                    messages = emptyList()
                }
            },
            onDismiss = { showSheet = false }
        )
    }

    // ── Floating Bubble ───────────────────────────────────────────────────────
    if (!isEnabled) return

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .size(80.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = false
                        dragDistance = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistance += kotlin.math.sqrt(
                            dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y
                        )
                        isDragging = dragDistance > 8f
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - bubbleSizePx)
                        offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - bubbleSizePx - 80f)
                    },
                    onDragEnd = {
                        if (!isDragging) showSheet = true
                        isDragging = false
                        dragDistance = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDistance = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showSheet = true })
            }
    ) {
        // Glow ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
                .background(Color.Transparent)
        )
        // GIF avatar — không clip, không viền, hiển thị nguyên bản
        key(botGifAsset) {
            BotGifAvatar(
                assetPath = botGifAsset,
                modifier = Modifier.fillMaxSize(),
                fallbackSize = 32.sp
            )
        }
        // Unread dot
        if (messages.isNotEmpty() && !showSheet) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFFef4444))
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom Sheet Chat UI
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiChatBottomSheet(
    messages: List<AiChatMsg>,
    inputText: String,
    isLoading: Boolean,
    serverReady: Boolean,
    serverOnline: Boolean?,
    botGifAsset: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Xóa cuộc trò chuyện?") },
            text = { Text("Toàn bộ lịch sử chat sẽ bị xóa.") },
            confirmButton = {
                TextButton(onClick = { showClearDialog = false; onClear() }) {
                    Text("Xóa", color = ScError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Hủy") }
            },
            containerColor = ScSurfaceContainerLowest,
            shape = RoundedCornerShape(20.dp)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ScBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Sheet header ─────────────────────────────────────────────────
            Surface(
                color = ScSurfaceContainerLowest,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(ScOutlineVariant)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val context = LocalContext.current
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(ScPrimary, Color(0xFF5e5b7a)))),
                            contentAlignment = Alignment.Center
                        ) {
                            BotGifAvatar(assetPath = botGifAsset, modifier = Modifier.fillMaxSize(), fallbackSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "KMAStudy AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ScOnSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                serverOnline == null -> ScOutline
                                                serverReady -> Color(0xFF22c55e)
                                                else -> ScWarning
                                            }
                                        )
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    when {
                                        serverOnline == null -> "Đang kết nối..."
                                        serverReady -> "Sẵn sàng trả lời"
                                        serverOnline == false -> "Server offline"
                                        else -> "Model chưa load"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant
                                )
                            }
                        }
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    Icons.Default.DeleteOutline, null,
                                    tint = ScOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.KeyboardArrowDown, null,
                                tint = ScOnSurfaceVariant
                            )
                        }
                    }
                    Divider(color = ScOutlineVariant.copy(0.5f))
                }
            }

            // ── Messages ─────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    ChatEmptyView(
                        serverReady = serverReady,
                        serverOnline = serverOnline,
                        onQuickPrompt = onQuickPrompt
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            AiChatBubbleItem(msg, botGifAsset)
                        }
                        if (isLoading) {
                            item(key = "typing") { AiTypingIndicator(botGifAsset) }
                        }
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            Surface(
                color = ScSurfaceContainerLowest,
                border = BorderStroke(1.dp, ScOutlineVariant.copy(0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (serverReady) "Hỏi bất cứ điều gì..." else "Server chưa sẵn sàng",
                                color = ScOutline,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        enabled = serverReady && !isLoading,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScPrimary,
                            unfocusedBorderColor = ScOutlineVariant,
                            focusedContainerColor = ScSurfaceContainerLow,
                            unfocusedContainerColor = ScSurfaceContainerLow,
                            focusedTextColor = ScOnSurface,
                            unfocusedTextColor = ScOnSurface,
                            cursorColor = ScPrimary
                        )
                    )
                    val canSend = inputText.isNotBlank() && serverReady && !isLoading
                    FilledIconButton(
                        onClick = onSend,
                        enabled = canSend,
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ScPrimary,
                            disabledContainerColor = ScPrimaryContainer
                        )
                    ) {
                        Icon(
                            if (isLoading) Icons.Default.HourglassEmpty else Icons.Default.Send,
                            null,
                            tint = if (canSend) ScOnPrimary else ScOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Empty / welcome view
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatEmptyView(
    serverReady: Boolean,
    serverOnline: Boolean?,
    onQuickPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Xin chào! Tôi có thể giúp gì cho bạn?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ScOnSurface,
            textAlign = TextAlign.Center
        )
        Text(
            "Hỏi về bất kỳ chủ đề học tập nào",
            style = MaterialTheme.typography.bodySmall,
            color = ScOnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (serverOnline == false) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ScErrorContainer.copy(0.3f),
                border = BorderStroke(1.dp, ScErrorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiOff, null, tint = ScError, modifier = Modifier.size(16.dp))
                    Text(
                        "Server offline. Chạy: python ai_server/server.py",
                        style = MaterialTheme.typography.bodySmall,
                        color = ScOnSurface
                    )
                }
            }
        }

        if (serverReady) {
            Spacer(Modifier.height(20.dp))
            Text(
                "GỢI Ý NHANH",
                style = MaterialTheme.typography.labelSmall,
                color = ScOnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(10.dp))
            QUICK_PROMPTS.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { prompt ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuickPrompt(prompt) },
                            shape = RoundedCornerShape(12.dp),
                            color = ScSurfaceContainerLowest,
                            border = BorderStroke(1.dp, ScOutlineVariant),
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                prompt,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Chat bubble item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AiChatBubbleItem(msg: AiChatMsg, botGifAsset: String) {
    val isUser = msg.role == "user"
    val timeStr = remember(msg.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
    }
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(ScPrimary, Color(0xFF5e5b7a)))),
                contentAlignment = Alignment.Center
            ) {
                BotGifAvatar(assetPath = botGifAsset, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = when {
                    msg.isError -> ScErrorContainer.copy(0.4f)
                    isUser -> ScPrimary
                    else -> ScSurfaceContainerLowest
                },
                border = if (!isUser && !msg.isError) BorderStroke(1.dp, ScOutlineVariant) else null,
                shadowElevation = if (isUser) 2.dp else 1.dp
            ) {
                Text(
                    text = msg.content,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        msg.isError -> ScError
                        isUser -> ScOnPrimary
                        else -> ScOnSurface
                    },
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = ScOutline, fontSize = 10.sp)
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ScPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Typing indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AiTypingIndicator(botGifAsset: String) {
    val inf = rememberInfiniteTransition(label = "typing")
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ScPrimary, Color(0xFF5e5b7a)))),
            contentAlignment = Alignment.Center
        ) {
            BotGifAvatar(assetPath = botGifAsset, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = ScSurfaceContainerLowest,
            border = BorderStroke(1.dp, ScOutlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    val alpha by inf.animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(500, delayMillis = i * 150, easing = EaseInOutSine),
                            RepeatMode.Reverse
                        ),
                        label = "dot$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ScPrimary.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}
