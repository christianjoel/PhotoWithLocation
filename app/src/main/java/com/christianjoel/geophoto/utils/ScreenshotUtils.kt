package com.christianjoel.geophoto.utils

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.core.graphics.scale
import androidx.core.view.drawToBitmap
import java.io.File
import java.io.FileOutputStream

fun captureComposeScreenshot(
    context: Context,
    view: View
): File {
    val originalBitmap: Bitmap = view.drawToBitmap()

    // Bitmap downsampling / scaling to optimize memory usage and performance
    val maxWidth = 1920
    val maxHeight = 1080
    val bitmap = if (originalBitmap.width > maxWidth || originalBitmap.height > maxHeight) {
        val ratio = minOf(
            maxWidth.toFloat() / originalBitmap.width,
            maxHeight.toFloat() / originalBitmap.height
        )
        val width = (originalBitmap.width * ratio).toInt()
        val height = (originalBitmap.height * ratio).toInt()
        originalBitmap.scale(width, height, filter = true).also {
            if (it != originalBitmap) {
                originalBitmap.recycle()
            }
        }
    } else {
        originalBitmap
    }

    val file = File(
        context.cacheDir,
        "GeoPhoto_${System.currentTimeMillis()}.jpg"
    )

    FileOutputStream(file).use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
    }

    if (!bitmap.isRecycled) {
        bitmap.recycle()
    }

    return file
}
