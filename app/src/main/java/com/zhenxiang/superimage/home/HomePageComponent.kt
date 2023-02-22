package com.zhenxiang.superimage.home

import android.app.Activity
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
import com.zhenxiang.superimage.BuildConfig
import com.zhenxiang.superimage.datastore.SETTINGS_DATA_STORE_QUALIFIER
import com.zhenxiang.superimage.datastore.writeIntIdentifiable
import com.zhenxiang.superimage.intent.InputImageIntentManager
import com.zhenxiang.superimage.shared.model.Changelog
import com.zhenxiang.superimage.shared.model.DataState
import com.zhenxiang.superimage.shared.model.InputImage
import com.zhenxiang.superimage.shared.model.OutputFormat
import com.zhenxiang.superimage.navigation.ChildComponent
import com.zhenxiang.superimage.navigation.RootComponent
import com.zhenxiang.superimage.navigation.getViewModel
import com.zhenxiang.superimage.playstore.InAppReviewManager
import com.zhenxiang.superimage.tracking.AppReviewTracking
import com.zhenxiang.superimage.tracking.AppVersionTracking
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
import timber.log.Timber
import java.io.File
import java.io.InputStream

class HomePageComponent(
    componentContext: ComponentContext,
    navigation: StackNavigation<RootComponent.Config>
): ChildComponent<HomePageComponent.ViewModel>(componentContext, navigation) {

    override val viewModel = getViewModel(::ViewModel)

    class ViewModel: ChildComponent.ViewModel(), KoinComponent {

        private val context by inject<Context>()
        private val appReviewTracking by inject<AppReviewTracking>()
        private val inAppReviewManager = InAppReviewManager(context)

        private val realESRGANWorkerManager = get<RealESRGANWorkerManager>()
        private val inputImageIntentManager by inject<InputImageIntentManager>()
        private val dataStore by inject<DataStore<Preferences>>(SETTINGS_DATA_STORE_QUALIFIER)
        private val outputFormatPrefKey = intPreferencesKey("output_format")
        private val upscalingModelPrefKey = intPreferencesKey("upscaling_model")

        private val _selectedImageFlow: MutableStateFlow<DataState<InputImage, Unit>?>

        val selectedOutputFormatFlow: MutableStateFlow<OutputFormat>
        val selectedUpscalingModelFlow: MutableStateFlow<UpscalingModel>
        val selectedImageFlow: StateFlow<DataState<InputImage, Unit>?>
        val workProgressFlow = realESRGANWorkerManager.workProgressFlow

        val showChangelogFlow: MutableStateFlow<Changelog>

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
            viewModelScope.launch(Dispatchers.IO) {
                selectedOutputFormatFlow.collect {
                    dataStore.writeIntIdentifiable(outputFormatPrefKey, it)
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                selectedUpscalingModelFlow.collect {
                    dataStore.writeIntIdentifiable(upscalingModelPrefKey, it)
                }
            }
            selectedImageFlow = _selectedImageFlow

            viewModelScope.launch {
                inputImageIntentManager.imageUriFlow.collect {
                    when (realESRGANWorkerManager.workProgressFlow.value?.second) {
                        is RealESRGANWorker.Progress.Running -> {}
                        is RealESRGANWorker.Progress.Failed, is RealESRGANWorker.Progress.Success -> {
                            consumeWorkCompleted()
                            clearSelectedImage()
                            loadImage(it)
                        }
                        null -> loadImage(it)
                    }
                }
            }

            if (runBlocking { AppVersionTracking.shouldShowChangelog(dataStore) }) {
                showChangelogFlow = MutableStateFlow(Changelog.Loading)
                viewModelScope.launch(Dispatchers.IO) {
                    AppVersionTracking.clearShowChangelog(dataStore)
                    showChangelogFlow.tryEmit(readChangelog())
                }
            } else {
                showChangelogFlow = MutableStateFlow(Changelog.Hide)
            }

            setupShowReviewTracking()
        }

        private fun setupShowReviewTracking() {
            viewModelScope.launch(Dispatchers.IO) {
                if (appReviewTracking.shouldShowReviewFlow()) {
                    inAppReviewManager.prepareReviewInfo()
                } else {
                    workProgressFlow.collect {
                        if (it?.second is RealESRGANWorker.Progress.Success && appReviewTracking.shouldShowReviewFlow()) {
                            inAppReviewManager.prepareReviewInfo()
                        }
                    }
                }
            }
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

        fun upscale() = (selectedImageFlow.value as DataState.Success).data.let {
            realESRGANWorkerManager.beginWork(
                RealESRGANWorker.InputData(it.fileName, it.tempFile.name, selectedOutputFormatFlow.value, selectedUpscalingModelFlow.value)
            )
        }

        fun cancelWork() {
            realESRGANWorkerManager.cancelWork()
        }

        fun consumeWorkCompleted() {
            realESRGANWorkerManager.clearCurrentWorkProgress()
        }

        fun clearSelectedImage() = _selectedImageFlow.value.let {
            _selectedImageFlow.tryEmit(null)
            if (it is DataState.Success) {
                viewModelScope.launch {
                    realESRGANWorkerManager.deleteTempImageFile(it.data.tempFile)
                }
            }
        }

        private suspend fun readChangelog(): Changelog = try {
            context.assets.open(BuildConfig.CHANGELOG_ASSET_NAME).reader().use {
                val lines = mutableListOf<String>()
                it.forEachLine { line ->
                    if (line.isNotBlank()) {
                        lines.add(line)
                    }
                }
                if (lines.isEmpty()) Changelog.Hide else Changelog.Show(lines)
            }
        } catch (e: Exception) {
            Timber.wtf(e)
            Changelog.Hide
        }

        fun showReviewFlowConditionally(activity: Activity) {
            if (inAppReviewManager.reviewFlowReady) {
                inAppReviewManager.launchReviewFlow(activity)
            }
        }
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
