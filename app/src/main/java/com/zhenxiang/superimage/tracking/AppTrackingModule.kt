package com.zhenxiang.superimage.tracking

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

private val TRACKING_DATA_STORE_QUALIFIER = qualifier("tracking")

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = TRACKING_DATA_STORE_QUALIFIER.value)

val AppTrackingModule = module {
    single { AppReviewTracking(get<Context>().dataStore) }
}
