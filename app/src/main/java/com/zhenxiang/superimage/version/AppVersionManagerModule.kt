package com.zhenxiang.superimage.version

import com.zhenxiang.superimage.datastore.SETTINGS_DATA_STORE_QUALIFIER
import org.koin.dsl.module

val AppVersionManagerModule = module {
    single { AppVersionManager(get(SETTINGS_DATA_STORE_QUALIFIER)) }
}
