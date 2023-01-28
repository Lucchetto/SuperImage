package com.zhenxiang.superimage.home

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.model.DataState
import com.zhenxiang.superimage.model.InputImage
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.work.RealESRGANWorker
import com.zhenxiang.superimage.work.RealESRGANWorkerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File
import java.io.InputStream

class HomePageViewModel(application: Application): AndroidViewModel(application), KoinComponent {

    private val realESRGANWorkerManager = get<RealESRGANWorkerManager>()

    private val _selectedImageFlow: MutableStateFlow<DataState<InputImage, Unit>?>

    val selectedOutputFormatFlow: MutableStateFlow<OutputFormat>
    val selectedUpscalingModelFlow: MutableStateFlow<UpscalingModel>
    val selectedImageFlow: StateFlow<DataState<InputImage, Unit>?>
    val workProgressFlow = realESRGANWorkerManager.workProgressFlow

    init {
        val inputData = workProgressFlow.value?.first
        if (inputData != null) {
            _selectedImageFlow = MutableStateFlow(DataState.Success(inputData.toInputImage(application)))
            selectedOutputFormatFlow = MutableStateFlow(inputData.outputFormat)
            selectedUpscalingModelFlow = MutableStateFlow(inputData.upscalingModel)
        } else {
            _selectedImageFlow = MutableStateFlow(null)
            selectedOutputFormatFlow = MutableStateFlow(OutputFormat.PNG)
            selectedUpscalingModelFlow = MutableStateFlow(UpscalingModel.X4_PLUS)
        }
        selectedImageFlow = _selectedImageFlow
    }

    fun loadImage(imageUri: Uri) {
        _selectedImageFlow.apply {
            val currentTempImageFile = (value as? DataState.Success)?.data?.tempFile
            tryEmit(DataState.Loading())
            viewModelScope.launch(Dispatchers.IO) {
                currentTempImageFile?.let { realESRGANWorkerManager.deleteTempImageFile(it) }
                createInputImage(imageUri)?.let {
                    emit(DataState.Success(it))
                } ?: emit(DataState.Error(Unit))
            }
        }
    }

    private fun createInputImage(imageUri: Uri): InputImage? {
        val application = getApplication<Application>()
        val imageFileName = DocumentFile.fromSingleUri(application, imageUri)?.name ?: return null
        val tempImageFile = application.contentResolver.openInputStream(imageUri)?.use {
            copyToTempFile(it)
        } ?: return null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        tempImageFile.inputStream().use {
            BitmapFactory.decodeStream(it, null, options)
        }
        return InputImage(imageFileName, tempImageFile, options.outWidth, options.outHeight)
    }

    private fun copyToTempFile(inputStream: InputStream): File = realESRGANWorkerManager.createTempImageFile().apply {
        outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    fun upscale() {
        (selectedImageFlow.value as DataState.Success).data.let {
            viewModelScope.launch(Dispatchers.IO) {
                realESRGANWorkerManager.beginWork(
                    RealESRGANWorker.InputData(it.fileName, it.tempFile.name, selectedOutputFormatFlow.value, selectedUpscalingModelFlow.value)
                )
            }
        }
    }

    fun consumeWorkCompleted() = realESRGANWorkerManager.clearCurrentWorkProgress()

    override fun onCleared() {
        viewModelScope.launch(Dispatchers.IO) {
            (selectedImageFlow.value as? DataState.Success)?.data?.tempFile?.let {
                realESRGANWorkerManager.deleteTempImageFile(it)
            }
        }
        super.onCleared()
    }

    companion object {
        const val IMAGE_MIME_TYPE = "image/*"
    }
}

private fun RealESRGANWorker.InputData.toInputImage(context: Context) = with(RealESRGANWorkerManager.getTempFile(context, tempFileName)) {
    inputStream().use {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(it, null, options)
        InputImage(originalFileName, this, options.outWidth, options.outHeight)
    }
}
