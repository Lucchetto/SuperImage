package com.zhenxiang.realesrgan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wrapper to help tracking progress of the execution of a native function
 */
class JNIProgressTracker {

    private val _progressFlow = MutableStateFlow<Progress>(Progress.Indeterminate)
    val progressFlow: StateFlow<Progress>
        get() = _progressFlow

    fun setProgress(progress: Progress) {
        _progressFlow.tryEmit(progress)
    }

    fun setIndeterminate() {
        _progressFlow.tryEmit(Progress.Indeterminate)
    }

    fun setLoadingPercentage(percentage: Float) {
        _progressFlow.tryEmit(Progress.Loading(percentage))
    }

    fun setSuccess() {
        _progressFlow.tryEmit(Progress.Success)
    }

    fun setError() {
        _progressFlow.tryEmit(Progress.Error)
    }

    sealed interface Progress {

        object Indeterminate: Progress

        data class Loading(val percentage: Float): Progress

        object Success: Progress

        object Error: Progress
    }
}
