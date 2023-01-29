package com.zhenxiang.superimage.utils

import android.content.Intent
import android.net.Uri

object IntentUtils {

    fun actionViewNewTask(uri: Uri) = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
