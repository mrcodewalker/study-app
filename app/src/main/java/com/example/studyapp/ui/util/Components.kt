package com.example.studyapp.ui.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.studyapp.ui.theme.*

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    iconPath: String,
    confirmText: String,
    confirmColor: Color = ScPrimary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ScSurfaceContainerLowest,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(confirmColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(loadAssetImage(iconPath), null, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, 
                        color = ScOnSurface, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = ScOnSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(99.dp),
                        border = BorderStroke(1.dp, ScOutlineVariant)
                    ) { Text("Hủy", color = ScOnSurfaceVariant) }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) { Text(confirmText, color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

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
