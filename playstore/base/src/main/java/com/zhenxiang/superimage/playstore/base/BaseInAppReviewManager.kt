package com.zhenxiang.superimage.playstore.base

import android.app.Activity
import android.content.Context

abstract class BaseInAppReviewManager(context: Context) {

    abstract val reviewFlowReady: Boolean

    abstract fun prepareReviewInfo()

    abstract fun launchReviewFlow(activity: Activity)
}