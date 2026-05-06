package com.example.studyapp.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkInsertScreen(
    deckName: String,
    onBack: () -> Unit,
    onParse: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    val parsedCards = remember(input) {
        input.lines().filter { it.contains("~") }.mapNotNull { line ->
            val parts = line.split("~")
            if (parts.size >= 2) {
                val f = parts[0].trim()
                val b = parts.drop(1).joinToString("~").trim()
                if (f.isNotBlank() && b.isNotBlank()) Pair(f, b) else null
            } else null
        }
    }

    val errorLines = remember(input) {
        input.lines().filter { it.isNotBlank() && !it.contains("~") }
    }

    val cardCount = parsedCards.size

    Box(modifier = Modifier.fillMaxSize().background(ScBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = ScOnSurface)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bulk Creation",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ScOnSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Hero text ────────────────────────────────────────────────
                Text(
                    "Transform your notes into study decks in seconds. Paste your content below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScOnSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // ── Instruction card ─────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ScSecondaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, ScSecondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            tint = ScSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "HOW IT WORKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = ScSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Mỗi dòng một thẻ, dùng dấu ~ để phân cách thuật ngữ và định nghĩa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScOnSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ScSurfaceContainerLowest
                            ) {
                                Text(
                                    "Photosynthesis ~ The process by which plants use sunlight to synthesize foods.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ScOnSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Text input ───────────────────────────────────────────────
                Box {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = {
                            Text(
                                "Thuật ngữ ~ Định nghĩa\nMitochondria ~ Nhà máy điện của tế bào\nH2O ~ Công thức hóa học của nước",
                                color = ScOutline,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 320.dp),
                        maxLines = 40,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScPrimaryContainer,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = ScOnSurface,
                            unfocusedTextColor = ScOnSurface,
                            cursorColor = ScPrimary,
                            focusedContainerColor = ScSurfaceContainerLow,
                            unfocusedContainerColor = ScSurfaceContainerLow
                        )
                    )
                    // Badges bottom-right
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (errorLines.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = ScErrorContainer
                            ) {
                                Text(
                                    "${errorLines.size} lỗi",
                                    color = ScError,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (cardCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = ScSecondaryContainer
                            ) {
                                Text(
                                    "$cardCount dòng hợp lệ",
                                    color = ScSecondary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // ── Live preview ─────────────────────────────────────────────
                AnimatedVisibility(
                    visible = parsedCards.isNotEmpty(),
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Column {
                        Spacer(Modifier.height(20.dp))
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
                            Spacer(Modifier.weight(1f))
                            if (input.isNotBlank()) {
                                TextButton(
                                    onClick = { input = "" },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        "Clear all",
                                        color = ScPrimary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        // 2-column grid preview — all cards, no limit
                        val accentColors = listOf(ScPrimary, ScSecondary, ScTertiary)
                        val accentBgColors = listOf(ScPrimaryContainer, ScSecondaryContainer, ScTertiaryContainer)

                        // Use a non-lazy column since we're already inside a scroll
                        parsedCards.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEachIndexed { colIdx, (front, back) ->
                                    val globalIdx = parsedCards.indexOf(Pair(front, back))
                                    val accent = accentColors[globalIdx % accentColors.size]
                                    val accentBg = accentBgColors[globalIdx % accentBgColors.size]
                                    PreviewCard(
                                        front = front,
                                        back = back,
                                        accent = accent,
                                        accentBg = accentBg,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill empty slot if odd number
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        // Error lines
                        AnimatedVisibility(visible = errorLines.isNotEmpty()) {
                            Column {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning, null,
                                        tint = ScError,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "DÒNG LỖI (thiếu dấu ~)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ScError,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                errorLines.forEach { line ->
                                    Text(
                                        "• $line",
                                        color = ScError.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ScErrorContainer.copy(alpha = 0.3f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp)) // space for FAB
            }
        }

        // ── Floating action button ────────────────────────────────────────────
        AnimatedVisibility(
            visible = cardCount > 0,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ScBackground.copy(alpha = 0f), ScBackground)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Button(
                    onClick = { onParse(input) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScSecondary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, null,
                        tint = ScOnSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Import Cards",
                        color = ScOnPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Importing will add these to your \"$deckName\" deck.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScOutline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    front: String,
    back: String,
    accent: androidx.compose.ui.graphics.Color,
    accentBg: androidx.compose.ui.graphics.Color,
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
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        accent,
                        RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )
            Column(modifier = Modifier.padding(16.dp)) {
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = ScOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Divider(color = ScSurfaceContainerHigh)
                Spacer(Modifier.height(10.dp))
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScOnSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
