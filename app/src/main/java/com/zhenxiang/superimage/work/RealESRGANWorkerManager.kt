package com.zhenxiang.superimage.work

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

private typealias InputAndProgress = Pair<RealESRGANWorker.InputData, RealESRGANWorker.Progress>

class RealESRGANWorkerManager(context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val currentWorkerLiveData = MutableLiveData<Pair<UUID, RealESRGANWorker.InputData>?>()
    private val workInfosLiveData = MutableLiveData<List<WorkInfo>>()

    val workProgressFlow: StateFlow<InputAndProgress?>

    init {
        workManager.getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_ID).observeForever {
            workInfosLiveData.value = it
        }
        workProgressFlow = MutableStateFlow<InputAndProgress?>(null).apply {
            currentWorkerLiveData.observeForever { currentWorker ->
                tryEmit(workInfosLiveData.value?.let { infos -> processWorkInfo(infos, currentWorker) })
            }
            workInfosLiveData.observeForever { infos ->
                tryEmit(currentWorkerLiveData.value?.let { currentWorker -> processWorkInfo(infos, currentWorker) })
            }
        }
    }

    fun beginWork(input: RealESRGANWorker.InputData) {
        // TODO: Check if worker is already running
        val request = OneTimeWorkRequestBuilder<RealESRGANWorker>().setInputData(input.toWorkData()).build()
        currentWorkerLiveData.value = Pair(request.id, input)
        workManager.enqueueUniqueWork(UNIQUE_WORK_ID, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val UNIQUE_WORK_ID = "real_esrgan"

        private fun processWorkInfo(workInfos: List<WorkInfo>, currentWorker: Pair<UUID, RealESRGANWorker.InputData>?): InputAndProgress? {
            val workInfo = workInfos.firstOrNull()
            return if (workInfo != null && currentWorker != null && workInfo.id == currentWorker.first) {
                RealESRGANWorker.Progress.fromWorkInfo(workInfo)?.let {
                    Pair(currentWorker.second, it)
                }
            } else {
                null
            }
        }
    }
}
