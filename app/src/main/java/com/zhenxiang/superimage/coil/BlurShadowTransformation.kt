package com.zhenxiang.superimage.coil

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import coil.size.Size
import coil.transform.Transformation
import eightbitlab.com.blurview.BlurAlgorithm
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

class BlurShadowTransformation(
    context: Context,
    val radius: Int,
    val coroutineScope: CoroutineScope
    ): Transformation {

    private var blurJob: Job? = null
    private val blurEffect: BlurAlgorithm = RenderScriptBlur(context)

    private val _blurBitmapFlow = MutableStateFlow<Bitmap?>(null)
    val blurBitmapFlow: StateFlow<Bitmap?> = _blurBitmapFlow

    override val cacheKey: String
        get() = "blur_shadow_$radius"

    @SuppressLint("NewApi")
    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        blurJob?.cancel()
        blurJob = coroutineScope.launch(Dispatchers.Default) {
            renderBlurShadow(input)?.let { _blurBitmapFlow.emit(it) }
            blurJob = null
        }

        return input
    }

    private suspend fun renderBlurShadow(input: Bitmap): Bitmap? {
        try {
            val downscaledRadius = radius / DOWNSCALING_RATIO
            val outputBitmapWidth = input.width + radius * 2
            val outputBitmapHeight = input.height + radius * 2

            // Downscale input bitmap to multiply actual blur radius and performance
            val downscaledInput = Bitmap.createScaledBitmap(
                input,
                (input.width / DOWNSCALING_RATIO).roundToInt(),
                (input.height / DOWNSCALING_RATIO).roundToInt(),
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