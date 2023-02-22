package com.zhenxiang.superimage.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.Settings
import androidx.core.net.toUri
import com.zhenxiang.superimage.MainActivity

object IntentUtils {

    fun actionViewNewTask(string: String) = Intent(Intent.ACTION_VIEW).apply {
        data = string.toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

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

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(
    name: String
): T? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getParcelableExtra(name, T::class.java)
} else {
    getParcelableExtra(name)
}

fun Intent.removeFlagsCompat(flags: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        removeFlags(flags)
    } else {
        this.flags = this.flags and flags.inv()
    }
}
