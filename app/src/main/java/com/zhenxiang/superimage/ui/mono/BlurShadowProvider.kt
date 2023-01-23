package com.zhenxiang.superimage.ui.mono

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.zhenxiang.superimage.utils.BitmapUtils
import eightbitlab.com.blurview.BlurAlgorithm
import eightbitlab.com.blurview.RenderScriptBlur
import timber.log.Timber
import kotlin.math.roundToInt

class BlurShadowProvider(context: Context, val radius: Int) {

    private val blurEffect: BlurAlgorithm = RenderScriptBlur(context)

    fun getBlurShadow(input: Bitmap): Bitmap? = BitmapUtils.copyToSoftware(input).let {
        try {
            val downscaledRadius = radius / DOWNSCALING_RATIO
            val outputBitmapWidth = it.width + radius * 2
            val outputBitmapHeight = it.height + radius * 2

            // Downscale input bitmap to multiply actual blur radius and performance
            val downscaledInput = Bitmap.createScaledBitmap(
                it,
                (it.width / DOWNSCALING_RATIO).roundToInt(),
                (it.height / DOWNSCALING_RATIO).roundToInt(),
                true
            )

            // Draw the downscaled input into another bitmap for blurring
            val blurBitmap = Bitmap.createBitmap(
                (outputBitmapWidth / DOWNSCALING_RATIO).roundToInt(),
                (outputBitmapHeight / DOWNSCALING_RATIO).roundToInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(blurBitmap)
            canvas.drawBitmap(downscaledInput, downscaledRadius, downscaledRadius, null)

            // Blur, yeah
            blurEffect.blur(blurBitmap, downscaledRadius)

            // Upscale the blurred bitmap again
            val upscaledBlurBitmap = Bitmap.createScaledBitmap(
                blurBitmap,
                outputBitmapWidth,
                outputBitmapHeight,
                true
            )
            downscaledInput.recycle()
            blurBitmap.recycle()

            return upscaledBlurBitmap
        } catch (e: Exception) {
            Timber.e("Failed to render blur shadow")
            Timber.e(e)

            return null
        }
    }

    companion object {

        private const val DOWNSCALING_RATIO = 10f
    }
}