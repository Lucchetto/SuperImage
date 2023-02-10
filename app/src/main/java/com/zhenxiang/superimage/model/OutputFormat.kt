package com.zhenxiang.superimage.model

import com.zhenxiang.superimage.common.Identifiable

enum class OutputFormat(
    override val id: Int,
    val formatExtension: String,
    val formatName: String,
): Identifiable<Int> {

    JPEG(0, "jpg", "JPEG"),
    PNG(1, "png", "PNG");

    companion object: Identifiable.EnumCompanion<OutputFormat> {

        override val VALUES = values()

        fun fromFormatName(name: String): OutputFormat? = VALUES.firstOrNull { it.formatName == name }
    }
}
