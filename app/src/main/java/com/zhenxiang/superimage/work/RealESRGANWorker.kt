package com.zhenxiang.superimage.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.zhenxiang.realesrgan.InterpreterError
import com.zhenxiang.realesrgan.JNIProgressTracker
import com.zhenxiang.realesrgan.RealESRGAN
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.shared.model.DataResult
import com.zhenxiang.superimage.shared.model.OutputFormat
import com.zhenxiang.superimage.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

class RealESRGANWorker(
    context: Context,
    params: WorkerParameters
): CoroutineWorker(context, params) {

    private val realESRGAN = RealESRGAN()
    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    private val progressNotificationBuilder = NotificationCompat.Builder(applicationContext, PROGRESS_NOTIFICATION_CHANNEL_ID)

    private val inputImageTempFile = params.inputData.getString(INPUT_IMAGE_TEMP_FILE_NAME_PARAM)?.let {
        RealESRGANWorkerManager.getTempFile(context, it)
    }
    private val inputImageName = params.inputData.getString(INPUT_IMAGE_NAME_PARAM) ?: throw IllegalArgumentException(
        "INPUT_IMAGE_NAME_PARAM must not be null"
    )
    private val outputFormat = params.inputData.getString(OUTPUT_IMAGE_FORMAT_PARAM)?.let {
        OutputFormat.fromFormatName(it)
    } ?: throw IllegalArgumentException("Invalid OUTPUT_IMAGE_FORMAT_PARAM")
    private val upscalingModel = UpscalingModel.fromId(
        params.inputData.getInt(UPSCALING_MODEL_ID_PARAM, Int.MIN_VALUE)
    ) ?: throw IllegalArgumentException("Invalid UPSCALING_MODEL_ID_PARAM")
    
    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val startMillis = SystemClock.elapsedRealtime()
        val resultData = actualWork()
        val executionTime = SystemClock.elapsedRealtime() - startMillis

        // Don't send result notification if app is in foreground or permission denied
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && notificationPermission) {
            notificationManager.notifyAutoId(buildResultNotification(resultData))
        }

        when (resultData) {
            is DataResult.Success -> Result.success(
                workDataOf(
                    OUTPUT_FILE_URI_PARAM to resultData.data.toString(),
                    OUTPUT_EXECUTION_TIME_PARAM to executionTime
                )
            )
            is DataResult.Error -> Result.failure(
                workDataOf(OUTPUT_ERROR_PARAM to resultData.error)
            )
            null -> Result.failure()
        }
    }

    /**
     * @return the uri of the saved output image
     */
    private suspend fun CoroutineScope.actualWork(): DataResult<Uri, Int>? {
        if (upscalingModel.scale < 2) {
            return null
        }
        val inputBitmap = getInputBitmap() ?: return null
        val upscalingModelData = getUpscalingModelData() ?: return null

        setupProgressNotificationBuilder()
        setForeground(createForegroundInfo())

        val progressTracker = JNIProgressTracker()
        val progressUpdateJob = progressTracker.progressFlow.onEach {
            updateProgress(it)
        }.launchIn(this)

        val outputBitmap = Bitmap.createBitmap(
            inputBitmap.width * upscalingModel.scale,
            inputBitmap.height * upscalingModel.scale,
            Bitmap.Config.ARGB_8888
        )

        val result = realESRGAN.runUpscaling(
            progressTracker,
            this,
            upscalingModelData,
            upscalingModel.scale,
            inputBitmap,
            outputBitmap
        )
        progressUpdateJob.cancelAndJoin()

        return if (result == 0) {
            saveOutputImage(outputBitmap)?.let { DataResult.Success(it) }
        } else {
            DataResult.Error(result)
        }
    }

    private fun setupProgressNotificationBuilder() = with(applicationContext) {
        progressNotificationBuilder.apply {
            setTitleAndTicker(getString(R.string.upscaling_worker_notification_title, inputImageName))
            setContentText(getString(R.string.upscaling_worker_notification_desc))
            setSmallIcon(R.drawable.outline_photo_size_select_large_24)
            setOngoing(true)
            setContentIntent(
                IntentUtils.mainActivityPendingIntent(
                    this@with,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            addAction(R.drawable.baseline_close_24, getString(R.string.cancel), cancelWorkIntent)
        }
    }

    private fun getUpscalingModelData(): ByteArray? = upscalingModel.assetPath.let { path ->
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
        BitmapFactory.decodeStream(it, null, options)?.transformWithExif(ExifInterface(inputImageTempFile))
    }

    private fun createForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat.Builder(
                PROGRESS_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName(applicationContext.getString(R.string.upscaling_worker_progress_notification_channel_name))
                .build()
                .let { notificationManager.createNotificationChannel(it) }
        } else {
            progressNotificationBuilder.priority = NotificationCompat.PRIORITY_LOW
        }

        return ForegroundInfo(
            PROGRESS_NOTIFICATION_ID,
            buildProgressNotification(JNIProgressTracker.INDETERMINATE_PROGRESS)
        )
    }

    private fun buildProgressNotification(progress: Float): Notification = progressNotificationBuilder.apply {
        if (progress == JNIProgressTracker.INDETERMINATE_PROGRESS) {
            setProgress(100, 0, true)
        } else {
            setProgress(100, progress.roundToInt(), false)
        }
    }.build()

    @SuppressLint("MissingPermission")
    private suspend fun updateProgress(progress: JNIProgressTracker.Progress) {
        setProgress(
            workDataOf(
                PROGRESS_VALUE_PARAM to progress.value,
                ESTIMATED_MILLIS_LEFT_PARAM to progress.estimatedMillisLeft
            )
        )
        if (notificationPermission) {
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, buildProgressNotification(progress.value))
        }
    }

    private fun buildResultNotification(dataResult: DataResult<Uri, Int>?) = with(applicationContext) {
        val notificationBuilder = NotificationCompat.Builder(this, RESULT_NOTIFICATION_CHANNEL_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat.Builder(
                RESULT_NOTIFICATION_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName(applicationContext.getString(R.string.upscaling_worker_result_notification_channel_name))
                .build()
                .let { notificationManager.createNotificationChannel(it) }
        } else {
            notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
        }

        if (dataResult is DataResult.Success) {
            notificationBuilder.apply {
                setTitleAndTicker(getString(R.string.upscaling_worker_success_notification_title, inputImageName))
                setSmallIcon(R.drawable.outline_photo_size_select_large_24)
                setContentText(getString(R.string.upscaling_worker_success_notification_desc))
                setAutoCancel(true)
                setContentIntent(
                    IntentUtils.notificationPendingIntent(
                        this@with,
                        IntentUtils.actionViewNewTask(dataResult.data)
                    )
                )
            }.build()
        } else {
            notificationBuilder.apply {
                if (InterpreterError.fromNativeErrorEnum((dataResult as? DataResult.Error)?.error) == InterpreterError.CREATE_SESSION) {
                    setTitleAndTicker(getString(R.string.upscaling_worker_error_no_backend_title))
                    setContentText(getString(R.string.upscaling_worker_error_no_backend_desc))
                } else {
                    setTitleAndTicker(getString(R.string.upscaling_worker_error_notification_title, inputImageName))
                }
                setSmallIcon(R.drawable.outline_photo_size_select_large_24)
                setAutoCancel(true)
                setContentIntent(IntentUtils.mainActivityPendingIntent(this@with))
            }.build()
        }
    }

    private fun createOutputFilePreQ(fileName: String): File? = createOutputDirPreQ()?.let {
        FileUtils.newFileAutoSuffix(it, fileName)
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

    private fun saveOutputImage(bitmap: Bitmap): Uri? {
        val outputFileName = inputImageName
            .addFileNameSuffix(upscalingModel.fileNameSuffix)
            .replaceFileExtension(outputFormat.formatExtension)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, outputFileName)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}${File.separatorChar}$OUTPUT_FOLDER_NAME"
                )
            }

            return with(applicationContext.contentResolver) {
                val uri = insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { openOutputStream(it) }?.let {
                    val success = bitmap.compress(outputFormat, 100, it)
                    bitmap.recycle()
                    if (success) uri else null
                }
            }
        } else {
            val outputFile = createOutputFilePreQ(outputFileName)
            return outputFile?.outputStream()?.use {
                val success = bitmap.compress(outputFormat, 100, it)
                bitmap.recycle()
                if (success) {
                    val uri = FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.fileprovider",
                        outputFile
                    )
                    applicationContext.sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
                    )
                    uri
                } else {
                    null
                }
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
            UPSCALING_MODEL_ID_PARAM to upscalingModel.id
        )
    }

    sealed interface Progress {

        /**
         * @param progress progress in range 0 to 100 or [INDETERMINATE_PROGRESS]
         * @param estimatedMillisLeft estimated time left in milliseconds
         */
        data class Running(val progress: Float, val estimatedMillisLeft: Long): Progress

        /**
         * @param outputFileUri the [Uri] of the output image
         * @param executionTime execution time of the work in milliseconds
         */
        data class Success(val outputFileUri: Uri, val executionTime: Long): Progress

        data class Failed(val error: InterpreterError): Progress

        companion object {

            fun fromWorkInfo(workInfo: WorkInfo): Progress? = when (workInfo.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> Running(
                    JNIProgressTracker.INDETERMINATE_PROGRESS,
                    JNIProgressTracker.INDETERMINATE_TIME
                )
                WorkInfo.State.RUNNING -> workInfo.progress.let {
                    Running(
                        it.getFloat(PROGRESS_VALUE_PARAM, JNIProgressTracker.INDETERMINATE_PROGRESS),
                        it.getLong(ESTIMATED_MILLIS_LEFT_PARAM, JNIProgressTracker.INDETERMINATE_TIME),
                    )
                }
                WorkInfo.State.SUCCEEDED -> Success(
                    workInfo.outputData.getString(OUTPUT_FILE_URI_PARAM)!!.toUri(),
                    workInfo.outputData.getLong(OUTPUT_EXECUTION_TIME_PARAM, 0)
                )
                WorkInfo.State.FAILED -> Failed(
                    InterpreterError.fromNativeErrorEnum(
                        workInfo.outputData.getInt(OUTPUT_ERROR_PARAM, -1)
                    )
                )
                WorkInfo.State.CANCELLED -> null
            }
        }
    }

    companion object {

        private const val INPUT_IMAGE_TEMP_FILE_NAME_PARAM = "input_image_uri"
        private const val INPUT_IMAGE_NAME_PARAM = "input_image_name"
        private const val PROGRESS_VALUE_PARAM = "progress"
        private const val ESTIMATED_MILLIS_LEFT_PARAM = "est_time"
        private const val OUTPUT_IMAGE_FORMAT_PARAM = "output_format"
        private const val OUTPUT_FILE_URI_PARAM = "output_uri"
        private const val OUTPUT_EXECUTION_TIME_PARAM = "exec_time"
        private const val OUTPUT_ERROR_PARAM = "output_error"
        private const val UPSCALING_MODEL_ID_PARAM = "model_id"

        private const val PROGRESS_NOTIFICATION_CHANNEL_ID = "progress_ch"
        private const val RESULT_NOTIFICATION_CHANNEL_ID = "result_ch"
        private const val PROGRESS_NOTIFICATION_ID = -1

        private const val OUTPUT_FOLDER_NAME = "SuperImage"
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

private val ListenableWorker.cancelWorkIntent: PendingIntent
    get() = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

/**
 * If EXIF contains transformations to apply return new a bitmap with the transformations and recycle original
 * Otherwise return original bitmap
 */
private fun Bitmap.transformWithExif(exifInterface: ExifInterface): Bitmap {
    val isFlipped = exifInterface.isFlipped
    val rotationDegrees = exifInterface.rotationDegrees
    if (!isFlipped && rotationDegrees == 0) {
        return this
    }
    val centerX = width / 2f
    val centerY = height / 2f
    val matrix = Matrix().apply {
        if (isFlipped) {
            postScale(-1f, 1f, centerX, centerY)
        }
        if (rotationDegrees != 0) {
            postRotate(rotationDegrees.toFloat(), centerX, centerY)
        }
    }
    val transformed = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    // Recycle original bitmap
    recycle()
    return transformed
}
