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

    fun load(imageUri: Uri) {
        val croppedBitmap = loadImageFromUri(getApplication<Application>().contentResolver, imageUri)?.let {
            val cropped = Bitmap.createBitmap(it, 0, 0, 64, 64)
            it.recycle()
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
        TensorFlowLite.init()

        val options = Interpreter.Options().apply {
            useNNAPI = true
        }
        val interpreter = Interpreter(
            loadModelFile(getApplication<Application>().assets, "realesrgan-x4plus.tflite"),
            options
        )
        val inputTensor = tensor(bitmap)
        val outputBufferTensor = TensorBuffer.createFixedSize(intArrayOf(1, 3, 256, 256), DataType.FLOAT32)
        bitmap.recycle()

        interpreter.run(inputTensor.buffer, outputBufferTensor.buffer)

        getOutputImageFile("${UUID.randomUUID()}.png")?.outputStream()?.use {
            tensorToBitmap(outputBufferTensor).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun tensor(bitmap: Bitmap): TensorBuffer {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val tensor = TensorBuffer.createFixedSize(intArrayOf(1, 3, bitmap.height, bitmap.width), DataType.FLOAT32)
        val greenStartIndex = 1 * pixels.size
        val blueStartIndex = 2 * pixels.size
        tensor.buffer.let {
            pixels.forEachIndexed { index, pixel ->
                it.putFloat(index * Float.SIZE_BYTES, intColourToFloat(Color.red(pixel)))
                it.putFloat((greenStartIndex + index) * Float.SIZE_BYTES, intColourToFloat(Color.green(pixel)))
                it.putFloat((blueStartIndex + index) * Float.SIZE_BYTES, intColourToFloat(Color.blue(pixel)))
            }
            it.rewind()

        }

        return tensor
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
                val colour: Int =
                    255 and 0xff shl 24 or
                            (floatColourToInt(red) and 0xff shl 16) or
                            (floatColourToInt(green) and 0xff shl 8) or
                            (floatColourToInt(blue) and 0xff)
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