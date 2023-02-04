package com.zhenxiang.superimage.ui.daynight

import com.zhenxiang.superimage.datastore.SETTINGS_DATA_STORE
import org.koin.core.qualifier.named
import org.koin.dsl.module

val DayNightModule = module {
    single { DayNightManager(get(named(SETTINGS_DATA_STORE))) }
}
