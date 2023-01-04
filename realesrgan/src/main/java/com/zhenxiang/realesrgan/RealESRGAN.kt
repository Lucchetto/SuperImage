package com.zhenxiang.realesrgan

import java.nio.ByteBuffer
import java.nio.MappedByteBuffer

class RealESRGAN {

    external fun runUpscaling(
        modelData: MappedByteBuffer,
        scale: Int,
        inputImage: IntArray
    ): ByteBuffer?

    companion object {
        // Used to load the 'realesrgan' library on application startup.
        init {
            System.loadLibrary("realesrgan")
        }
    }
}