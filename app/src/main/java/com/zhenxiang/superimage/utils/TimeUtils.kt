package com.zhenxiang.superimage.utils

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import com.zhenxiang.superimage.R
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

object TimeUtils {

    fun periodToString(context: Context, millis: Long): String = with(context.resources) {
        val unitSpace = getString(R.string.time_unit_space)
        val wordSpace = getString(R.string.word_space)
        val formatter = PeriodFormatterBuilder()
            .appendHours()
            .appendPluralSuffix(this, R.plurals.hour, unitSpace)
            .appendSeparator(wordSpace)
            .appendMinutes()
            .appendPluralSuffix(this, R.plurals.minute, unitSpace)
            .appendSeparator(wordSpace)
            .appendSeconds()
            .appendPluralSuffix(this, R.plurals.second, unitSpace)
            .toFormatter()

        formatter.print(Period(millis))
    }

    private fun PeriodFormatterBuilder.appendPluralSuffix(
        resources: Resources,
        @PluralsRes resId: Int,
        spacer: String,
    ) = apply {
        appendSuffix(
            "$spacer${resources.getQuantityString(resId, 1)}",
            "$spacer${resources.getQuantityString(resId, 2)}"
        )
    }
}