package com.zhenxiang.superimage.shared.model

import java.io.File

data class InputImage(
    val fileName: String,
    val tempFile: File,
    val width: Int,
    val height: Int
)
