package com.zhenxiang.superimage.work

import android.app.Notification
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.zhenxiang.realesrgan.JNIProgressTracker
import com.zhenxiang.realesrgan.RealESRGAN
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.utils.BitmapUtils
import com.zhenxiang.superimage.utils.compress
import com.zhenxiang.superimage.utils.replaceFileExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import kotlin.math.roundToInt

class RealESRGANWorker(
    context: Context,
    params: WorkerParameters
): CoroutineWorker(context, params) {

    private val realESRGAN = RealESRGAN()
    private val notificationManager = NotificationManagerCompat.from(context)
    private val progressNotificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

    private val inputImageUri = params.inputData.getString(INPUT_IMAGE_URI_PARAM)?.let { Uri.parse(it) }
    private val inputImageName = params.inputData.getString(INPUT_IMAGE_NAME_PARAM) ?: throw IllegalArgumentException(
        "INPUT_IMAGE_NAME_PARAM must not be null"
    )
    private val outputFormat = params.inputData.getString(OUTPUT_IMAGE_FORMAT_PARAM)?.let {
        OutputFormat.fromFormatName(it)
    } ?: throw IllegalArgumentException("Invalid OUTPUT_IMAGE_FORMAT_PARAM")
    private val upscalingModelAssetPath = params.inputData.getString(UPSCALING_MODEL_PATH_PARAM)
    private val upscalingScale = params.inputData.getInt(UPSCALING_SCALE_PARAM, -1)
    
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            actualWork().also {
                when (it) {
                    is Result.Success -> updateNotification(JNIProgressTracker.Progress.Success)
                    is Result.Failure -> updateNotification(JNIProgressTracker.Progress.Error)
                }
            }
        }
    }
    
    private suspend fun CoroutineScope.actualWork(): Result {
        if (upscalingScale < 2) {
            return Result.failure()
        }
        val inputBitmap = getInputBitmap() ?: return Result.failure()
        val upscalingModel = getUpscalingModel() ?: return Result.failure()
        val inputPixels = getPixels(inputBitmap)
        val inputWidth = inputBitmap.width
        val inputHeight = inputBitmap.height
        inputBitmap.recycle()

        setForeground(createForegroundInfo())

        val progressTracker = JNIProgressTracker()
        progressTracker.progressFlow.onEach {
            updateNotification(it)
        }.launchIn(this)

        val outputPixels = realESRGAN.runUpscaling(
            progressTracker,
            upscalingModel,
            upscalingScale,
            inputPixels,
            inputWidth,
            inputHeight
        ) ?: return Result.failure()

        return if (saveOutputImage(outputPixels, inputWidth * upscalingScale, inputHeight * upscalingScale)) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    private fun getUpscalingModel(): ByteArray? = upscalingModelAssetPath?.let { path ->
        try {
            applicationContext.assets.open(path).use {
                it.readBytes()
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun getInputBitmap(): Bitmap? = inputImageUri?.let { uri ->
        BitmapUtils.loadImageFromUri(applicationContext.contentResolver, uri)?.let { bitmap ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                val bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                bitmap.recycle()
                bitmapCopy
            } else {
                bitmap
            }
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(buildNotificationChannel())
        }

        return ForegroundInfo(
            NOTIFICATION_ID,
            buildNotification(applicationContext, progressNotificationBuilder, inputImageName)
        )
    }

    private fun updateNotification(progress: JNIProgressTracker.Progress) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(applicationContext, progressNotificationBuilder, inputImageName, progress)
        )
    }

    private fun buildNotificationChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        ).setName(applicationContext.getString(R.string.upscaling_worker_notification_channel_name)).build()

    private fun getOutputStream(): OutputStream? {
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                inputImageName.replaceFileExtension(outputFormat.formatExtension)
            )
        }

        return applicationContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let {
            applicationContext.contentResolver.openOutputStream(it)
        }
    }

    private fun saveOutputImage(pixels: IntArray, width: Int, height: Int): Boolean = getOutputStream()?.use {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).run {
            setPixels(pixels, 0, width, 0, 0, width, height)
            val success = compress(outputFormat, 100, it)
            recycle()

            success
        }
    } ?: false

    companion object {

        const val INPUT_IMAGE_URI_PARAM = "input_image_uri"
        const val INPUT_IMAGE_NAME_PARAM = "input_image_name"
        const val OUTPUT_IMAGE_FORMAT_PARAM = "output_format"
        const val UPSCALING_MODEL_PATH_PARAM = "model_path"
        const val UPSCALING_SCALE_PARAM = "scale"

        private const val NOTIFICATION_CHANNEL_ID = "real_esrgan"
        private const val NOTIFICATION_ID = 69

        private const val OUTPUT_FOLDER_NAME = "RealESRGAN"

        private fun buildNotification(
            context: Context,
            notificationBuilder: NotificationCompat.Builder,
            fileName: String,
            progress: JNIProgressTracker.Progress = JNIProgressTracker.Progress.Indeterminate
        ): Notification {

            val title = when (progress) {
                JNIProgressTracker.Progress.Error -> R.string.upscaling_worker_error_notification_title
                JNIProgressTracker.Progress.Indeterminate -> R.string.upscaling_worker_notification_title
                is JNIProgressTracker.Progress.Loading ->R.string.upscaling_worker_notification_title
                JNIProgressTracker.Progress.Success -> R.string.upscaling_worker_success_notification_title
            }.let { context.getString(it, fileName) }

            return notificationBuilder.apply {
                setContentTitle(title)
                setTicker(title)
                setSmallIcon(R.drawable.outline_photo_size_select_large_24)

                when (progress) {
                    JNIProgressTracker.Progress.Indeterminate -> {
                        setOngoing(true)
                        setProgress(100, 0, true)
                    }
                    is JNIProgressTracker.Progress.Loading -> {
                        setOngoing(true)
                        setProgress(
                            100,
                            progress.percentage.roundToInt().coerceAtMost(100),
                            false
                        )
                    }
                    else -> {
                        setOngoing(false)
                        setProgress(0, 0, false)
                    }
                }
            }.build()
        }

        private fun getPixels(bitmap: Bitmap): IntArray {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            return pixels
        }
    }
}