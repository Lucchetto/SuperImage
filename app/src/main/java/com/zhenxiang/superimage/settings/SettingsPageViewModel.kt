package com.zhenxiang.superimage.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import com.zhenxiang.superimage.utils.IntentUtils

class SettingsPageViewModel(application: Application): AndroidViewModel(application) {

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    companion object {

        const val GITHUB_PAGE_URL = "github.com/Lucchetto/SuperImage"

        fun openGithubPage(context: Context) {
            context.startActivity(IntentUtils.actionViewNewTask("https://$GITHUB_PAGE_URL"))
        }
    }
}