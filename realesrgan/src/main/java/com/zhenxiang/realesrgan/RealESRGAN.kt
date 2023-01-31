package com.zhenxiang.realesrgan

class RealESRGAN {

    external fun runUpscaling(
        progressTracker: JNIProgressTracker,
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