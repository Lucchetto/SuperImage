package com.zhenxiang.superimage

import android.app.Application
import com.zhenxiang.superimage.work.RealESRGANWorkerModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class SuperImageApplication: Application() {

    override fun onCreate() {
        Timber.plant(Timber.DebugTree())
        super.onCreate()
        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@SuperImageApplication)

            modules(RealESRGANWorkerModule)
        }
    }
}