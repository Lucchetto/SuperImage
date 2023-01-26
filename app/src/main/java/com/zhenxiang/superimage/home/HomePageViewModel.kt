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
import com.zhenxiang.superimage.model.InputImagePreview
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.work.RealESRGANWorker
import com.zhenxiang.superimage.work.RealESRGANWorkerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomePageViewModel(application: Application): AndroidViewModel(application), KoinComponent {

    private val realESRGANWorkerManager by inject<RealESRGANWorkerManager>()

    private val _selectedImageFlow = MutableStateFlow<DataState<InputImagePreview, Unit>?>(null)

    val selectedOutputFormatFlow = MutableStateFlow(OutputFormat.PNG)
    val selectedUpscalingModelFlow = MutableStateFlow(UpscalingModel.X4_PLUS)
    val selectedImageFlow: StateFlow<DataState<InputImagePreview, Unit>?> = _selectedImageFlow

    fun loadImage(imageUri: Uri) {
        _selectedImageFlow.apply {
            tryEmit(DataState.Loading())
            viewModelScope.launch(Dispatchers.IO) {
                imageUri.toInputImagePreview(getApplication())?.let {
                    emit(DataState.Success(it))
                } ?: emit(DataState.Error(Unit))
            }
        }
    }

    fun upscale() {
        (selectedImageFlow.value as DataState.Success).data.let {
            realESRGANWorkerManager.beginWork(
                RealESRGANWorker.InputData(it.fileName, it.fileUri, selectedOutputFormatFlow.value, selectedUpscalingModelFlow.value)
            )
        }
    }

    companion object {
        const val IMAGE_MIME_TYPE = "image/*"
    }
}

private fun Uri.toInputImagePreview(context: Context): InputImagePreview? = DocumentFile.fromSingleUri(context, this)?.let {
    val fileName = it.name ?: return null
    return try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            InputImagePreview(fileName, this, options.outWidth, options.outHeight)
        }
    } catch (e: Exception) {
        null
    }
}
