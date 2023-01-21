package com.zhenxiang.superimage.model

enum class OutputFormat(val formatExtension: String, val formatName: String) {

    JPEG("jpg", "JPEG"),
    PNG("png", "PNG");

    companion object {

        val VALUES = values()

        fun fromFormatName(name: String): OutputFormat? = VALUES.firstOrNull { it.formatName == name }
    }
}
