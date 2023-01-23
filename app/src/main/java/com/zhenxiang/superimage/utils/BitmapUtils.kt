package com.zhenxiang.superimage.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.zhenxiang.superimage.model.OutputFormat
import timber.log.Timber
import java.io.OutputStream

object BitmapUtils {

    fun loadImageFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    /**
     * Copy hardware bitmap to software.
     * It will simply return the input bitmap if it's not a hardware bitmap
     */
    fun copyToSoftware(
        hwBitmap: Bitmap,
        recycleHwBitmap: Boolean = false
    ): Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hwBitmap.config == Bitmap.Config.HARDWARE) {
        val bitmapCopy = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
        if (recycleHwBitmap) {
            hwBitmap.recycle()
        }
        bitmapCopy
    } else {
        hwBitmap
    }
}

fun Bitmap.compress(outputFormat: OutputFormat, quality: Int, outputStream: OutputStream): Boolean {
    val bitmapCompressFormat = when (outputFormat) {
        OutputFormat.PNG -> Bitmap.CompressFormat.PNG
        OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
    }

    return compress(bitmapCompressFormat, quality, outputStream)
}
