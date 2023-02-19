package com.zhenxiang.superimage.utils

fun String.replaceFileExtension(extension: String): String {
    val extStartIndex = lastIndexOf(FileUtils.FILE_EXTENSION_CHAR)
    return if (extStartIndex < 0) {
        "$this.$extension"
    } else {
        replaceRange(extStartIndex + 1, length, extension)
    }
}

fun String.addFileNameSuffix(suffix: String) = buildString {
    val split = this@addFileNameSuffix.split(FileUtils.FILE_EXTENSION_CHAR)
    var suffixAppended = false

    split.forEachIndexed { index, part ->
        if (part.isNotEmpty()) {
            append(part)
            if (!suffixAppended) {
                suffixAppended = true
                append(suffix)
            }
        }
        if (index < split.size - 1) {
            append(FileUtils.FILE_EXTENSION_CHAR)
        }
    }
}
