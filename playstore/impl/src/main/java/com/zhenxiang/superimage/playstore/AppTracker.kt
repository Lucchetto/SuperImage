package com.zhenxiang.superimage.playstore

import android.content.Context
import com.zhenxiang.superimage.playstore.base.BaseAppTracker

class AppTracker(context: Context): BaseAppTracker(context) {

    override fun trackInstall() = Unit

    override fun trackViewScreen(screenId: String) = Unit

    override fun trackAction(actionId: String) = Unit
}
