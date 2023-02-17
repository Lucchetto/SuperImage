package com.zhenxiang.superimage.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val SETTINGS_DATA_STORE_QUALIFIER = qualifier("settings")

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATA_STORE_QUALIFIER.value)

val DataStoreModule = module {
    single(SETTINGS_DATA_STORE_QUALIFIER) { get<Context>().dataStore }
}
