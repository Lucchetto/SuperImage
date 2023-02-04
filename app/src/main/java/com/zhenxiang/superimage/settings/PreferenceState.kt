package com.zhenxiang.superimage.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class IntPreferenceState(
    private val dataStore: DataStore<Preferences>,
    key: String,
    defaultValue: Int
) {

    private val prefKey = intPreferencesKey(key)

    private val valueFlow = dataStore.data.map { it[prefKey] ?: defaultValue }

    val state: State<Int>
        @Composable
        get() = valueFlow.collectAsStateWithLifecycle(initialValue = runBlocking { valueFlow.first() })

    suspend fun setValue(value: Int) = withContext(Dispatchers.IO) {
        dataStore.edit { it[prefKey] = value }
    }
}