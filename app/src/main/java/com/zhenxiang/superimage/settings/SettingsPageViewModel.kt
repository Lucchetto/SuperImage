package com.zhenxiang.superimage.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel

class SettingsPageViewModel(application: Application): AndroidViewModel(application) {

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
}