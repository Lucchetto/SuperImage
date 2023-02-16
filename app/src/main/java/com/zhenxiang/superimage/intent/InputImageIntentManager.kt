package com.zhenxiang.superimage.intent

import android.content.Intent
import android.net.Uri
import com.zhenxiang.superimage.utils.getParcelableExtraCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class InputImageIntentManager {

    private val _intentUriFlow = MutableSharedFlow<Uri>(1, 1, BufferOverflow.DROP_OLDEST)
    val intentUriFlow: SharedFlow<Uri> = _intentUriFlow

    fun notifyNewIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val uri = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
            if (uri != null && _intentUriFlow.replayCache.firstOrNull() != uri) {
                _intentUriFlow.tryEmit(uri)
            }
        }
    }

    companion object {
        const val IMAGE_MIME_TYPE = "image/*"
    }
}
