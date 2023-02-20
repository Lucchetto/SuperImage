package com.zhenxiang.superimage.playstore

import android.app.Activity
import android.content.Context
import com.zhenxiang.superimage.playstore.base.BaseInAppReviewManager

class InAppReviewManager(context: Context): BaseInAppReviewManager(context) {

    override val reviewFlowReady = false

    override fun prepareReviewInfo() {}

    override fun launchReviewFlow(activity: Activity) {}
}