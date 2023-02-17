package com.zhenxiang.superimage

import android.app.Application
import com.zhenxiang.superimage.datastore.DataStoreModule
import com.zhenxiang.superimage.datastore.SETTINGS_DATA_STORE_QUALIFIER
import com.zhenxiang.superimage.intent.InputImageIntentManagerModule
import com.zhenxiang.superimage.ui.daynight.DayNightModule
import com.zhenxiang.superimage.version.AppVersionUtils
import com.zhenxiang.superimage.work.RealESRGANWorkerModule
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import timber.log.Timber

class SuperImageApplication: Application(), KoinComponent {

    override fun onCreate() {
        Timber.plant(Timber.DebugTree())
        super.onCreate()
        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@SuperImageApplication)

            modules(
                RealESRGANWorkerModule,
                DataStoreModule,
                DayNightModule,
                InputImageIntentManagerModule,
            )
        }
        runBlocking {
            AppVersionUtils.refreshAppVersion(this@SuperImageApplication, get(SETTINGS_DATA_STORE_QUALIFIER))
        }
    }
}