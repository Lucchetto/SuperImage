package com.zhenxiang.superimage.intent

import android.content.Intent
import android.net.Uri
import com.zhenxiang.superimage.utils.getParcelableExtraCompat
import com.zhenxiang.superimage.utils.removeFlagsCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

class InputImageIntentManager {

    private val _intentUriChannel = Channel<Uri>(1, BufferOverflow.DROP_OLDEST)
    val imageUriFlow: Flow<Uri>
        get() = _intentUriChannel.consumeAsFlow()

    /**
     * @return whether the [Intent] has been consumed
     */
    fun notifyNewIntent(intent: Intent): Boolean {
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)?.let {
                _intentUriChannel.trySend(it)
                return true
            }
        }
        return false
    }

    companion object {
        const val IMAGE_MIME_TYPE = "image/*"

        fun markAsConsumed(intent: Intent): Intent = intent.apply {
            intent.removeFlagsCompat(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            intent.data = null
        }
    }
}
