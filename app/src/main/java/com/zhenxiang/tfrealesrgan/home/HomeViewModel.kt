package com.zhenxiang.tfrealesrgan.home

import android.app.Application
import android.content.ContentResolver
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random
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

        interpreter.run(inputTensor.buffer, outputBufferTensor.buffer)
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

    private fun generateRandomFloatMatrix(width: Int, height: Int): Array<FloatArray> {
        val random = Random(0)
        val matrix = Array(height) { y ->
            val row = FloatArray(width)
            (0 until width).forEach { x ->
                row[x] = random.nextFloat()
            }
            row
        }

        return matrix
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {

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