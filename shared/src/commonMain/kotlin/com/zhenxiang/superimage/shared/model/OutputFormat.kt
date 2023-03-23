package com.zhenxiang.superimage.shared.model

import com.zhenxiang.superimage.common.Identifiable

enum class OutputFormat(
    override val id: Int,
    val formatExtension: String,
    val formatName: String,
    val mimeType: String
): Identifiable<Int> {

    JPEG(0, "jpg", "JPEG", "image/jpeg"),
    PNG(1, "png", "PNG", "image/png");

    companion object: Identifiable.EnumCompanion<OutputFormat> {

        override val VALUES = values()

        fun fromFormatName(name: String): OutputFormat? = VALUES.firstOrNull { it.formatName == name }
    }
}
