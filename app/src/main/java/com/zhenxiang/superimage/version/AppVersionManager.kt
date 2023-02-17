package com.zhenxiang.superimage.version

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.zhenxiang.superimage.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppVersionManager(private val dataStore: DataStore<Preferences>) {

    private val versionCodeKey = intPreferencesKey("version_code")

    fun refreshAppVersion() = runBlocking {
        val currentVersionCode = dataStore.data.first()[versionCodeKey]
        if (currentVersionCode == null || currentVersionCode < BuildConfig.VERSION_CODE) {
            dataStore.edit {
                it[versionCodeKey] = BuildConfig.VERSION_CODE
            }
        }
    }
}
