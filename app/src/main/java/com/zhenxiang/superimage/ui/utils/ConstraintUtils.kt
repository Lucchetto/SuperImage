package com.zhenxiang.superimage.ui.utils

import androidx.compose.ui.unit.Constraints

val Constraints.isLandscape: Boolean
    get() = maxWidth > maxHeight
