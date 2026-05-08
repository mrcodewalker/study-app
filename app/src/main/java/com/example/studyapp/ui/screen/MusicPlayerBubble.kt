package com.example.studyapp.ui.screen

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.studyapp.music.MusicService
import com.example.studyapp.ui.theme.*

@Composable
private fun MusicGifAvatar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/music-girl.gif")
            .memoryCacheKey("music-girl-gif")
            .diskCacheKey("music-girl-gif")
            .decoderFactory(
                if (android.os.Build.VERSION.SDK_INT >= 28)
                    ImageDecoderDecoder.Factory()
                else
                    GifDecoder.Factory()
            )
            .crossfade(false)
            .build(),
        contentDescription = "Music",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        error = {
            Box(modifier, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerBubble(
    isEnabled: Boolean = true,
    isVisible: Boolean = true
) {
    // Luôn giữ state trong composition kể cả khi bị ẩn tạm thời (isVisible=false)
    // Chỉ return hoàn toàn nếu bị disable từ settings (isEnabled=false)
    if (!isEnabled) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val config  = LocalConfiguration.current

    val screenWidthPx  = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    
    val bubbleSizeTotal = with(density) { 70.dp.toPx() }
    
    // Sử dụng rememberSaveable để giữ vị trí khi xoay màn hình hoặc đóng app
    var offsetX by rememberSaveable { mutableFloatStateOf(with(density) { 32.dp.toPx() }) }
    var offsetY by rememberSaveable { mutableFloatStateOf(screenHeightPx - bubbleSizeTotal - with(density) { 100.dp.toPx() }) }
    
    var isDragging  by remember { mutableStateOf(false) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var showSheet   by remember { mutableStateOf(false) }

    // ── Observe service state ─────────────────────────────────────────────────
    val isPlaying    by MusicService.isPlaying.collectAsState()
    val isAutoNext   by MusicService.isAutoNext.collectAsState()
    val currentTrack by MusicService.currentTrack.collectAsState()

    val playlist = remember {
        try {
            context.assets.list("music")
                ?.filter { it.endsWith(".mp3") }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun svcIntent(action: String, track: String? = null) =
        Intent(context, MusicService::class.java).apply {
            this.action = action
            track?.let { putExtra(MusicService.EXTRA_TRACK, it) }
        }

    fun play(track: String)  = context.startForegroundService(svcIntent(MusicService.ACTION_PLAY, track))
    fun pause()              = context.startService(svcIntent(MusicService.ACTION_PAUSE))
    fun resume()             = context.startService(svcIntent(MusicService.ACTION_RESUME))
    fun next()               = context.startService(svcIntent(MusicService.ACTION_NEXT))
    fun prev()               = context.startService(svcIntent(MusicService.ACTION_PREV))
    fun toggleAutoNext()     = context.startService(svcIntent(MusicService.ACTION_TOGGLE_AUTO_NEXT))
    fun togglePlayPause()    = if (isPlaying) pause() else if (currentTrack != null) resume()
                                else playlist.firstOrNull()?.let { play(it) }

    // ── Bottom Sheet ──────────────────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ScBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null,
            windowInsets = WindowInsets(0),
            modifier = Modifier.fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (đã có trong file cũ)
                Surface(
                    color = ScSurfaceContainerLowest,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.width(40.dp).height(4.dp)
                                .clip(RoundedCornerShape(99.dp)).background(ScOutlineVariant))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(ScSecondary, ScSecondaryContainer))),
                                contentAlignment = Alignment.Center
                            ) {
                                MusicGifAvatar(modifier = Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Trình phát nhạc",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold, color = ScOnSurface)
                                Text(
                                    if (isPlaying) "Đang phát: ${currentTrack?.removeSuffix(".mp3") ?: ""}"
                                    else if (currentTrack != null) "Tạm dừng"
                                    else "Chưa phát",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ScOnSurfaceVariant
                                )
                            }
                            IconButton(onClick = { toggleAutoNext() }) {
                                Icon(
                                    if (isAutoNext) Icons.Default.RepeatOne else Icons.Default.RepeatOneOn,
                                    contentDescription = if (isAutoNext) "Tắt tự động phát" else "Bật tự động phát",
                                    tint = if (isAutoNext) ScPrimary else ScOnSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showSheet = false }) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = ScOnSurfaceVariant)
                            }
                        }
                        Divider(color = ScOutlineVariant.copy(0.5f))
                    }
                }

                // Now playing card
                if (currentTrack != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = ScPrimaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, ScPrimary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                                    .background(Brush.radialGradient(listOf(ScPrimary, ScSecondary))),
                                contentAlignment = Alignment.Center
                            ) {
                                val noteScale by rememberInfiniteTransition(label = "note").animateFloat(
                                    initialValue = 0.8f, targetValue = 1.1f,
                                    animationSpec = infiniteRepeatable(tween(600, easing = EaseInOutSine), RepeatMode.Reverse),
                                    label = "note"
                                )
                                Icon(Icons.Default.MusicNote, null, tint = Color.White,
                                    modifier = Modifier.size((24 * if (isPlaying) noteScale else 1f).dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentTrack!!.removeSuffix(".mp3"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ScOnSurface, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Surface(shape = RoundedCornerShape(99.dp),
                                    color = if (isPlaying) ScPrimary.copy(alpha = 0.15f) else ScSurfaceContainerLow) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                                            .background(if (isPlaying) ScPrimary else ScOnSurfaceVariant))
                                        Text(if (isPlaying) "Đang phát" else "Tạm dừng",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isPlaying) ScPrimary else ScOnSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { prev() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.SkipPrevious, null, tint = ScOnSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                FilledIconButton(
                                    onClick = { togglePlayPause() },
                                    modifier = Modifier.size(44.dp), shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ScPrimary)
                                ) {
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        null, tint = ScOnPrimary, modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = { next() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.SkipNext, null, tint = ScOnSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // Playlist
                Text("DANH SÁCH PHÁT",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScOnSurfaceVariant, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                if (playlist.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicOff, null, tint = ScOnSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có file nhạc", style = MaterialTheme.typography.bodyMedium, color = ScOnSurfaceVariant)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        playlist.forEachIndexed { idx, track ->
                            val isActive = track == currentTrack
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (isActive) togglePlayPause() else play(track)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isActive) ScPrimaryContainer.copy(alpha = 0.5f) else ScSurfaceContainerLowest,
                                border = if (isActive) BorderStroke(1.dp, ScPrimary.copy(alpha = 0.3f)) else null
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) ScPrimary.copy(alpha = 0.15f) else ScSurfaceContainerLow),
                                        contentAlignment = Alignment.Center) {
                                        if (isActive && isPlaying) {
                                            Icon(Icons.Default.VolumeUp, null, tint = ScPrimary, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("${idx + 1}", style = MaterialTheme.typography.labelMedium,
                                                color = if (isActive) ScPrimary else ScOnSurfaceVariant, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(track.removeSuffix(".mp3"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isActive) ScPrimary else ScOnSurface,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // ── Floating GIF Bubble (Vị trí được bao bọc bên ngoài AnimatedVisibility để giữ state) ──
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .size(70.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible && !showSheet,
            enter = fadeIn(tween(400)) + 
                    scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) +
                    slideInVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) { it / 2 },
            exit = fadeOut(tween(300)) + scaleOut(spring(stiffness = Spring.StiffnessMedium)) +
                   slideOutVertically(tween(300)) { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = false; dragDistance = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDistance += kotlin.math.sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                                isDragging = dragDistance > 10f
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - bubbleSizeTotal)
                                offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeightPx - bubbleSizeTotal - 80f)
                            },
                            onDragEnd = {
                                if (!isDragging) showSheet = true
                                isDragging = false; dragDistance = 0f
                            },
                            onDragCancel = { isDragging = false; dragDistance = 0f }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showSheet = true })
                    }
            ) {
                MusicGifAvatar(modifier = Modifier.fillMaxSize())

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(ScPrimary)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

