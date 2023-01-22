package com.zhenxiang.superimage.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object BorderThickness {

    val thin = 0.75.dp
    val regular = 1.25.dp

}
object Border {

    val Border.thickness: BorderThickness
        get() = BorderThickness

    val thin: BorderStroke
        @Composable
        get() = BorderStroke(BorderThickness.thin, MaterialTheme.colorScheme.outline)

    val regular: BorderStroke
        @Composable
        get() = BorderStroke(BorderThickness.regular, MaterialTheme.colorScheme.outline)

    @Composable
    fun RegularWithAlpha(alpha: Float) = BorderStroke(
        BorderThickness.regular,
        MaterialTheme.colorScheme.outline.copy(alpha)
    )
}

val MaterialTheme.border: Border
    get() = Border
