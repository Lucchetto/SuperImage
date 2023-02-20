package com.zhenxiang.superimage.playstore

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test

import org.junit.Before

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class InAppReviewManagerTest {

    private lateinit var inAppReviewManager: InAppReviewManager
    private val playStoreReviewManager = mockk<ReviewManager>()

    @Before
    fun setup() {
        mockkStatic(ReviewManagerFactory::create)
        every { ReviewManagerFactory.create(any()) } returns playStoreReviewManager
        inAppReviewManager = InAppReviewManager(mockk())
    }

    @Test
    fun testPrepareReviewInfo() {
        every { playStoreReviewManager.requestReviewFlow() } returns mockk {
            every { addOnCompleteListener(any()) } returns this
        }
        inAppReviewManager.prepareReviewInfo()
        inAppReviewManager.prepareReviewInfo()

        verify(exactly = 1) { playStoreReviewManager.requestReviewFlow() }
    }

    @Test
    fun testLaunchReviewFlow() {
        inAppReviewManager.reviewInfo = mockk()
        val fakeActivity = mockk<Activity>()
        every { playStoreReviewManager.launchReviewFlow(any(), any()) } returns mockk()

        inAppReviewManager.launchReviewFlow(fakeActivity)
        inAppReviewManager.launchReviewFlow(fakeActivity)

        verify(exactly = 1) { playStoreReviewManager.launchReviewFlow(any(), any()) }
    }
}