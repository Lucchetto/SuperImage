package com.zhenxiang.superimage.utils

fun String.replaceFileExtension(extension: String): String {
    val extStartIndex = lastIndexOf(".")
    return if (extStartIndex < 0) {
        "$this.$extension"
    } else {
        replaceRange(extStartIndex + 1, length, extension)
    }
}
