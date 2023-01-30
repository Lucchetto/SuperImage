package com.zhenxiang.superimage.work

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.zhenxiang.realesrgan.JNIProgressTracker
import com.zhenxiang.realesrgan.RealESRGAN
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.MainActivity
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.utils.IntentUtils
import com.zhenxiang.superimage.utils.compress
import com.zhenxiang.superimage.utils.replaceFileExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
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

    private val inputImageTempFile = params.inputData.getString(INPUT_IMAGE_TEMP_FILE_NAME_PARAM)?.let {
        RealESRGANWorkerManager.getTempFile(context, it)
    }
    private val inputImageName = params.inputData.getString(INPUT_IMAGE_NAME_PARAM) ?: throw IllegalArgumentException(
        "INPUT_IMAGE_NAME_PARAM must not be null"
    )
    private val outputFormat = params.inputData.getString(OUTPUT_IMAGE_FORMAT_PARAM)?.let {
        OutputFormat.fromFormatName(it)
    } ?: throw IllegalArgumentException("Invalid OUTPUT_IMAGE_FORMAT_PARAM")
    private val upscalingModelAssetPath = params.inputData.getString(UPSCALING_MODEL_PATH_PARAM)
    private val upscalingScale = params.inputData.getInt(UPSCALING_SCALE_PARAM, -1)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val startMillis = SystemClock.elapsedRealtime()
        val outputUri = actualWork()
        val executionTime = SystemClock.elapsedRealtime() - startMillis
        // Don't send result notification if app is in foreground
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            notificationManager.notifyAutoId(buildResultNotification(outputUri))
        }
        outputUri?.let {
            Result.success(
                workDataOf(
                    OUTPUT_FILE_URI_PARAM to it.toString(),
                    OUTPUT_EXECUTION_TIME_PARAM to executionTime
                )
            )
        } ?: Result.failure()
    }

    /**
     * @return the uri of the saved output image
     */
    private suspend fun CoroutineScope.actualWork(): Uri? {
        if (upscalingScale < 2) {
            return null
        }
        val inputBitmap = getInputBitmap() ?: return null
        val upscalingModel = getUpscalingModel() ?: return null
        val inputPixels = getPixels(inputBitmap)
        val inputWidth = inputBitmap.width
        val inputHeight = inputBitmap.height
        inputBitmap.recycle()

        setupProgressNotificationBuilder()
        setForeground(createForegroundInfo())

        val progressTracker = JNIProgressTracker()
        val progressUpdateJob = progressTracker.progressFlow.onEach {
            updateProgress(it.coerceAtMost(100f))
        }.launchIn(this)

        val outputPixels = realESRGAN.runUpscaling(
            progressTracker,
            upscalingModel,
            upscalingScale,
            inputPixels,
            inputWidth,
            inputHeight
        )
        progressUpdateJob.cancelAndJoin()

        return if (outputPixels != null) {
            saveOutputImage(outputPixels, inputWidth * upscalingScale, inputHeight * upscalingScale)
        } else {
            null
        }
    }

    private fun setupProgressNotificationBuilder() {
        progressNotificationBuilder.apply {
            setTitleAndTicker(applicationContext.getString(R.string.upscaling_worker_notification_title, inputImageName))
            setSmallIcon(R.drawable.outline_photo_size_select_large_24)
            setOngoing(true)
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

    private fun getInputBitmap(): Bitmap? = inputImageTempFile?.inputStream()?.use {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeStream(it, null, options)
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(buildNotificationChannel())
        }

        return ForegroundInfo(
            PROGRESS_NOTIFICATION_ID,
            buildProgressNotification(JNIProgressTracker.INDETERMINATE)
        )
    }

    private fun buildProgressNotification(progress: Float): Notification = progressNotificationBuilder.apply {
        if (progress == JNIProgressTracker.INDETERMINATE) {
            setProgress(100, 0, true)
        } else {
            setProgress(100, progress.roundToInt(), false)
        }
    }.build()

    private suspend fun updateProgress(progress: Float) {
        setProgress(workDataOf(PROGRESS_VALUE_PARAM to progress))
        notificationManager.notify(PROGRESS_NOTIFICATION_ID, buildProgressNotification(progress))
    }

    private fun buildNotificationChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        ).setName(applicationContext.getString(R.string.upscaling_worker_notification_channel_name)).build()

    private fun buildResultNotification(outputUri: Uri?) = outputUri?.let {
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).apply {
            setTitleAndTicker(applicationContext.getString(R.string.upscaling_worker_success_notification_title, inputImageName))
            setSmallIcon(R.drawable.outline_photo_size_select_large_24)
            setContentText(applicationContext.getString(R.string.upscaling_worker_success_notification_desc))
            setAutoCancel(true)
            setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    IntentUtils.actionViewNewTask(it),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                )
            )
        }.build()
    } ?: run {
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID).apply {
            setTitleAndTicker(applicationContext.getString(R.string.upscaling_worker_error_notification_title, inputImageName))
            setSmallIcon(R.drawable.outline_photo_size_select_large_24)
            val intent = Intent(applicationContext, MainActivity::class.java)
            setAutoCancel(true)
            setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                )
            )
        }.build()
    }

    private fun getOutputStream(): Pair<OutputStream, Uri>? {
        val outputFileName = inputImageName.replaceFileExtension(outputFormat.formatExtension)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, outputFileName)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}${File.separatorChar}$OUTPUT_FOLDER_NAME"
                )
            }

            with(applicationContext.contentResolver) {
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                    openOutputStream(uri)?.let { Pair(it, uri) }
                }
            }
        } else {
            createOutputFilePreQ(outputFileName)?.let { Pair(it.outputStream(), it.toUri()) }
        }
    }

    private fun createOutputFilePreQ(fileName: String): File? = createOutputDirPreQ()?.let {
        File(it, fileName)
    }

    private fun createOutputDirPreQ(): File? {
        val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), OUTPUT_FOLDER_NAME)
        val outputDirCreated = when {
            outputDir.exists() && !outputDir.isDirectory -> {
                /**
                 * Handle the case when file with same name already exists like how MediaStore would.
                 * By doing nothing
                 */
                false
            }
            !outputDir.exists() -> outputDir.mkdir()
            else -> true
        }

        return if (!outputDirCreated) null else outputDir
    }

    private fun saveOutputImage(pixels: IntArray, width: Int, height: Int): Uri? = getOutputStream()?.let {
        it.first.use { outputStream ->
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).run {
                setPixels(pixels, 0, width, 0, 0, width, height)
                val success = compress(outputFormat, 100, outputStream)
                recycle()

                if (success) it.second else null
            }
        }
    }

    data class InputData(
        val originalFileName: String,
        val tempFileName: String,
        val outputFormat: OutputFormat,
        val upscalingModel: UpscalingModel
    ) {
        fun toWorkData(): Data = workDataOf(
            INPUT_IMAGE_TEMP_FILE_NAME_PARAM to tempFileName,
            INPUT_IMAGE_NAME_PARAM to originalFileName,
            OUTPUT_IMAGE_FORMAT_PARAM to outputFormat.formatName,
            UPSCALING_MODEL_PATH_PARAM to upscalingModel.assetPath,
            UPSCALING_SCALE_PARAM to upscalingModel.scale
        )
    }

    sealed interface Progress {

        data class Running(val progress: Float): Progress

        /**
         * @param outputFileUri the [Uri] of the output image
         * @param executionTime execution time of the work in milliseconds
         */
        data class Success(val outputFileUri: Uri, val executionTime: Long): Progress

        object Failed: Progress

        companion object {

            fun fromWorkInfo(workInfo: WorkInfo): Progress? = when (workInfo.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> Running(JNIProgressTracker.INDETERMINATE)
                WorkInfo.State.RUNNING -> Running(
                    workInfo.progress.getFloat(PROGRESS_VALUE_PARAM, JNIProgressTracker.INDETERMINATE)
                )
                WorkInfo.State.SUCCEEDED -> Success(
                    workInfo.outputData.getString(OUTPUT_FILE_URI_PARAM)!!.toUri(),
                    workInfo.outputData.getLong(OUTPUT_EXECUTION_TIME_PARAM, 0)
                )
                WorkInfo.State.FAILED -> Failed
                WorkInfo.State.CANCELLED -> null
            }
        }
    }

    companion object {

        private const val INPUT_IMAGE_TEMP_FILE_NAME_PARAM = "input_image_uri"
        private const val INPUT_IMAGE_NAME_PARAM = "input_image_name"
        private const val PROGRESS_VALUE_PARAM = "progress"
        private const val OUTPUT_IMAGE_FORMAT_PARAM = "output_format"
        private const val OUTPUT_FILE_URI_PARAM = "output_uri"
        private const val OUTPUT_EXECUTION_TIME_PARAM = "exec_time"
        private const val UPSCALING_MODEL_PATH_PARAM = "model_path"
        private const val UPSCALING_SCALE_PARAM = "scale"

        private const val NOTIFICATION_CHANNEL_ID = "real_esrgan"
        private const val PROGRESS_NOTIFICATION_ID = -1

        private const val OUTPUT_FOLDER_NAME = "SuperImage"

        private fun getPixels(bitmap: Bitmap): IntArray {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            return pixels
        }
    }
}

private fun NotificationCompat.Builder.setTitleAndTicker(title: String) = apply {
    setContentTitle(title)
    setTicker(title)
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
private fun NotificationManagerCompat.notifyAutoId(notification: Notification) = notify(
    SystemClock.elapsedRealtime().toInt(),
    notification
)
