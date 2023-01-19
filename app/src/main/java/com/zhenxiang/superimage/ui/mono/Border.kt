package com.zhenxiang.superimage.ui.mono

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset

fun Modifier.drawTopBorder(borderStroke: BorderStroke) = drawWithContent {
    val strokeWidth = borderStroke.width.value * density
    val y = strokeWidth / 2

    drawContent()

    drawLine(
        borderStroke.brush,
        Offset(0f, y),
        Offset(size.width, y),
        strokeWidth
    )
}

fun Modifier.drawBottomBorder(borderStroke: BorderStroke) = drawWithContent {
    val strokeWidth = borderStroke.width.value * density
    val y = size.height - strokeWidth / 2

    drawContent()

    drawLine(
        borderStroke.brush,
        Offset(0f, y),
        Offset(size.width, y),
        strokeWidth
    )
}
