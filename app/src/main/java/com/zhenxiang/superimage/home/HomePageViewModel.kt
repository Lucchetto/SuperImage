package com.zhenxiang.superimage.home

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.model.DataState
import com.zhenxiang.superimage.model.InputImage
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.work.RealESRGANWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomePageViewModel(application: Application): AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(getApplication())

    private val _selectedImageFlow = MutableStateFlow<DataState<InputImage, Unit>?>(null)

    val selectedOutputFormatFlow = MutableStateFlow(OutputFormat.PNG)
    val selectedUpscalingModelFlow = MutableStateFlow(UpscalingModel.X4_PLUS)
    val selectedImageFlow: StateFlow<DataState<InputImage, Unit>?> = _selectedImageFlow

    fun loadImage(imageUri: Uri) {
        _selectedImageFlow.apply {
            tryEmit(DataState.Loading())
            viewModelScope.launch(Dispatchers.IO) {
                imageUri.toInputImage(getApplication())?.let {
                    emit(DataState.Success(it))
                } ?: emit(DataState.Error(Unit))
            }
        }
    }

    fun upscale() {
        (selectedImageFlow.value as DataState.Success).data.let {
            val selectedModel = selectedUpscalingModelFlow.value
            val inputData = workDataOf(
                RealESRGANWorker.INPUT_IMAGE_URI_PARAM to it.fileUri.toString(),
                RealESRGANWorker.INPUT_IMAGE_NAME_PARAM to it.fileName,
                RealESRGANWorker.OUTPUT_IMAGE_FORMAT_PARAM to selectedOutputFormatFlow.value.formatName,
                RealESRGANWorker.UPSCALING_MODEL_PATH_PARAM to selectedModel.assetPath,
                RealESRGANWorker.UPSCALING_SCALE_PARAM to selectedModel.scale
            )
            workManager.beginUniqueWork(
                RealESRGANWorker.UNIQUE_WORK_ID,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<RealESRGANWorker>().setInputData(inputData).build()
            ).enqueue()
        }
    }

    companion object {
        const val IMAGE_MIME_TYPE = "image/*"
    }
}

private fun Uri.toInputImage(context: Context): InputImage? = DocumentFile.fromSingleUri(context, this)?.let {
    val fileName = it.name ?: return null
    return try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            InputImage(fileName, this, options.outWidth, options.outHeight)
        }
    } catch (e: Exception) {
        null
    }
}
