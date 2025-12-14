package com.christianjoel.geophoto.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

fun saveImageToGallery(context: Context, imageFile: File) {
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/GeoPhoto"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    if (uri == null) {
        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        return
    }

    resolver.openOutputStream(uri).use { outputStream: OutputStream? ->
        FileInputStream(imageFile).use { input ->
            input.copyTo(outputStream!!)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    }

    Toast.makeText(context, "Saved to Gallery 📸", Toast.LENGTH_SHORT).show()
}
