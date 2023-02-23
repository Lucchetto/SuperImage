package com.zhenxiang.superimage.utils

import android.content.Context
import android.content.res.Resources
import androidx.annotation.PluralsRes
import com.zhenxiang.superimage.R
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import kotlin.math.roundToLong

object TimeUtils {

    private const val MAX_MILLIS = 2562047788015

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
            .printZeroAlways()
            .appendSeconds()
            .appendPluralSuffix(this, R.plurals.second, unitSpace)
            .toFormatter()

        // Round to nearest second
        formatter.print(Period((millis.coerceAtMost(MAX_MILLIS) / 1000.0).roundToLong() * 1000))
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