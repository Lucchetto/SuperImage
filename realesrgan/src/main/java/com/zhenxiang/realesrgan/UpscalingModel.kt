package com.zhenxiang.realesrgan

import com.zhenxiang.superimage.common.Identifiable

enum class UpscalingModel(
    override val id: Int,
    val labelRes: Int,
    val assetPath: String,
    val scale: Int,
): Identifiable<Int> {
    X2_PLUS(0, R.string.x2_plus_model_label, "realesrgan-x2plus.mnn", 2),
    X4_PLUS(1, R.string.x4_plus_model_label, "realesrgan-x4plus.mnn", 4),
    X4_PLUS_ANIME(2, R.string.x4_plus_anime_model_label, "realesrgan-x4plus-anime.mnn", 4);

    companion object: Identifiable.EnumCompanion<UpscalingModel> {
        override val VALUES = values()
    }
}
