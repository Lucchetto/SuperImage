package com.zhenxiang.superimage.ui.daynight

import com.zhenxiang.superimage.datastore.SETTINGS_DATA_STORE_QUALIFIER
import org.koin.dsl.module

val DayNightModule = module {
    single { DayNightManager(get(SETTINGS_DATA_STORE_QUALIFIER)) }
}
