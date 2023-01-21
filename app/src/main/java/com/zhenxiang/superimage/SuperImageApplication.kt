package com.zhenxiang.superimage

import android.app.Application
import timber.log.Timber

class SuperImageApplication: Application() {

    override fun onCreate() {
        Timber.plant(Timber.DebugTree())
        super.onCreate()
    }
}