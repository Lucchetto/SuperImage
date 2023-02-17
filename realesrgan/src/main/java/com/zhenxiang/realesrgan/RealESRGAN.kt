package com.zhenxiang.realesrgan

import kotlinx.coroutines.CoroutineScope
import java.nio.ByteBuffer

class RealESRGAN {

    external suspend fun runUpscaling(
        progressTracker: JNIProgressTracker,
        coroutineScope: CoroutineScope,
        modelData: ByteArray,
        scale: Int,
        inputImage: IntArray,
        inputImageWidth: Int,
        inputImageHeight: Int,
    ): ByteBuffer?

    /**
     * Free a [ByteBuffer] allocated with
     */
    external fun freeDirectBuffer(buffer: ByteBuffer)

    companion object {
        // Used to load the 'realesrgan' library on application startup.
        init {
            System.loadLibrary("MNN_VK")
            System.loadLibrary("MNN_CL")
            System.loadLibrary("realesrgan")
        }
    }
}