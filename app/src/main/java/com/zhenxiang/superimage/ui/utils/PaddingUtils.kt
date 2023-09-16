package com.zhenxiang.superimage.ui.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

/**
 * Workaround to offset the horizontal padding applied from the given [PaddingValues]
 */
fun Modifier.ignoreHorizontalPadding(padding: PaddingValues): Modifier {
    return layout { measurable, constraints ->
        val overriddenWidth = constraints.maxWidth +
                padding.calculateLeftPadding(layoutDirection).roundToPx() +
                padding.calculateRightPadding(layoutDirection).roundToPx()
        val placeable = measurable.measure(constraints.copy(maxWidth = overriddenWidth))
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}
