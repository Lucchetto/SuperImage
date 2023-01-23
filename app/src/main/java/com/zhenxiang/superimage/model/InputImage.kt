package com.zhenxiang.superimage.model

import android.net.Uri

data class InputImage(
    val fileName: String,
    val fileUri: Uri,
    val width: Int,
    val height: Int
)
