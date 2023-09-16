package com.zhenxiang.superimage.playstore

import android.content.Context
import com.zhenxiang.superimage.playstore.base.BaseAppTracker
import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.extra.DownloadTracker
import org.matomo.sdk.extra.TrackHelper

class AppTracker(private val context: Context): BaseAppTracker(context) {

    private val tracker = TrackerBuilder
        .createDefault("https://superimage.matomo.cloud/matomo.php", 1)
        .build(Matomo.getInstance(context))

    override fun trackInstall() {
        TrackHelper.track().download().identifier(DownloadTracker.Extra.ApkChecksum(context)).with(tracker)
        tracker.dispatch()
    }

    override fun trackViewScreen(screenId: String) {
        TrackHelper.track().screen(screenId).with(tracker)
        tracker.dispatch()
    }

    override fun trackAction(actionId: String) {
        TrackHelper.track().interaction(actionId, "action").with(tracker)
        tracker.dispatch()
    }
}
