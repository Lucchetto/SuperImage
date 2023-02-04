package com.zhenxiang.superimage.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val SETTINGS_DATA_STORE = "settings"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATA_STORE)

val DataStoreModule = module {
    single(named(SETTINGS_DATA_STORE)) { get<Context>().dataStore }
}
