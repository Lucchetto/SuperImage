package com.zhenxiang.realesrgan

import java.nio.MappedByteBuffer

class RealESRGAN {

    external fun runUpscaling(
        modelData: MappedByteBuffer,
        scale: Int,
        inputImage: IntArray,
        inputImageWidth: Int,
        inputImageHeight: Int,
    ): IntArray?

    companion object {
        // Used to load the 'realesrgan' library on application startup.
        init {
            System.loadLibrary("realesrgan")
        }
    }
}