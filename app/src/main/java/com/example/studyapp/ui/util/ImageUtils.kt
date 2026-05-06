package com.example.studyapp.ui.util

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext

@Composable
fun loadAssetImage(path: String): Painter {
    val context = LocalContext.current
    val bitmap = remember(path) {
        val inputStream = context.assets.open(path)
        BitmapFactory.decodeStream(inputStream).asImageBitmap()
    }
    return BitmapPainter(bitmap)
}
