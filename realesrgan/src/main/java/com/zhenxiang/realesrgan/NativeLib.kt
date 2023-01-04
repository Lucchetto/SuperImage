package com.zhenxiang.realesrgan

class NativeLib {

    /**
     * A native method that is implemented by the 'realesrgan' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'realesrgan' library on application startup.
        init {
            System.loadLibrary("realesrgan")
        }
    }
}