package com.zhenxiang.superimage.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat.startActivity


object IntentUtils {

    fun actionViewNewTask(uri: Uri) = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun appSettingsIntent(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
}
