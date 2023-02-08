package com.zhenxiang.realesrgan

import kotlinx.coroutines.CoroutineScope

class RealESRGAN {

    external suspend fun runUpscaling(
        progressTracker: JNIProgressTracker,
        coroutineScope: CoroutineScope,
        modelData: ByteArray,
        scale: Int,
        inputImage: IntArray,
        inputImageWidth: Int,
        inputImageHeight: Int,
    ): IntArray?

    companion object {
        // Used to load the 'realesrgan' library on application startup.
        init {
            System.loadLibrary("MNN_VK")
            System.loadLibrary("MNN_CL")
            System.loadLibrary("realesrgan")
        }
    }
}