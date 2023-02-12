package com.zhenxiang.superimage.home

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.documentfile.provider.DocumentFile
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.zhenxiang.realesrgan.UpscalingModel
import com.zhenxiang.superimage.datastore.SETTINGS_DATA_STORE
import com.zhenxiang.superimage.datastore.writeIntIdentifiable
import com.zhenxiang.superimage.model.DataState
import com.zhenxiang.superimage.model.InputImage
import com.zhenxiang.superimage.model.OutputFormat
import com.zhenxiang.superimage.navigation.ChildComponent
import com.zhenxiang.superimage.navigation.RootComponent
import com.zhenxiang.superimage.navigation.getViewModel
import com.zhenxiang.superimage.work.RealESRGANWorker
import com.zhenxiang.superimage.work.RealESRGANWorkerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File
import java.io.InputStream

class HomePageComponent(
    componentContext: ComponentContext,
    navigation: StackNavigation<RootComponent.Config>
): ChildComponent<HomePageComponent.ViewModel>(componentContext, navigation) {

    override val viewModel = getViewModel(::ViewModel)

    class ViewModel: ChildComponent.ViewModel(), KoinComponent {
        private val context by inject<Context>()
        private val realESRGANWorkerManager = get<RealESRGANWorkerManager>()
        private val dataStore by inject<DataStore<Preferences>>(named(SETTINGS_DATA_STORE))
        private val outputFormatPrefKey = intPreferencesKey("output_format")
        private val upscalingModelPrefKey = intPreferencesKey("upscaling_model")

        private val _selectedImageFlow: MutableStateFlow<DataState<InputImage, Unit>?>

        val selectedOutputFormatFlow: MutableStateFlow<OutputFormat>
        val selectedUpscalingModelFlow: MutableStateFlow<UpscalingModel>
        val selectedImageFlow: StateFlow<DataState<InputImage, Unit>?>
        val workProgressFlow = realESRGANWorkerManager.workProgressFlow

        init {
            val inputData = workProgressFlow.value?.first
            if (inputData != null) {
                _selectedImageFlow = MutableStateFlow(DataState.Success(inputData.toInputImage(context)))
                selectedOutputFormatFlow = MutableStateFlow(inputData.outputFormat)
                selectedUpscalingModelFlow = MutableStateFlow(inputData.upscalingModel)
            } else {
                _selectedImageFlow = MutableStateFlow(null)
                val prefs = runBlocking { dataStore.data.first() }
                selectedOutputFormatFlow = MutableStateFlow(
                    OutputFormat.fromId(prefs[outputFormatPrefKey]) ?: OutputFormat.PNG
                )
                selectedUpscalingModelFlow = MutableStateFlow(
                    UpscalingModel.fromId(prefs[upscalingModelPrefKey]) ?: UpscalingModel.X2_PLUS
                )
            }
            coroutineScope.launch(Dispatchers.IO) {
                selectedOutputFormatFlow.collect {
                    dataStore.writeIntIdentifiable(outputFormatPrefKey, it)
                }
            }
            coroutineScope.launch(Dispatchers.IO) {
                selectedUpscalingModelFlow.collect {
                    dataStore.writeIntIdentifiable(upscalingModelPrefKey, it)
                }
            }
            selectedImageFlow = _selectedImageFlow
        }

        fun loadImage(imageUri: Uri) {
            _selectedImageFlow.apply {
                val currentTempImageFile = (value as? DataState.Success)?.data?.tempFile
                tryEmit(DataState.Loading())
                coroutineScope.launch(Dispatchers.IO) {
                    currentTempImageFile?.let { realESRGANWorkerManager.deleteTempImageFile(it) }
                    createInputImage(imageUri)?.let {
                        emit(DataState.Success(it))
                    } ?: emit(DataState.Error(Unit))
                }
            }
        }

        private fun createInputImage(imageUri: Uri): InputImage? {
            val imageFileName = DocumentFile.fromSingleUri(context, imageUri)?.name ?: return null
            val tempImageFile = context.contentResolver.openInputStream(imageUri)?.use {
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
                realESRGANWorkerManager.beginWork(
                    RealESRGANWorker.InputData(it.fileName, it.tempFile.name, selectedOutputFormatFlow.value, selectedUpscalingModelFlow.value)
                )
            }
        }

        fun cancelWork() {
            realESRGANWorkerManager.cancelWork()
        }

        fun consumeWorkCompleted() {
            realESRGANWorkerManager.clearCurrentWorkProgress()
        }

        fun clearSelectedImage() {
            _selectedImageFlow.tryEmit(null)
        }
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
