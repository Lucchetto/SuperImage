package com.zhenxiang.superimage.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object Border {

    val regular: BorderStroke
        @Composable
        get() = BorderStroke(1.25.dp, MaterialTheme.colorScheme.outline)
}

val MaterialTheme.border: Border
    get() = Border
