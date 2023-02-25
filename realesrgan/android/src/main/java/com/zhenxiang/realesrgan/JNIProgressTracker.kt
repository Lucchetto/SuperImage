package com.zhenxiang.realesrgan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wrapper to help tracking progress of the execution of a native function
 */
class JNIProgressTracker {

    private val _progressFlow = MutableStateFlow(Progress(INDETERMINATE_PROGRESS, INDETERMINATE_TIME))
    val progressFlow: StateFlow<Progress>
        get() = _progressFlow

    fun setProgress(value: Float, estimatedTime: Long) {
        _progressFlow.tryEmit(Progress(value, estimatedTime))
    }

    /**
     * @param value progress in range 0 to 100 or [INDETERMINATE_PROGRESS]
     * @param estimatedMillisLeft estimated time left in milliseconds
     */
    data class Progress(val value: Float, val estimatedMillisLeft: Long)

    companion object {
        const val INDETERMINATE_PROGRESS = -1f
        const val INDETERMINATE_TIME = -1L
    }
}
