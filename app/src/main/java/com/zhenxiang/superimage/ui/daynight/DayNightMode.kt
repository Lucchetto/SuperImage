package com.zhenxiang.superimage.ui.daynight

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.zhenxiang.superimage.R
import com.zhenxiang.superimage.model.Identifiable

enum class DayNightMode(override val id: Int, @StringRes val stringRes: Int): Identifiable {
    AUTO(0, R.string.auto_label),
    DAY(1, R.string.light_label),
    NIGHT(2, R.string.dark_label);

    @NightMode
    val delegateNightMode: Int
        get() = when (this) {
            AUTO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            DAY -> AppCompatDelegate.MODE_NIGHT_NO
            NIGHT -> AppCompatDelegate.MODE_NIGHT_YES
        }

    val lightMode: Boolean
        @Composable get() = when (this) {
            AUTO -> !isSystemInDarkTheme()
            DAY -> true
            NIGHT -> false
        }

    companion object {

        val VALUES = values()

        fun fromId(id: Int) = VALUES.firstOrNull { it.id == id }

        fun fromDelegateNightMode(@NightMode mode: Int) = when (mode) {
            AppCompatDelegate.MODE_NIGHT_YES -> NIGHT
            AppCompatDelegate.MODE_NIGHT_NO -> DAY
            else -> AUTO
        }
    }
}
