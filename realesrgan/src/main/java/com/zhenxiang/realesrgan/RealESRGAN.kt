package com.zhenxiang.realesrgan

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope

class RealESRGAN {

    external fun runUpscaling(
        progressTracker: JNIProgressTracker,
        coroutineScope: CoroutineScope,
        modelData: ByteArray,
        scale: Int,
        inputBitmap: Bitmap,
        outputBitmap: Bitmap
    ): Int

    companion object {
        // Used to load the 'realesrgan' library on application startup.
        init {
            System.loadLibrary("MNN_VK")
            System.loadLibrary("MNN_CL")
            System.loadLibrary("realesrgan")
        }
    }
}