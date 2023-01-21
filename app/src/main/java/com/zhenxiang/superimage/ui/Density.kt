package com.zhenxiang.superimage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Convert an [Int] pixel value to [Dp].
 */
@Composable
fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }
