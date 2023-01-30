package com.zhenxiang.superimage.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ContentInfoCompat
import com.zhenxiang.superimage.MainActivity


object IntentUtils {

    fun actionViewNewTask(uri: Uri) = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun appSettingsIntent(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )

    fun notificationPendingIntent(
        context: Context,
        intent: Intent,
        flags: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
    ): PendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

    fun mainActivityPendingIntent(
        context: Context,
        flags: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
    ): PendingIntent = notificationPendingIntent(context, Intent(context, MainActivity::class.java), flags)
}
