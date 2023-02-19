package com.zhenxiang.superimage.playstore

import android.app.Activity
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class InAppReviewManager(context: Context) {

    private val manager = ReviewManagerFactory.create(context)

    private var busy = false

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var reviewInfo: ReviewInfo? = null

    val readyForReview: Boolean
        get() = reviewInfo != null

    fun prepareReviewInfo() {
        if (busy) {
            return
        }
        busy = true
        manager.requestReviewFlow().addOnCompleteListener { task ->
            reviewInfo = if (task.isSuccessful) task.result else null
            busy = false
        }
    }

    fun launchReviewFlow(activity: Activity) {
        reviewInfo?.let { info ->
            reviewInfo = null
            manager.launchReviewFlow(activity, info)
        }
    }
}