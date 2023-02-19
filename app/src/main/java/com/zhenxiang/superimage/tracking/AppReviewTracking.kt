package com.zhenxiang.superimage.tracking

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first

class AppReviewTracking(private val dataStore: DataStore<Preferences>) {

    private val successfulUpscalingCountKey = intPreferencesKey("success_upscaling")

    suspend fun trackUpscalingSuccess() {
        dataStore.edit {
            it[successfulUpscalingCountKey] = (it[successfulUpscalingCountKey] ?: 0) + 1
        }
    }

    suspend fun shouldShowReview() = with(dataStore.data.first()) {
        (this[successfulUpscalingCountKey] ?: 0) >= SHOW_APP_REVIEW_THRESHOLD
    }

    companion object {

        private const val SHOW_APP_REVIEW_THRESHOLD = 3
    }
}
