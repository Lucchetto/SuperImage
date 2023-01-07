package com.zhenxiang.tfrealesrgan.home

import android.app.Application
import android.content.ContentResolver
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhenxiang.realesrgan.RealESRGAN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.UUID
import kotlin.system.measureNanoTime

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val realESRGAN by lazy {
        RealESRGAN()
    }

    fun load(imageUri: Uri) {
        val bitmap = loadImageFromUri(getApplication<Application>().contentResolver, imageUri)?.let {
            val softwareBitmap = it.copy(Bitmap.Config.ARGB_8888, false)
            it.recycle()
            softwareBitmap
        } ?: return

        viewModelScope.launch(Dispatchers.Default) {
            measureNanoTime {
                inference(bitmap)
            }.also {
                Log.d(null, "Execution took ${it / 1000 / 1000}ms")
            }
        }
    }

    private suspend fun inference(bitmap: Bitmap) {

        val application = getApplication<Application>()
        val modelBuffer = loadModelFile(application.assets, "realesrgan-x4plus.tflite")
        val outputImage = realESRGAN.runUpscaling(
            modelBuffer,
            4,
            getPixels(bitmap),
            bitmap.width,
            bitmap.height
        )

        outputImage?.let {
            getOutputImageFile("${UUID.randomUUID()}.png")?.outputStream()?.use { os ->
                // TODO: remove hardcoded output size
                val outputBitmap = Bitmap.createBitmap(bitmap.width * 4, bitmap.height * 4, Bitmap.Config.ARGB_8888).apply {
                    setPixels(it, 0, width, 0, 0, width, height)
                }
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                outputBitmap.recycle()
            }
        }
        bitmap.recycle()
    }

    private fun getPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        return pixels
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getOutputImageFile(fileName: String): File? {
        val file = File(getOutputFolder(), fileName)
        if (file.exists()) {
            if (!file.delete()) {
                return null
            }
        }
        return if (file.createNewFile()) {
            file
        } else {
            null
        }
    }

    companion object {

        private const val OUTPUT_SUBDIRECTORY = "TFRealESRGAN"

        private fun getOutputFolder(): File {
            val picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val outputFolder = File(picturesFolder, OUTPUT_SUBDIRECTORY)
            if (!outputFolder.isDirectory) {
                // File with same name of output folder, nuke it
                if (outputFolder.exists()) {
                    outputFolder.delete()
                }
                outputFolder.mkdir()
            }
            return outputFolder
        }

        private fun loadImageFromUri(contentResolver: ContentResolver, uri: Uri): Bitmap? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.wtf(null, e)
            null
        }
    }
}