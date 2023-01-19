package com.zhenxiang.superimage.model

import android.graphics.Bitmap
import android.net.Uri

data class InputImage(
    val fileName: String,
    val fileUri: Uri,
    val preview: Bitmap
)
