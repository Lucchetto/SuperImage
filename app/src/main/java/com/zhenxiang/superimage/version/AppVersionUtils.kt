package com.zhenxiang.superimage.version

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.zhenxiang.superimage.BuildConfig
import com.zhenxiang.superimage.utils.appNeverUpdated
import kotlinx.coroutines.flow.first

object AppVersionUtils {

    private val showChangelogKey: Preferences.Key<Boolean>
        get() = booleanPreferencesKey("show_changelog")

    suspend fun refreshAppVersion(context: Context, dataStore: DataStore<Preferences>) {
        val versionCodeKey = intPreferencesKey("version_code")

        val currentVersionCode = dataStore.data.first()[versionCodeKey]
        if (
            (currentVersionCode == null && !context.appNeverUpdated) ||
            (currentVersionCode != null && currentVersionCode < BuildConfig.VERSION_CODE)
        ) {
            dataStore.edit {
                it[versionCodeKey] = BuildConfig.VERSION_CODE
                it[showChangelogKey] = true
            }
        }
    }

    suspend fun shouldShowChangelog(dataStore: DataStore<Preferences>) =
        dataStore.data.first()[showChangelogKey] == true

    suspend fun clearShowChangelog(dataStore: DataStore<Preferences>) = dataStore.edit {
        it.remove(showChangelogKey)
    }

    const val CHANGELOG_FILE_NAME = "changelog.txt"
}
