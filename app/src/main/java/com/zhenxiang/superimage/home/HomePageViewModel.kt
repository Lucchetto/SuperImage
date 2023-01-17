package com.zhenxiang.superimage.home

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.work.RealESRGANWorker
import kotlinx.coroutines.flow.MutableStateFlow

class HomePageViewModel(application: Application): AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(getApplication())

    val selectedUpscalingModelFlow = MutableStateFlow(UpscalingModel.X4_PLUS)

    fun upscale(imageUri: Uri) {
        DocumentFile.fromSingleUri(getApplication(), imageUri)?.name?.let {
            val selectedModel = selectedUpscalingModelFlow.value
            val inputData = workDataOf(
                RealESRGANWorker.INPUT_IMAGE_URI_PARAM to imageUri.toString(),
                RealESRGANWorker.INPUT_IMAGE_NAME_PARAM to it,
                RealESRGANWorker.UPSCALING_MODEL_PATH_PARAM to selectedModel.assetPath,
                RealESRGANWorker.UPSCALING_SCALE_PARAM to selectedModel.scale
            )
            workManager.beginWith(
                OneTimeWorkRequestBuilder<RealESRGANWorker>().setInputData(inputData).build()
            ).enqueue()
        }
    }

    companion object {
        const val IMAGE_MIME_TYPE = "image/*"
    }
}
