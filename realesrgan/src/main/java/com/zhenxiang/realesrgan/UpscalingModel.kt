package com.zhenxiang.realesrgan

enum class UpscalingModel(val labelRes: Int, val assetPath: String, val scale: Int) {
    X4_PLUS(R.string.x4_plus_model_label, "realesrgan-x4plus.mnn", 4);

    companion object {
        val VALUES = values()
    }
}
