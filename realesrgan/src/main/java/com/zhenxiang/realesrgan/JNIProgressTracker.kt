package com.zhenxiang.realesrgan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wrapper to help tracking progress of the execution of a native function
 */
class JNIProgressTracker {

    private val _progressFlow = MutableStateFlow(INDETERMINATE)
    val progressFlow: StateFlow<Float>
        get() = _progressFlow

    fun setProgress(progress: Float) {
        _progressFlow.tryEmit(progress)
    }

    companion object {
        const val INDETERMINATE = -1f
    }
}
