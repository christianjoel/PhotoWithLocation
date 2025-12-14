package com.christianjoel.geophoto.utils

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.core.view.drawToBitmap
import java.io.File
import java.io.FileOutputStream

fun captureComposeScreenshot(
    context: Context,
    view: View
): File {
    val bitmap: Bitmap = view.drawToBitmap()

    val file = File(
        context.cacheDir,
        "GeoPhoto_${System.currentTimeMillis()}.jpg"
    )

    FileOutputStream(file).use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
    }

    return file
}
