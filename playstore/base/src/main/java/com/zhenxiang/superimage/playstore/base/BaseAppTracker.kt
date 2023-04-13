package com.zhenxiang.superimage.playstore.base

import android.content.Context

abstract class BaseAppTracker(context: Context) {

    abstract fun trackInstall()

    abstract fun trackViewScreen(screenId: String)

    abstract fun trackAction(actionId: String)
}
