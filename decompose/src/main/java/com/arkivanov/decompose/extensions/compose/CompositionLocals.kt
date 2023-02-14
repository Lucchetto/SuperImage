package com.arkivanov.decompose.extensions.compose

import androidx.compose.runtime.staticCompositionLocalOf
import com.arkivanov.essenty.lifecycle.LifecycleOwner

val LocalLifecycleOwner = staticCompositionLocalOf<LifecycleOwner> {
    error("CompositionLocal LocalLifecycleOwner not present")
}
