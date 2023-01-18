package com.zhenxiang.realesrgan

enum class UpscalingModel(val labelRes: Int, val assetPath: String, val scale: Int) {
    X2_PLUS(R.string.x2_plus_model_label, "realesrgan-x2plus.mnn", 2),
    X4_PLUS(R.string.x4_plus_model_label, "realesrgan-x4plus.mnn", 4);

    companion object {
        val VALUES = values()
    }
}
