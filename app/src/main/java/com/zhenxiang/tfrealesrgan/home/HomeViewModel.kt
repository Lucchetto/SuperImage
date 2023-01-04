package com.zhenxiang.tfrealesrgan.home

import android.app.Application
import android.content.ContentResolver
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Color
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
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val realESRGAN by lazy {
        RealESRGAN()
    }

    fun load(imageUri: Uri) {
        val croppedBitmap = loadImageFromUri(getApplication<Application>().contentResolver, imageUri)?.let {
            val cropped = Bitmap.createBitmap(it, 0, 0, 64, 64)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && cropped.config == Bitmap.Config.HARDWARE) {
                // We can't get pixels from hardware bitmap, so convert it to software first
                val software = cropped.copy(Bitmap.Config.ARGB_8888, false)
                cropped.recycle()
                software
            } else {
                cropped
            }
        } ?: return

        viewModelScope.launch(Dispatchers.Default) {
            measureNanoTime {
                inference(croppedBitmap)
            }.also {
                Log.d(null, "Execution took ${it / 1000 / 1000}ms")
            }
        }
    }

    private suspend fun inference(bitmap: Bitmap) {

        val outputImage = realESRGAN.runUpscaling(
            loadModelFile(getApplication<Application>().assets, "realesrgan-x4plus.tflite"),
            4,
            getPixels(bitmap)
        )

        outputImage?.let {
            getOutputImageFile("${UUID.randomUUID()}.png")?.outputStream()?.use { os ->
                // TODO: remove hardcoded output size
                val outputBitmap = Bitmap.createBitmap(bitmap.width * 4, bitmap.height * 4, Bitmap.Config.ARGB_8888).apply {
                    setPixels(it, 0, width, 0, 0, width, height)
                }
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
            }
        }
    }

    private fun getPixels(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        return pixels
    }

    private fun intColourToFloat(colour: Int): Float = colour / 255f

    private fun floatColourToInt(colour: Float): Int = (colour * 255f).roundToInt()

    private fun tensorToBitmap(tensorBuffer: TensorBuffer): Bitmap {
        val channels = tensorBuffer.shape[1]
        val height = tensorBuffer.shape[2]
        val width = tensorBuffer.shape[3]

        if (channels != 3) {
            throw IllegalStateException("TensorBuffer must contain RGB image ! Image with $channels channels found instead")
        }

        val channelMatrixSize = width * height
        val pixels = IntArray(channelMatrixSize)
        val greenStartIndex = 1 * channelMatrixSize
        val blueStartIndex = 2 * channelMatrixSize
        tensorBuffer.buffer.let {
            (0 until channelMatrixSize).forEach { index ->
                val red = it.getFloat(index * Float.SIZE_BYTES)
                val green = it.getFloat((index + greenStartIndex) * Float.SIZE_BYTES)
                val blue = it.getFloat((index + blueStartIndex) * Float.SIZE_BYTES)
                val colour = Color.rgb(red, green, blue)
                pixels[index] = colour
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
        return bitmap
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