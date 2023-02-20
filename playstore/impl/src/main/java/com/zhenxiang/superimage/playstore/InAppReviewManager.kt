package com.zhenxiang.superimage.playstore

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.zhenxiang.superimage.playstore.base.BaseInAppReviewManager

class InAppReviewManager(context: Context): BaseInAppReviewManager(context) {

    private val manager = ReviewManagerFactory.create(context)

    private var busy = false

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var reviewInfo: ReviewInfo? = null

    override val reviewFlowReady: Boolean
        get() = reviewInfo != null

    override fun prepareReviewInfo() {
        if (busy || reviewInfo != null) {
            return
        }
        busy = true
        manager.requestReviewFlow().addOnCompleteListener { task ->
            reviewInfo = if (task.isSuccessful) task.result else null
            busy = false
        }
    }

    override fun launchReviewFlow(activity: Activity) {
        reviewInfo?.let { info ->
            reviewInfo = null
            manager.launchReviewFlow(activity, info)
        }
    }
}